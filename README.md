# Gestión Unimag — Módulo 3: Control de Uso, Sanciones y Analítica
> **Versión 2.0.0 (Full Stack: Backend API + Frontend Web + Kafka)**

Implementación integral del **Módulo 3** para el sistema de reservas y gestión de recursos universitarios de la Universidad del Magdalena. Este módulo gestiona el ciclo completo posterior a la asignación de recursos: control de check-out, cálculo dinámico de score de confianza, aplicación y auditoría de sanciones, gestión de cobros por daños o pérdidas, integración asíncrona intermodular con Apache Kafka y analítica institucional con portal web interactivo.

---

## 📌 Alcance Funcional del Sistema

El sistema implementa los **6 casos de uso** estipulados en los planes técnicos, disponibles tanto a nivel de API REST como a través de su interfaz web:

| Caso de Uso | Backend (API & Eventos) | Frontend (Vistas de Usuario) |
|---|---|---|
| **CU-1: Registrar Check-out** | `POST /api/v1/checkout`<br>Kafka: `CheckOutRegistered` (M2), `reserva.finalizada` (M1) | Visualización de reservas activas, cálculo de tiempo de uso y registro de entrega. |
| **CU-2: Novedades Técnicas y Cobros** | `POST /api/v1/damages`<br>`GET /api/v1/admin/charges`<br>Kafka: `reporte.danos` (M1) | **Estudiante**: Formulario de reporte con subida de evidencia fotográfica.<br>**Admin**: Gestión de cobros (`PAGADO`/`ANULADO`) y catálogo de costos. |
| **CU-3: Confirmar Inasistencia** | `POST /api/v1/absences`<br>`POST /api/v1/absences/{id}/annul`<br>Kafka: `NoShowReported` (M2) | **Admin**: Listado de reservas con >30 min de abandono, confirmación en sitio y anulación auditada con motivo. |
| **CU-4: Desbloqueo de Usuario** | `POST /api/v1/admin/students/{code}/unblock`<br>Recuperación automática de +5 pts / 30 días | **Admin**: Modal de desbloqueo manual para estudiantes con score 0 y registro de justificación. |
| **CU-5: Consultar Cumplimiento** | `GET /api/v1/me/compliance`<br>`GET /api/v1/compliance/people/{code}` (M2)<br>`GET /api/v1/novedades/pendientes` (M1) | **Estudiante**: Historial de score, sanciones activas y nivel de confianza.<br>**Admin**: Búsqueda por código estudiantil. |
| **CU-6: Analítica Institucional** | `GET /api/v1/admin/overview`<br>`GET /api/v1/admin/rankings`<br>`GET /api/v1/admin/export` | **Admin**: Métricas globales, ranking de estudiantes con menor score y exportación en CSV. |

---

## 💻 Arquitectura y Componentes

```text
┌────────────────────────────────────────────────────────┐
│                   Frontend (React 19)                  │
│       Portal Estudiante      │      Panel Admin        │
└───────────────────────────┬────────────────────────────┘
                            │ HTTP / REST (JWT)
                            ▼
┌────────────────────────────────────────────────────────┐
│               Backend (Spring Boot 4.1.2)              │
│  Controllers  ◄──►  Services  ◄──►  Outbox / Inbox     │
└─────────────┬───────────────────────────────┬──────────┘
              │ JDBC                          │ Kafka Protocol
              ▼                               ▼
     ┌─────────────────┐             ┌─────────────────┐
     │ MySQL 8.4 (DB)  │             │   Apache Kafka  │
     │  (Data + Outbox)│             │ (M1 & M2 Events)│
     └─────────────────┘             └─────────────────┘
```

### 1. Backend (Spring Boot 4 / Java 21)
* **Arquitectura Hexagonal / Por Capas**: Separación estricta entre presentación, servicios de dominio, persistencia e integración.
* **Patrón Transaccional Outbox (`outbox_message`)**: Asegura atomicidad; los eventos de dominio se guardan en MySQL dentro de la misma transacción de negocio y un worker los publica a Kafka con reintentos y tolerancia a fallos.
* **Patrón Inbox (`processed_event`)**: Deduplica mensajes entrantes por `eventId` para garantizar procesamiento idempotente.
* **Seguridad JWT Stateless**: Cadenas de filtros separadas para usuarios del portal (`ESTUDIANTE`, `PERSONAL_ADMINISTRATIVO`) y endpoints intermodulares sin autenticación directa según contratos de M1/M2.

