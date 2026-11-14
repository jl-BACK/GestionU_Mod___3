#!/usr/bin/env python3
"""Smoke tests de solo lectura para la API de GestiónU."""

from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from email.message import Message
from pathlib import Path
from typing import Any


# El archivo .env está en la raíz del proyecto, no dentro de backend/test.
PROJECT_ROOT = Path(__file__).resolve().parents[2]
TIMEOUT_SECONDS = 10


@dataclass(frozen=True)
class TestConfig:
    """Direcciones y credenciales de prueba cargadas del entorno local."""

    backend_url: str
    api_url: str
    student_code: str
    student_password: str
    admin_code: str
    admin_password: str


@dataclass(frozen=True)
class Response:
    """Respuesta HTTP reducida a los datos que necesitan estas pruebas."""

    status: int
    body: bytes
    headers: Message

    def json(self) -> Any:
        """Convierte una respuesta JSON y da un error claro si el formato no sirve."""
        try:
            return json.loads(self.body)
        except (UnicodeDecodeError, json.JSONDecodeError) as error:
            raise AssertionError("La API no respondió JSON válido.") from error


class SmokeTests:
    """Ejecuta peticiones HTTP y agrupa las comprobaciones por función."""

    def __init__(self, config: TestConfig) -> None:
        self.config = config
        self.passed = 0

    def request(
        self,
        method: str,
        path: str,
        expected_status: int,
        *,
        token: str | None = None,
        body: dict[str, Any] | None = None,
        base_url: str | None = None,
        accept: str = "application/json",
    ) -> Response:
        """Envía una petición y comprueba su código HTTP, sin imprimir secretos."""
        headers = {"Accept": accept}
        request_body = None

        # Solo el login usa POST y su efecto es emitir un token; no escribe datos.
        if body is not None:
            request_body = json.dumps(body).encode("utf-8")
            headers["Content-Type"] = "application/json"
        if token:
            headers["Authorization"] = f"Bearer {token}"

        base = (base_url or self.config.api_url).rstrip("/")
        http_request = urllib.request.Request(
            f"{base}{path}",
            data=request_body,
            headers=headers,
            method=method,
        )

        try:
            with urllib.request.urlopen(http_request, timeout=TIMEOUT_SECONDS) as response:
                result = Response(response.status, response.read(), response.headers)
        except urllib.error.HTTPError as error:
            # HTTP 401/403 también son respuestas válidas para probar permisos.
            result = Response(error.code, error.read(), error.headers)
        except (urllib.error.URLError, TimeoutError, OSError) as error:
            raise RuntimeError(f"No se pudo conectar con {base}: {error}") from error

        if result.status != expected_status:
            raise AssertionError(
                f"{method} {path}: se esperaba HTTP {expected_status} "
                f"y se recibió {result.status}."
            )

        self.passed += 1
        print(f"OK  {method:5} {path} (HTTP {result.status})")
        return result

    def login(self, code: str, password: str, expected_role: str) -> str:
        """Inicia sesión y devuelve el JWT usado en las peticiones protegidas."""
        response = self.request(
            "POST",
            "/auth/login",
            200,
            body={"code": code, "password": password},
        ).json()
        token = response.get("accessToken")
        if response.get("role") != expected_role or not isinstance(token, str) or not token:
            raise AssertionError(f"El login no devolvió un token para el rol {expected_role}.")
        return token

    def test_system_is_available(self) -> None:
        """Comprueba que el backend vive y que publica su especificación OpenAPI."""
        health = self.request(
            "GET",
            "/actuator/health",
            200,
            base_url=self.config.backend_url,
        ).json()
        if health.get("status") != "UP":
            raise AssertionError("El health check no reportó estado UP.")

        api_docs = self.request(
            "GET",
            "/v3/api-docs",
            200,
            base_url=self.config.backend_url,
        ).json()
        if not api_docs.get("openapi"):
            raise AssertionError("La especificación no contiene una versión OpenAPI.")

    def test_student_endpoints(self, student_token: str, admin_token: str) -> None:
        """Comprueba lectura pública, lectura del estudiante y rechazo de roles ajenos."""
        student_code = urllib.parse.quote(self.config.student_code, safe="")
        self.request("GET", f"/compliance/people/{student_code}", 200)
        self.request("GET", "/me/compliance", 200, token=student_token)

        # El administrador no debe poder consultar la ruta reservada al estudiante.
        self.request("GET", "/me/compliance", 403, token=admin_token)

        # El estudiante tampoco debe poder acceder a un endpoint administrativo.
        self.request(
            "GET",
            "/admin/analytics/overview",
            403,
            token=student_token,
        )

    def test_admin_read_endpoints(self, admin_token: str) -> None:
        """Comprueba listados y reportes administrativos sin modificar registros."""
        student_code = urllib.parse.quote(self.config.student_code, safe="")
        read_paths = (
            "/absences/eligible",
            "/absences",
            f"/admin/students/{student_code}",
            "/admin/analytics/overview",
            "/admin/analytics/rankings",
            "/admin/charges",
            "/admin/cost-catalog",
            "/admin/config",
            "/damage-reports",
            "/novedades/pendientes?recursoId=SMOKE-TEST-RESOURCE",
        )
        for path in read_paths:
            self.request("GET", path, 200, token=admin_token)

        # El reporte se entrega como CSV, por eso se pide explícitamente ese formato.
        export = self.request(
            "GET",
            "/admin/analytics/export",
            200,
            token=admin_token,
            accept="text/csv",
        )
        if not export.headers.get("Content-Type", "").startswith("text/csv"):
            raise AssertionError("El endpoint de exportación no respondió como CSV.")
        if not export.body.startswith(b"indicador,valor"):
            raise AssertionError("El CSV no contiene la cabecera esperada.")

    def run(self) -> None:
        """Ejecuta todas las comprobaciones en el orden que suelen diagnosticarse."""
        self.test_system_is_available()

        # Las credenciales se configuran en .env; el token no se imprime ni persiste.
        student_token = self.login(
            self.config.student_code,
            self.config.student_password,
            "ESTUDIANTE",
        )
        admin_token = self.login(
            self.config.admin_code,
            self.config.admin_password,
            "PERSONAL_ADMINISTRATIVO",
        )

        self.test_student_endpoints(student_token, admin_token)
        self.test_admin_read_endpoints(admin_token)
        print(f"\nSmoke tests superados: {self.passed}")


