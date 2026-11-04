# Gestión Unimag — Módulo 3: Control de Uso, Sanciones y Analítica
> **Versión 1.0.0 (Core Backend & Integración de Módulos)**

Implementación del **Módulo 3** para el sistema de reservas y gestión de recursos universitarios de la Universidad del Magdalena. Este módulo se encarga del ciclo completo de control de uso, cálculo dinámico de sanciones, score de confianza, gestión de cobros por daños y analítica institucional.

---

## 📌 Alcance de la Versión 1.0.0

Esta versión entrega el núcleo completo del **Backend**, lógica de negocio, persistencia y eventos asíncronos para los 6 casos de uso del módulo:

| Caso de Uso | Descripción | Endpoints / Eventos Clave |
|---|---|---|
| **CU-1: Registrar Check-out** | Cierre de préstamos y reservas, cálculo de retrasos y deducción diaria de score. | `POST /api/v1/checkout`<br>Kafka: `CheckOutRegistered` (M2), `reserva.finalizada` (M1) |
| **CU-2: Reportar Novedades y Cobros** | Daños técnicos, subida de evidencias fotográficas, catálogo de costos y gestión de cobros. | `POST /api/v1/damages`<br>`GET /api/v1/admin/charges`<br>Kafka: `reporte.danos` (M1) |
| **CU-3: Confirmar Inasistencia** | Constatación en sitio de no asistencia pasados 30 minutos, con acuse de aceptación y anulación. | `POST /api/v1/absences`<br>`POST /api/v1/absences/{id}/annul`<br>Kafka: `NoShowReported` (M2) |
| **CU-4: Desbloqueo de Usuario** | Desbloqueo manual por personal administrativo y recuperación programada de score cada 30 días. | `POST /api/v1/admin/students/{code}/unblock` |
| **CU-5: Consultar Cumplimiento** | Consulta de historial de score y sanciones para estudiantes y endpoints REST para M1 y M2. | `GET /api/v1/me/compliance`<br>`GET /api/v1/compliance/people/{code}` (M2)<br>`GET /api/v1/novedades/pendientes` (M1) |
| **CU-6: Analítica y Métricas** | Tablero de control, rankings de cumplimiento, exportación a CSV y expiración de sanciones. | `GET /api/v1/admin/overview`<br>`GET /api/v1/admin/rankings`<br>`GET /api/v1/admin/export` |

---

## 🛠️ Stack Tecnológico

* **Lenguaje**: Java 21 (LTS)
* **Framework**: Spring Boot 4.1.2 (Spring MVC, Spring Data JPA, Spring Security, Spring Kafka)
* **Base de Datos**: MySQL 8.4 (Migraciones versionadas con Flyway)
* **Message Broker**: Apache Kafka 4.3.1 (KRaft)
* **Seguridad**: Autenticación JWT stateless con segregación de roles (`ESTUDIANTE` y `PERSONAL_ADMINISTRATIVO`)
* **Documentación Interactiva**: OpenAPI 3 y Swagger UI
* **Contenedores**: Docker y Docker Compose

---

## 🏛️ Arquitectura y Patrones de Integración

* **Patrón Transaccional Outbox (`outbox_message`)**: Los eventos hacia M1 y M2 se registran en la misma transacción atómica de MySQL y son despachados asíncronamente a Kafka por un worker periódico con reintentos exponenciales.
* **Patrón Inbox / Deduplicación (`processed_event`)**: Cada mensaje entrante de Kafka valida su `eventId` único para garantizar procesamiento exactamente una vez (*idempotencia*).
* **Dead Letter Topics (DLT)**: Los mensajes mal formados o reservas no reconocidas se desvían automáticamente a tópicos DLT para evitar bloqueos del consumidor.
* **Aislamiento REST**: M3 no invoca servicios REST externos de M1/M2; la comunicación saliente es 100% asíncrona mediante Kafka. Las únicas llamadas HTTP intermodulares son entrantes hacia M3 (sin JWT).

---

## 🚀 Puesta en Marcha Rápida (Docker Compose)

### 1. Prerrequisitos
* Docker y Docker Compose instalados y en ejecución.
* Git para control de versiones.

### 2. Configurar Variables de Entorno
Copia la plantilla de entorno:
```bash
cp .env.example .env
```
*(Puedes ajustar las contraseñas, secretos y parámetros demo según lo requieras).*

### 3. Iniciar Servicios
Para levantar la base de datos MySQL, el broker Kafka y el servicio backend:
```bash
docker compose up --build -d mysql kafka backend
```

Para verificar que los contenedores estén activos:
```bash
docker compose ps
```

---

## 📖 Documentación y Pruebas de la API

Una vez iniciado el contenedor de backend, accede a la documentación interactiva desde tu navegador:

* **Swagger UI interactivo**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
* **Especificación OpenAPI (JSON)**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Credenciales de Demostración (Seed Local)
Si `M3_DEMO_SEED=true` está activo en tu `.env`, puedes autenticarte con:

* **Estudiante**:
  * Código: `202610001`
  * Contraseña: `Estudiante123!`
  * Rol: `ESTUDIANTE`
* **Personal Administrativo**:
  * Código: `admin`
  * Contraseña: `Admin123!`
  * Rol: `PERSONAL_ADMINISTRATIVO`

---

## 🗺️ Roadmap de Versiones

* **v1.0.0 (Actual)**:
  * ✅ Core Backend, Base de Datos, Kafka y API REST completa.
  * ✅ Migraciones Flyway y patrones Outbox/Inbox implementados.
  * ✅ Soporte de roles JWT y documentación OpenAPI.
* **v2.0.0 (Próxima)**:
  * 🔄 Integración del Frontend web en React 19 + Vite (`frontend/`).
  * 🔄 Panel visual para estudiantes y panel de control administrativo.