### 2. Frontend (React 19 + Vite 8.3)
* **Diseño Responsivo y Modular**: Estilos limpios y directos sin dependencias pesadas de terceros.
* **Portal del Estudiante**:
  * Visualización dinámica del score de confianza con barras de estado de color.
  * Historial detallado de deducciones por mora o inasistencia y recuperaciones periódicas.
  * Módulo interactivo para reportar daños y adjuntar archivos de evidencia.
* **Panel de Control Administrativo**:
  * **Tablero Principal**: Métricas clave en tiempo real y exportación de reportes a archivo `.csv`.
  * **Control de Inasistencias**: Bandeja de reservas vencidas para confirmación o anulación justificada.
  * **Módulo Financiero / Cobros**: Listado de deudas generadas por pérdidas o daños, cambio de estado a pagado y administración del catálogo de precios por tipo de recurso.
  * **Búsqueda y Desbloqueo**: Auditoría individual por código institucional con desbloqueo manual asistido.

---

## 🛠️ Stack Tecnológico

| Capa | Tecnologías |
|---|---|
| **Frontend** | React 19.3, Vite 8.3, Vanilla CSS Moderno, Context API / Hooks |
| **Backend** | Java 21 (LTS), Spring Boot 4.1.2, Spring MVC, Spring Data JPA, Spring Security, Spring Kafka |
| **Base de Datos** | MySQL 8.4, Migraciones automatizadas con Flyway |
| **Mensajería** | Apache Kafka 4.3.1 (Modo KRaft) |
| **Infraestructura** | Docker & Docker Compose |
| **Documentación** | OpenAPI 3.0 / Swagger UI |

---

## 🚀 Despliegue Rápido con Docker Compose

Toda la solución (Backend, Frontend, Base de Datos y Broker de Mensajería) se despliega con un solo comando.

### 1. Clonar y Configurar Entorno
```bash
cp .env.example .env
```
*(Puedes revisar y personalizar las contraseñas, secreto JWT y puertos en el archivo `.env`).*

### 2. Arrancar Todos los Servicios
```bash
docker compose up --build -d
```

### 3. Verificar Contenedores
```bash
docker compose ps
```

---

## 🌐 Puntos de Acceso del Sistema

Una vez los contenedores estén levantados:

| Servicio | URL | Descripción |
|---|---|---|
| **Frontend (Portal Web)** | [http://localhost:5173](http://localhost:5173) | Interfaz gráfica interactiva para Estudiantes y Administradores |
| **Backend API** | [http://localhost:8083/api/v1](http://localhost:8083/api/v1) | Endpoints REST del sistema (puerto estándar intermodular M3) |
| **Documentación Swagger** | [http://localhost:8083/swagger-ui/index.html](http://localhost:8083/swagger-ui/index.html) | Interfaz interactiva de prueba de endpoints |
| **Especificación OpenAPI** | [http://localhost:8083/v3/api-docs](http://localhost:8083/v3/api-docs) | Esquema técnico en formato JSON |
| **Apache Kafka** | `localhost:9092` (Host) / `kafka:19092` (Docker) | Broker de eventos intermodular |
| **MySQL** | `localhost:3306` | Base de datos relacional del módulo |

---

## 👥 Cuentas de Demostración (Seed de Desarrollo)

Si `M3_DEMO_SEED=true` está activo en el `.env`, el sistema precarga usuarios de prueba listos para iniciar sesión en [http://localhost:5173](http://localhost:5173):

| Rol | Código Institucional | Contraseña | Vistas y Permisos |
|---|---|---|---|
| **Estudiante** | `202610001` | `Estudiante123!` | Ver score, historial de cumplimiento, reportar daños con foto. |
| **Administrativo** | `admin` | `Admin123!` | Tablero general, confirmar inasistencias, cobrar/anular daños, desbloquear usuarios, exportar CSV. |

---

## 📜 Historial de Versiones (Gitflow)

* **[v1.0.0](https://github.com/jl-BACK/GestionU_Mod___3/releases/tag/v1.0.0)**:
  * Implementación completa del Core Backend con Spring Boot y Java 21.
  * Esquema de base de datos Flyway y patrones Outbox/Inbox con Kafka.
  * Lógica de negocio de los 6 planes técnicos y contratos de integración para M1 y M2.
* **[v2.0.0](https://github.com/jl-BACK/GestionU_Mod___3/releases/tag/v2.0.0)**:
  * Aplicación cliente completa en React 19 + Vite.
  * Integración de autenticación JWT y consumo de la API REST.
  * Vistas dedicadas para portal estudiantil y consola administrativa integral.