def load_config() -> TestConfig:
    """Lee .env sin reemplazar variables que ya estén definidas en la terminal."""
    env_file = PROJECT_ROOT / ".env"
    if env_file.is_file():
        for raw_line in env_file.read_text(encoding="utf-8").splitlines():
            line = raw_line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue

            key, value = line.split("=", 1)
            key = key.strip()
            value = value.strip()
            # Permite valores entre comillas, por ejemplo contraseñas con espacios.
            if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
                value = value[1:-1]
            if key:
                os.environ.setdefault(key, value)

    backend_url = os.environ.get("BACKEND_URL", "http://localhost:8080").rstrip("/")
    return TestConfig(
        backend_url=backend_url,
        api_url=os.environ.get("API_BASE_URL", f"{backend_url}/api/v1").rstrip("/"),
        student_code=os.environ.get("M3_DEMO_STUDENT_CODE", "202610001"),
        student_password=os.environ.get("M3_DEMO_STUDENT_PASSWORD", "student123"),
        admin_code=os.environ.get("M3_DEMO_ADMIN_CODE", "admin"),
        admin_password=os.environ.get("M3_DEMO_ADMIN_PASSWORD", "admin123"),
    )


def main() -> int:
    """Punto de entrada: convierte fallos de pruebas en un código de salida útil."""
    try:
        SmokeTests(load_config()).run()
    except (AssertionError, RuntimeError) as error:
        print(f"\nERROR: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
