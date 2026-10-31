# Gestión Unimag — Módulo 3

Implementación local de control de uso, sanciones y analítica según las especificaciones actualizadas de este repositorio.

## Stack

Java 21, Spring Boot 4.1.2, Spring MVC, Spring Data JPA, Spring Security/JWT, Spring Kafka y Flyway. Frontend React 19.3 con Vite 8.3. Datos MySQL 8.4 y broker Kafka 4.3.1 en desarrollo.

## Desarrollo

Copia el archivo .env.example a .env, actualiza las contraseñas demo y arranca todo con Docker Compose.

Frontend disponible en http://localhost:5173. API disponible en http://localhost:8080/api/v1. OpenAPI y Swagger UI en /v3/api-docs y /swagger-ui/index.html.

Las cuentas demo solo son para desarrollo local. En producción usa un emisor institucional de JWT con sub como código institucional y role igual a ESTUDIANTE o PERSONAL_ADMINISTRATIVO. Reemplaza M3_JWT_SECRET, las contraseñas y los valores de conexión.

## Requisitos de integración

M3 no llama a M1/M2 por REST. Publica los eventos de check-out, daño e inasistencia mediante outbox de MySQL y consume reservas, acuses, pérdidas y estados de recurso por Kafka. Los consumidores deduplican por eventId. Los contratos marcados por acordar en el repositorio se mantienen aislados en integration para poder ajustar payloads sin alterar los casos de uso.

La carga inicial de reservas de demostración se controla con M3_DEMO_SEED. Desactívala en cualquier entorno conectado a otros módulos.

## Funcionalidades

Check-out y penalización diaria por mora; reportes de daño, catálogo de costos y cobros; confirmación y anulación de inasistencias con acuse; historial de score, recuperación y desbloqueo; consulta de cumplimiento del estudiante y de M2; novedades pendientes para M1; analítica, rankings, exportación CSV y detalle administrativo del estudiante.