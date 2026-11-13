# GUÍA MAESTRA DE ESTUDIO: GESTIÓN UNIMAG (MÓDULO 3)
## Control de Uso, Sanciones, Auditoría y Analítica Institucional
> **Documento Integral de Repaso y Arquitectura Técnica**  
> *Versión del Sistema:* 2.0.0 (Full Stack: Spring Boot 4 + React 19 + Apache Kafka + MySQL 8.4)

---

## TABLA DE CONTENIDO
1. [Visión General del Proyecto](#1-visión-general-del-proyecto)
2. [Arquitectura y Patrones Técnicos Fundamentales](#2-arquitectura-y-patrones-técnicos-fundamentales)
3. [Diccionario Exhaustivo de Clases del Backend](#3-diccionario-exhaustivo-de-clases-del-backend)
   - [3.1 Capa de Configuración (`config`)](#31-capa-de-configuración-config)
   - [3.2 Capa de Dominio - Entidades y Modelos (`domain.model`)](#32-capa-de-dominio---entidades-y-modelos-domainmodel)
   - [3.3 Capa de Dominio - Repositorios (`domain.repository`)](#33-capa-de-dominio---repositorios-domainrepository)
   - [3.4 Capa de Negocio - Servicios (`business.service` e `integration`)](#34-capa-de-negocio---servicios-businessservice-e-integration)
   - [3.5 Capa de Infraestructura - Mensajería y Outbox (`infrastructure.messaging`)](#35-capa-de-infraestructura---mensajería-y-outbox-infrastructuremessaging)
   - [3.6 Capa de Presentación - Controladores y DTOs (`presentation`)](#36-capa-de-presentación---controladores-y-dtos-presentation)
4. [Componentes del Frontend (React 19 + Vite 8)](#4-componentes-del-frontend-react-19--vite-8)
5. [Análisis Profundo de los 6 Casos de Uso con Ejemplos de Código](#5-análisis-profundo-de-los-6-casos-de-uso-con-ejemplos-de-código)
   - [CU-1: Registrar Check-out](#cu-1-registrar-check-out)
   - [CU-2: Novedades Técnicas y Cobros por Daño o Pérdida](#cu-2-novedades-técnicas-y-cobros-por-daño-o-pérdida)
   - [CU-3: Confirmar Inasistencia (No-Show)](#cu-3-confirmar-inasistencia-no-show)
   - [CU-4: Desbloqueo de Usuario y Gestión del Score](#cu-4-desbloqueo-de-usuario-y-gestión-del-score)
   - [CU-5: Consultar Cumplimiento](#cu-5-consultar-cumplimiento)
   - [CU-6: Analítica Institucional y Exportación](#cu-6-analítica-institucional-y-exportación)
6. [Banco de Preguntas Clave para Repaso y Sustentación](#6-banco-de-preguntas-clave-para-repaso-y-sustentación)

---

## 1. Visión General del Proyecto

### 1.1 Contexto y Propósito
El sistema general de la **Universidad del Magdalena** automatiza la reserva y préstamo de recursos físicos (aulas, laboratorios, auditorios, portátiles, videoproyectores, cámaras, etc.). El sistema está dividido en módulos:
* **Módulo 1 (M1 - Catálogo de Recursos)**: Administra el inventario, disponibilidad física, estados del activo y mantenimientos.
* **Módulo 2 (M2 - Gestión de Reservas)**: Gestiona la solicitud de préstamos, asignación de turnos y aprobación de reservas.
* **Módulo 3 (M3 - Este Proyecto)**: Asume la responsabilidad **una vez la reserva o préstamo está en curso**. Se encarga de:
  1. Registrar la devolución oportuna o tardía del recurso (**Check-out**).
  2. Procesar novedades por daños físicos o pérdidas con cálculo de cobros económicos.
  3. Detectar y confirmar inasistencias en espacios físicos (**No-show**).
  4. Mantener el **Score de Confianza** (reputación de 0 a 100) de cada estudiante con bloqueo automático y recuperación periódica.
  5. Proveer analítica de utilización, ranking de comportamiento y reportes a directivos.

### 1.2 Regla de Oro de Integración
**M3 no realiza peticiones HTTP directas hacia M1 ni hacia M2.**  
Toda la sincronización entre módulos es **asíncrona** y desacoplada mediante **Apache Kafka**:
* M3 se suscribe a los eventos de reservas creadas, préstamos retirados, cancelaciones y reportes de pérdida emitidos por M2.
* M3 publica eventos de finalización de reservas, confirmación de check-outs, novedades técnicas y reportes de inasistencia hacia M1 y M2.

```text
  ┌────────────────┐                     ┌────────────────┐
  │   Módulo 1     │                     │   Módulo 2     │
  │ (Recursos/Inv) │                     │   (Reservas)   │
  └───────┬────────┘                     └────────┬───────┘
          │ (Eventos Kafka)                       │ (Eventos Kafka)
          ▼                                       ▼
    ═════════════════════════════════════════════════════
                      APACHE KAFKA BROKER
    ═════════════════════════════════════════════════════
          ▲                                       │
          │ (Publica Outbox)                      ▼ (Consume Inbox)
  ┌───────┴───────────────────────────────────────┴───────┐
  │              MÓDULO 3: GESTIÓN UNIMAG                 │
  │  - Backend: Spring Boot 4 / Java 21                   │
  │  - Frontend: React 19 / Vite                          │
  │  - Base de Datos: MySQL 8.4 (Flyway V1, V2, V3)       │
  └───────────────────────────────────────────────────────┘
```

---

## 2. Arquitectura y Patrones Técnicos Fundamentales

### 2.1 Arquitectura Hexagonal / Por Capas
El backend sigue una separación estricta de responsabilidades:
1. **Presentación (`presentation`)**: Controladores REST (`@RestController`), validación de DTOs con `@Valid` y manejo de excepciones uniforme con `@ControllerAdvice`.
2. **Servicios de Negocio (`business.service`)**: Contienen las reglas universitarias, transaccionalidad (`@Transactional`), cálculos de retrasos y mutaciones de estado.
3. **Dominio (`domain.model` & `domain.repository`)**: Entidades JPA puras y contratos de persistencia con Spring Data.
4. **Infraestructura (`infrastructure` & `config`)**: Productores/consumidores de Kafka, tareas programadas (`@Scheduled`), configuración de seguridad JWT y serialización.

### 2.2 Patrón Transaccional Outbox (`outbox_message`)
* **Problema que resuelve**: En un sistema distribuido, si un servicio guarda un registro en la base de datos MySQL e inmediatamente intenta enviar un mensaje a Kafka por red, si la red falla o Kafka se cae, la base de datos queda actualizada pero el evento nunca se emitió (inconsistencia distribuida).
* **Solución implementada**:
  1. Dentro de la **misma transacción ACID de MySQL**, el servicio guarda los cambios de negocio (ej. `CheckOutRecord`) y simultáneamente inserta una fila en la tabla `outbox_message` con estado `PENDIENTE`.
  2. Un proceso en segundo plano ([`OutboxWorker.java`](#outboxworker)) consulta periódicamente los mensajes pendientes y los publica a Kafka.
  3. Una vez Kafka confirma la recepción (`acks=all`), el worker actualiza el mensaje a `PUBLICADO`. Si falla, calcula una reintentación exponencial (`next_retry_at`).

### 2.3 Patrón Inbox / Deduplicación Idempotente (`processed_event`)
* **Problema que resuelve**: La red de Kafka garantiza entrega *at-least-once* (al menos una vez). Un mismo evento de reserva de M2 podría recibirse dos veces por un reintento de red.
* **Solución implementada**: Cada mensaje entrante posee un `eventId` o llave única. Antes de ejecutar lógica de negocio, se consulta la tabla `processed_event`. Si el ID ya existe, se descarta silenciosamente, evitando cobros dobles o deducciones duplicadas de score.

### 2.4 Seguridad JWT Stateless y Control de Acceso (RBAC)
* La API no guarda sesiones en memoria (`SessionCreationPolicy.STATELESS`).
* Se utiliza firma simétrica **HMAC-SHA256** con secreto configurable (`M3_JWT_SECRET`).
* Los tokens transportan el `sub` (código de usuario) y el claim `role`:
  * `ROLE_ESTUDIANTE`: Permiso para registrar check-outs propios, reportar daños con foto y consultar su score personal.
  * `ROLE_PERSONAL_ADMINISTRATIVO`: Permiso para ver el tablero analítico, confirmar/anular inasistencias, gestionar cobros, desbloquear alumnos y exportar datos.
  * Endpoints públicos específicos para salud (`/actuator/health`), documentación (`/swagger-ui/**`) e integración entre módulos.

---

## 3. Diccionario Exhaustivo de Clases del Backend

A continuación se detalla **cada una de las clases Java** existentes en el código fuente, agrupadas por su paquete y capa técnica.

### 3.1 Capa de Configuración (`config`)

| Clase | Propósito y Responsabilidad |
|---|---|
| **`GestionUnimagApplication`** | Clase principal anotada con `@SpringBootApplication`, `@EnableScheduling` y `@EnableTransactionManagement`. Inicializa el contexto de Spring. |
| **`JacksonConfig`** | Configura el ObjectMapper para serialización y deserialización JSON uniforme de fechas Java 8 (`JavaTimeModule`) y zonas horarias (`America/Bogota`). |
| **`KafkaErrorHandlingConfig`** | Define interceptores de error y listeners de Kafka para manejar mensajes corruptos o desintoxicación de colas muertas (DLT). |
| **`SecurityConfig`** | Configura la cadena de filtros de Spring Security (`SecurityFilterChain`), CORS para orígenes locales, decodificador y codificador Nimbus JWT (`NimbusJwtDecoder`), y mapeo del claim `role` a `ROLE_*`. |
| **`TimeConfig`** | Define el Bean singleton de `java.time.Clock` ajustado a la zona horaria institucional `America/Bogota`, facilitando tests unitarios sin depender del reloj del hardware. |

---

### 3.2 Capa de Dominio - Entidades y Modelos (`domain.model`)

#### Modelos de Comportamiento y Sanciones
* **`TrustScore`**: Representa el puntaje de confianza de un estudiante (rango 0 a 100). Registra `studentCode`, `score`, `lastInfractionAt` y `lastRecoveryAt`.
* **`ScoreHistory`**: Bitácora inmutable de auditoría de cada variación de puntaje. Guarda: `studentCode`, `type` (ej. `AUMENTO_CHECKOUT`, `INASISTENCIA`, `MORA_DIARIA`), `effectivePoints`, `scoreBefore`, `scoreAfter`, `reason`, `originType` y `originId`.
* **`StudentRef`**: Referencia local desacoplada del estudiante (código institucional y nombre completo).
* **`Sanction`**: Modela una sanción formal impuesta al usuario con fecha de inicio (`startsOn`), fin (`endsOn`), tipo de falta (`TIPO_1`, `TIPO_2`), estado (`VIGENTE`, `CUMPLIDA`, `LEVANTADA`) y motivo.
* **`UnblockAudit`**: Registro de auditoría cuando un administrador desbloquea manualmente a un estudiante con score 0. Guarda el autor, justificación, sanciones levantadas y fecha.

#### Modelos de Reservas y Utilizaciones
* **`ReservationCopy`**: Réplica local de los datos esenciales de una reserva transmitida por M2 (`reservationId`, `holderCode`, `resourceId`, fechas `startsAt`/`endsAt`, `resourceCategory`, `sanctionable`, estado `ACTIVA`/`FINALIZADA`).
* **`ReservationOrder`**: Controla el secuenciamiento y ordenamiento de eventos recibidos por Kafka para una reserva específica.
* **`UsageRecord`**: Registro de la utilización efectiva de un recurso universitario (`startedAt`, `dueAt`, facultad, recurso y estudiante).
* **`CheckOutRecord`**: Registro oficial de devolución/entrega. Contiene `checkedOutAt`, resultado (`A_TIEMPO` o `CON_RETRASO`), minutos de retraso (`delayMinutes`), indicador booleano `hasDamage` y descripción.

#### Modelos de Inasistencias
* **`AbsenceConfirmation`**: Registro de inasistencia física generada cuando un estudiante no se presenta tras 30 minutos de iniciada la reserva. Estado: `PENDIENTE_ACUSE`, `ACEPTADA`, `RECHAZADA_M2`, `ANULADA`.
* **`AbsenceAnnulmentAudit`**: Registro de auditoría cuando un administrador anula una inasistencia (por ejemplo, justificación médica o fuerza mayor). Registra los puntos restituidos al estudiante.

#### Modelos de Daños, Pérdidas y Cobros
* **`DamageReport`**: Reporte técnico de daño sobre un bien universitario. Guarda descripción, ruta del archivo de foto (`evidencePath`), quién reportó, estado técnico y estado del cobro (`PENDIENTE`, `GENERADO`, `SIN_COSTO`).
* **`LostLoan`**: Registra la notificación de pérdida permanente de un activo proveniente de M2.
* **`CostCatalog`**: Catálogo institucional de tarifas y costos de reposición/reparación por tipo de recurso (`resourceType`, `amount`, `active`).
* **`Charge`**: Obligación económica o cobro asignado al estudiante por concepto de daño o pérdida. Estados: `PENDIENTE`, `PAGADO`, `ANULADO`.
* **`ChargeAudit`**: Auditoría de cambios sobre el cobro (registro de quién lo marcó pagado o quién lo anuló y por qué).

#### Modelos de Soporte, Parámetros y Mensajería
* **`OverdueDeduction`**: Controla las deducciones diarias acumuladas aplicadas a préstamos de activos que no han sido devueltos.
* **`Inconsistency`**: Tabla donde se aíslan desajustes o anomalías detectadas en la comunicación con otros módulos para posterior revisión.
* **`SystemConfiguration`**: Parámetros de negocio configurables en tiempo de ejecución (ej. puntos de inicio, minutos de inasistencia, porcentaje por pérdida).
* **`OutboxMessage`**: Tabla del patrón Outbox (`id`, `topic`, `messageKey`, `payload`, `headersJson`, `status`, `retryCount`, `nextRetryAt`).
* **`ProcessedEvent`**: Tabla del patrón Inbox para deduplicación idempotente (`eventId`, `eventType`, `processedAt`).

---

### 3.3 Capa de Dominio - Repositorios (`domain.repository`)

Todos extienden `JpaRepository<T, ID>` de Spring Data JPA:

| Repositorio | Consultas Clave y Propósito |
|---|---|
| **`TrustScoreRepository`** | `findLocked(code)`: Utiliza `@Lock(LockModeType.PESSIMISTIC_WRITE)` para bloquear la fila en MySQL y prevenir condiciones de carrera cuando se recalculan puntos concurrentemente. |
| **`ScoreHistoryRepository`** | Historial paginado o filtrado por estudiante y deduplicación por origen (`findFirstByStudentCodeAndTypeAndOriginTypeAndOriginId`). |
| **`ReservationCopyRepository`** | `findByStatusAndResourceCategoryAnd...`: Búsqueda de candidatos para inasistencia o validación de reservas activas. |
| **`CheckOutRecordRepository`** | `existsByReservationId(id)` y reportes por rango de fechas para analítica institucional. |
| **`AbsenceConfirmationRepository`**| `findByReservationId(id)` y listados por estado (`PENDIENTE_ACUSE`, etc.). |
| **`AbsenceAnnulmentAuditRepository`**| Persistencia de auditorías de anulación. |
| **`DamageReportRepository`** | `existsByUsageId(id)`, reportes pendientes de resolución y filtros por fecha. |
| **`CostCatalogRepository`** | `findFirstByResourceTypeIgnoreCaseAndActiveTrueOrderByIdDesc`: Obtiene la tarifa vigente para un tipo de recurso. |
| **`ChargeRepository`** | Búsqueda de cobros pendientes por estudiante o por id de reporte de daño. |
| **`ChargeAuditRepository`** | Historial de auditoría financiera. |
| **`SanctionRepository`** | Sanciones vigentes por estudiante y sanciones que vencen en la fecha actual. |
| **`UnblockAuditRepository`** | Auditorías de desbloqueo administrativo. |
| **`UsageRecordRepository`** | Historial de utilizaciones iniciadas y finalizadas. |
| **`LostLoanRepository`** | Registro de pérdidas enviadas por M2. |
| **`OverdueDeductionRepository`** | Evita deducir dos veces mora el mismo día para la misma reserva. |
| **`SystemConfigurationRepository`**| Lectura de llaves de configuración (`findByKey`). |
| **`StudentRefRepository`** | Búsqueda y almacenamiento de nombres de estudiantes. |
| **`InconsistencyRepository`** | Guardado de inconsistencias intermodulares. |
| **`ReservationOrderRepository`** | Secuenciamiento de eventos. |
| **`OutboxMessageRepository`** | `findTop50ByStatusAndNextRetryAtBeforeOrderByCreatedAtAsc`: Recupera los eventos pendientes de envío para el worker. |
| **`ProcessedEventRepository`** | Verificación de idempotencia por clave primaria `eventId`. |

---

### 3.4 Capa de Negocio - Servicios (`business.service` e `integration`)

* **`CheckOutService`**: Corazón del caso de uso CU-1. Valida que la reserva pertenezca al solicitante, calcula retrasos, registra el check-out, aplica penalizaciones o bonificaciones en el score y prepara los eventos Outbox hacia M1 y M2.
* **`ScoreService`**: Gestiona el puntaje de confianza. Provee métodos para asegurar que el estudiante exista (`ensure`), aplicar deltas positivos o negativos con bloqueo pesimista (`apply`), registrar auditoría en `ScoreHistory` y marcar fechas de infracción.
* **`AbsenceService`**: Maneja el ciclo de vida de inasistencias (CU-3). Identifica reservas con más de 30 minutos de abandono (`eligible`), procesa la confirmación en sitio por parte del administrador, emite el evento `NoShowReported` a M2, y procesa acuses de aceptación o rechazo.
* **`DamageReportService`**: Registra novedades técnicas sobre recursos con descripción y foto (CU-2). Dispara la evaluación de cobros y publica el evento `reporte.danos` hacia M1.
* **`ChargeService`**: Gestiona los cobros monetarios (CU-2). Cruza el daño o pérdida con el `CostCatalog`, genera la deuda al estudiante, gestiona pagos y anulaciones justificadas con auditoría.
* **`UnblockService`**: Permite al administrador desbloquear manualmente a estudiantes cuyo score llegó a 0 (CU-4). Eleva el score a 20 pts, levanta sanciones activas y guarda auditoría obligatoria.
* **`RecoveryService`**: Servicio programado (`@Scheduled`) que evalúa a estudiantes que llevan 30 días sin cometer infracciones y les recupera automáticamente +5 puntos de score hasta llegar al tope de 100.
* **`OverdueService`**: Evalúa diariamente los préstamos de activos vencidos que no han sido entregados y aplica descuentos de puntos por cada día de mora acumulado.
* **`SanctionExpiryService`**: Revisa diariamente las sanciones cuya fecha `endsOn` ya pasó y las marca como `CUMPLIDA`.
* **`ComplianceQueryService`**: Consulta el estado integral de cumplimiento (score actual, nivel de confianza, historial y sanciones activas) tanto para el estudiante como para las consultas intermodulares de M1 y M2 (CU-5).
* **`AnalyticsService`**: Calcula métricas agregadas (totales de check-outs a tiempo/tarde, inasistencias confirmadas, cobros generados/recaudados, ranking de estudiantes con menor score) con filtros por fecha, facultad o tipo de recurso (CU-6).
* **`ConfigurationService`**: Provee acceso tipado (`int`, `decimal`, `string`) a los valores de la tabla `SystemConfiguration`.
* **`EvidenceStorage`**: Servicio que guarda de forma segura los archivos físicos de imágenes subidas en los reportes de daños, sanitizando nombres de archivo y restringiendo rutas al directorio configurado.
* **`InboundEventService`**: Procesa la carga útil de los eventos recibidos desde Kafka (reservas creadas en M2, cancelaciones, retiros de activos, reportes de pérdida) asegurando idempotencia.
* **`CheckoutRetryService`**: Gestiona el reintento de confirmaciones de check-out cuando un módulo externo reporta demoras.
* **`InconsistencyService`**: Registra y cataloga eventos anómalos o datos corruptos para revisión técnica.
* **`DemoSeedRunner`**: Si `M3_DEMO_SEED=true`, inicializa en la base de datos usuarios de prueba, reservas de ejemplo y catálogos de costos al arrancar el backend.
* **`DomainException`**: Excepción desacoplada de la capa web que encapsula código de error de negocio (`code`) y estado HTTP correspondiente (`HttpStatus`).
* **`ReservationModulePublisher`**: Interfaz de integración para publicar eventos hacia M2 (`CheckOutRegistered`, `NoShowReported`).
* **`UsageEventsPublisher`**: Interfaz de integración para publicar eventos hacia M1 (`reserva.finalizada`, `reporte.danos`).

---

### 3.5 Capa de Infraestructura - Mensajería y Outbox (`infrastructure.messaging`)

* **`OutboxService`**: Recibe solicitudes de publicación de eventos de los servicios de negocio, los serializa a JSON y los inserta en la tabla `OutboxMessage` dentro de la transacción activa.
* **`OutboxWorker`**: Tarea programada mediante `@Scheduled(fixedDelay = 700ms)`. Lee lotes de mensajes pendientes de la base de datos y los envía a Kafka con `KafkaTemplate`. Al recibir el ACK, actualiza el estado a `PUBLICADO`.
* **`OutboxDomainPublisher`**: Implementa `ReservationModulePublisher` y `UsageEventsPublisher`, traduciendo las llamadas de dominio a registros en el Outbox.
* **`InboundKafkaListeners`**: Contiene los métodos anotados con `@KafkaListener` que escuchan los tópicos de reservas, cancelaciones, retiros y acuses de M2 y M1, delegando el procesamiento a `InboundEventService`.

---

### 3.6 Capa de Presentación - Controladores y DTOs (`presentation`)

* **`ApiModels`**: Archivo maestro que agrupa todas las clases de DTOs (*Data Transfer Objects*) para requests y responses JSON (`CheckOutRequest`, `CheckOutResponse`, `DamageReportRequest`, `ConfirmAbsenceRequest`, `UnblockRequest`, `LoginRequest`, `LoginResponse`, etc.).
* **`ApiExceptionHandler`**: Clase `@RestControllerAdvice` que captura `DomainException`, errores de validación de Bean Validation (`MethodArgumentNotValidException`) y errores de acceso, devolviendo respuestas estandarizadas en formato RFC 7807 / Problem Detail.
* **`AuthController`**: Endpoint `/api/v1/auth/login`. Autentica credenciales contra los usuarios registrados y emite el token JWT firmado.
* **`CheckOutController`**: Endpoint `POST /api/v1/checkout`. Expone el registro de devolución para el estudiante autenticado.
* **`DamageController`**: Endpoints para reporte de daños con formulario multipart (`POST /api/v1/damages`) y consulta de historial de daños.
* **`AbsenceController`**: Endpoints administrativos para listar candidatos a inasistencia (`GET /api/v1/absences/candidates`), confirmar inasistencia (`POST /api/v1/absences`) y anularla con motivo (`POST /api/v1/absences/{id}/annul`).
* **`ComplianceController`**: Endpoints para autoconsulta del estudiante autenticado (`GET /api/v1/me/compliance`) y consulta pública/intermodular para M2 (`GET /api/v1/compliance/people/{code}`).
* **`AdminController`**: Endpoints para administración general: overview analítico (`GET /api/v1/admin/overview`), ranking (`GET /api/v1/admin/rankings`), exportación en CSV (`GET /api/v1/admin/export`), gestión de cobros (`GET/PATCH /api/v1/admin/charges`), catálogo de precios y desbloqueo manual (`POST /api/v1/admin/students/{code}/unblock`).
* **`M1NoveltyController`**: Endpoint `GET /api/v1/novedades/pendientes` que provee a M1 las novedades técnicas reportadas que requieren mantenimiento.

---

## 4. Componentes del Frontend (React 19 + Vite 8)

El frontend está desarrollado como una SPA (*Single Page Application*) ligera, responsiva y modular:

### 4.1 Infraestructura de Comunicación y Estado
* **`src/api.js`**: Cliente HTTP basado en `fetch`. Inyecta automáticamente el token JWT en la cabecera `Authorization: Bearer <token>`, maneja errores de red y parsea respuestas JSON.
* **`src/formatters.js`**: Funciones auxiliares para formateo de fechas en hora de Colombia (`America/Bogota`), moneda en pesos colombianos (`COP`) y etiquetas visuales de estado.
* **`src/App.jsx`**: Componente raíz. Maneja el estado global de autenticación, el rol del usuario y el enrutamiento de vistas.

### 4.2 Componentes de Interfaz Reutilizables (`components/`)
* **`Shell.jsx`**: Layout maestro que incluye barra de navegación superior, información del usuario logueado, selector de vistas y botón de cierre de sesión.
* **`MetricCard.jsx`**: Tarjeta visual para indicadores numéricos clave (score, deudas, porcentajes).
* **`PromptModal.jsx`**: Ventana modal interactiva para solicitar justificaciones obligatorias (ej. anulación de inasistencias o desbloqueos).
* **`SectionTitle.jsx`**: Encabezados de sección con títulos y descripciones estandarizadas.
* **`Table.jsx`**: Componente de tabla responsiva con estilos predeterminados.
* **`Empty.jsx`**: Estado visual cuando una lista o consulta no arroja registros.

### 4.3 Vistas por Rol (`features/`)
* **`features/auth/Login.jsx`**: Formulario de ingreso al sistema con selector rápido de usuarios de prueba.
* **Portal del Estudiante**:
  * **`StudentOverview.jsx`**: Tablero principal del estudiante. Muestra el score actual en barra de progreso con código de color (verde >80, amarillo 50-79, rojo <50), reservas activas con botón para realizar Check-out, historial de variaciones de puntaje y sanciones vigentes.
  * **`StudentDamage.jsx`**: Formulario interactivo para reportar novedades técnicas sobre recursos, permitiendo adjuntar evidencia fotográfica desde la cámara o disco.
* **Panel Administrativo**:
  * **`AdminOverview.jsx`**: Tablero analítico con métricas globales, gráficos de cumplimiento y botón de exportación instantánea en archivo `.csv`.
  * **`AdminAbsences.jsx`**: Bandeja de reservas abandonadas (>30 min) para confirmar inasistencia o anular reportes previos con justificación.
  * **`AdminCharges.jsx`**: Gestión de cartera y cobros: listado de deudas generadas por daños o pérdidas, filtro por estado, marcado como pagado y administración del catálogo de costos.
  * **`AdminStudents.jsx`**: Buscador de estudiantes por código institucional, vista de auditoría individual y botón de desbloqueo asistido para usuarios en score 0.

---

## 5. Análisis Profundo de los 6 Casos de Uso con Ejemplos de Código

---

### CU-1: Registrar Check-out
* **Actor Principal**: Estudiante (o Administrativo en entrega).
* **Objetivo**: Registrar la finalización de uso de un recurso, verificar si fue entregado a tiempo o con retraso, procesar posibles daños y notificar a M1 y M2.
* **Reglas de Negocio Clave**:
  1. Solo se puede realizar check-out sobre reservas en estado `ACTIVA` que pertenezcan al estudiante.
  2. Si el recurso es un **Activo físico** (portátil, proyector), la fecha límite es `loanDueAt`. Si se entrega después, se marca `CON_RETRASO`.
  3. Si la entrega es a tiempo y sin daños, se premia al estudiante sumando puntos a su score (`CHECKOUT_ON_TIME_POINTS`).
  4. Si se reporta daño en el check-out, la descripción es obligatoria y se genera automáticamente un `DamageReport`.
  5. Se emite a Kafka: `reserva.finalizada` (o `reporte.danos`) para M1 y `CheckOutRegistered` para M2.

#### Representación en Código:
**1. Controlador REST ([`CheckOutController.java`](file:///C:/Users/joseg/Desktop/I_Software/GestionUnimag/backend/src/main/java/com/university/sanctions/presentation/controller/CheckOutController.java)):**
```java
@PostMapping
@PreAuthorize("hasAnyRole('ESTUDIANTE','PERSONAL_ADMINISTRATIVO')")
public CheckOutResponse checkOut(Authentication auth, @Valid @RequestBody CheckOutRequest request) {
    String student = auth.getName(); // Código institucional extraído del JWT
    return service.checkOut(student, request.reservationId(), request.hasDamage(), request.damageDescription());
}
```

**2. Lógica del Servicio ([`CheckOutService.java`](file:///C:/Users/joseg/Desktop/I_Software/GestionUnimag/backend/src/main/java/com/university/sanctions/business/service/CheckOutService.java)):**
```java
@Transactional
public CheckOutResponse checkOut(String student, String reservationId, boolean hasDamage, String description) {
    ReservationCopy reservation = reservations.findById(reservationId)
        .orElseThrow(() -> DomainException.notFound("RESERVATION_NOT_FOUND", "Reserva no encontrada"));

    if (!reservation.getHolderCode().equals(student)) {
        throw DomainException.forbidden("NOT_RESERVATION_HOLDER", "La reserva no pertenece a este estudiante.");
    }

    Instant now = clock.instant();
    Instant dueAt = "ACTIVO".equalsIgnoreCase(reservation.getResourceCategory()) 
        ? reservation.getLoanDueAt() : reservation.getEndsAt();

    boolean late = "ACTIVO".equalsIgnoreCase(reservation.getResourceCategory()) && now.isAfter(dueAt);
    int delayMinutes = late ? (int) Math.ceil(Duration.between(dueAt, now).toMillis() / 60000.0) : 0;

    // Persistir registro de CheckOut
    CheckOutRecord checkOut = new CheckOutRecord();
    checkOut.setReservationId(reservationId);
    checkOut.setStudentCode(student);
    checkOut.setCheckedOutAt(now);
    checkOut.setResult(late ? "CON_RETRASO" : "A_TIEMPO");
    checkOut.setDelayMinutes(delayMinutes);
    checkOut.setHasDamage(hasDamage);
    checkouts.save(checkOut);

    reservation.setStatus("FINALIZADA");
    reservations.save(reservation);

    // Si fue a tiempo y sin novedad, suma puntos al score
    if (!hasDamage && !late) {
        scores.apply(student, configuration.requiredInt("CHECKOUT_ON_TIME_POINTS"),
            "AUMENTO_CHECKOUT", "Devolución a tiempo y sin novedad.", "CHECKOUT", reservationId);
    }

    // Publicación atómica mediante Outbox hacia M1 y M2
    publishOutboxEvents(reservation, now, hasDamage, description);

    return new CheckOutResponse(reservationId, checkOut.getResult(), delayMinutes, hasDamage, 
        scores.current(student).getScore(), "Devolución procesada exitosamente.");
}
```

---

### CU-2: Novedades Técnicas y Cobros por Daño o Pérdida
* **Actor Principal**: Estudiante (reporte), Personal Administrativo (resolución y cobros).
* **Objetivo**: Documentar averías en recursos con soporte fotográfico y liquidar los cobros económicos según el catálogo de costos.
* **Reglas de Negocio Clave**:
  1. Si un activo sufre daño, el sistema busca en `CostCatalog` el precio asignado al tipo de recurso (`resourceType`).
  2. Si existe tarifa, se crea automáticamente un registro `Charge` en estado `PENDIENTE`.
  3. Si M2 notifica pérdida total (`LostLoan`), se cobra un porcentaje reglamentario (`LOST_LOAN_PERCENT`, ej. 100% del costo de reposición).
  4. El administrador puede marcar un cobro como `PAGADO` o `ANULADO` (con justificación auditada).

#### Representación en Código:
**Lógica de Creación de Cobro ([`ChargeService.java`](file:///C:/Users/joseg/Desktop/I_Software/GestionUnimag/backend/src/main/java/com/university/sanctions/business/service/ChargeService.java)):**
```java
@Transactional
public void resolveDamageReport(DamageReport report) {
    if ("GENERADO".equals(report.getChargeResult()) || "SIN_COSTO".equals(report.getChargeResult())) {
        return; // Evita cobros duplicados
    }

    // Buscar tarifa en el catálogo oficial
    CostCatalog cost = costs.findFirstByResourceTypeIgnoreCaseAndActiveTrueOrderByIdDesc(
        report.getResourceType() == null ? "" : report.getResourceType()
    ).orElse(null);

    if (cost == null || cost.getAmount() == null || cost.getAmount().signum() <= 0) {
        report.setChargeResult("SIN_COSTO");
        reports.save(report);
        return;
    }

    // Generar la obligación financiera al estudiante
    Charge charge = new Charge();
    charge.setDamageReportId(report.getId());
    charge.setStudentCode(report.getStudentCode());
    charge.setResourceId(report.getResourceId());
    charge.setResourceName(report.getResourceName());
    charge.setReason("Daño reportado: " + report.getDescription());
    charge.setAmount(cost.getAmount());
    charge.setStatus("PENDIENTE");
    charge.setCreatedAt(clock.instant());
    charges.save(charge);

    report.setChargeResult("GENERADO");
    reports.save(report);
}
```

---

### CU-3: Confirmar Inasistencia (No-Show)
* **Actor Principal**: Personal Administrativo (auditor en sitio).
* **Objetivo**: Detectar espacios reservados cuyos solicitantes no se presentaron y aplicar sanciones.
* **Reglas de Negocio Clave**:
  1. Aplica exclusivamente a recursos de categoría `ESPACIO` (aulas, laboratorios, cubículos).
  2. Solo es elegible tras haber transcurrido una tolerancia de **30 minutos** (`ABSENCE_MINUTES`) desde `startsAt`.
  3. Al confirmar en sitio, se guarda `AbsenceConfirmation` en estado `PENDIENTE_ACUSE` y se publica `NoShowReported` a M2 por Kafka.
  4. Cuando M2 responde con acuse `ACEPTADO`, M3 cambia el estado a `ACEPTADA`, descuenta puntos del score del estudiante y genera una `Sanction` formal de suspensión temporal de reservas.
  5. Si el estudiante presenta excusa válida, el admin puede anularla; esto devuelve los puntos descontados y levanta la sanción.

#### Representación en Código:
**Confirmación y Envío a M2 ([`AbsenceService.java`](file:///C:/Users/joseg/Desktop/I_Software/GestionUnimag/backend/src/main/java/com/university/sanctions/business/service/AbsenceService.java)):**
```java
@Transactional
public AbsenceConfirmation confirm(String reservationId, String actor, String note) {
    ReservationCopy reservation = reservations.findById(reservationId)
        .orElseThrow(() -> DomainException.notFound("RESERVATION_NOT_FOUND", "Reserva no existe."));

    Instant now = clock.instant();
    Instant acceptedFrom = reservation.getStartsAt().plus(Duration.ofMinutes(configuration.requiredInt("ABSENCE_MINUTES")));
    if (now.isBefore(acceptedFrom)) {
        throw DomainException.conflict("TOO_EARLY", "No se puede confirmar antes de los 30 minutos de tolerancia.");
    }

    AbsenceConfirmation row = new AbsenceConfirmation();
    row.setReservationId(reservationId);
    row.setStudentCode(reservation.getHolderCode());
    row.setSpaceId(reservation.getResourceId());
    row.setConfirmedBy(actor);
    row.setStatus("PENDIENTE_ACUSE");
    row = confirmations.save(row);

    // Enviar evento de no-show hacia M2 mediante Outbox
    Map<String, Object> payload = Map.of("reservationId", reservationId, "verifiedAt", now.toString());
    publisher.noShowReported(reservationId, payload);

    return row;
}

// Invocado cuando Kafka recibe el acuse de M2:
@Transactional
public void acknowledge(String reservationId, String result, String reason, Instant acceptedFrom) {
    AbsenceConfirmation row = confirmations.findByReservationId(reservationId).orElse(null);
    if (row != null && "ACEPTADO".equalsIgnoreCase(result)) {
        row.setStatus("ACEPTADA");
        confirmations.save(row);

        // Descontar puntos de score y crear la sanción
        int points = configuration.requiredInt("PENALTY_ABSENCE_POINTS"); // ej: -20 puntos
        scores.apply(row.getStudentCode(), -points, "INASISTENCIA", "Inasistencia confirmada", "ABSENCE", reservationId);
        createSanction(row);
    }
}
```

---

### CU-4: Desbloqueo de Usuario y Gestión del Score
* **Actor Principal**: Personal Administrativo y Worker de Fondo.
* **Objetivo**: Administrar el puntaje de reputación de los estudiantes (escala de 0 a 100) y rehabilitar usuarios bloqueados.
* **Reglas de Negocio Clave**:
  1. Todo usuario inicia con score **100**.
  2. Las faltas restan puntos (inasistencia: -20, mora diaria: -5 por día).
  3. Si el score llega a **0**, el usuario queda automáticamente inhabilitado para reservar.
  4. **Recuperación periódica**: Por cada 30 días continuos sin cometer nuevas faltas, el sistema suma automáticamente **+5 puntos** hasta alcanzar 100.
  5. **Desbloqueo manual**: Un administrador puede desbloquear a un usuario con score 0 mediante un acto asistido. Su puntaje se restaura a un valor base (ej. 20 puntos), se levantan sus sanciones vigentes y se exige un motivo justificado en la auditoría.

#### Representación en Código:
**Desbloqueo Administrativo Asistido ([`UnblockService.java`](file:///C:/Users/joseg/Desktop/I_Software/GestionUnimag/backend/src/main/java/com/university/sanctions/business/service/UnblockService.java)):**
```java
@Transactional
public UnblockResponse unblock(String code, String actor, String reason) {
    if (reason == null || reason.isBlank()) {
        throw DomainException.badRequest("REASON_REQUIRED", "El motivo del desbloqueo es obligatorio.");
    }

    var lockedScore = scores.findLocked(code).orElseThrow();
    if (lockedScore.getScore() != 0) {
        throw DomainException.conflict("STUDENT_NOT_BLOCKED", "Solo se puede desbloquear a estudiantes con score 0.");
    }

    int target = configuration.requiredInt("UNBLOCK_SCORE"); // ej: 20 puntos base

    // Registrar en auditoría institucional
    UnblockAudit audit = new UnblockAudit();
    audit.setStudentCode(code);
    audit.setUnblockedBy(actor);
    audit.setReason(reason);
    audit.setScoreBefore(0);
    audit.setScoreAfter(target);

    // Ajustar el score formalmente
    scoreService.apply(code, target, "DESBLOQUEO_MANUAL", reason, "UNBLOCK", UUID.randomUUID().toString());

    // Levantar sanciones vigentes
    List<Sanction> active = sanctions.findByStudentCodeAndStatusInOrderByStartsOnDesc(code, List.of("VIGENTE"));
    for (Sanction sanction : active) {
        sanction.setStatus("LEVANTADA");
        sanction.setLiftedReason(reason);
    }
    sanctions.saveAll(active);

    audit.setSanctionsLifted(active.size());
    audits.save(audit);

    return new UnblockResponse(code, 0, target, active.size());
}
```

---

### CU-5: Consultar Cumplimiento
* **Actor Principal**: Estudiante (autoconsulta) y Módulos Externos (M1 y M2).
* **Objetivo**: Proporcionar visibilidad transparente sobre el estado disciplinario, confiabilidad y penalizaciones de un estudiante.
* **Reglas de Negocio Clave**:
  1. El estudiante consulta su propio estado autenticado en `/api/v1/me/compliance`.
  2. M2 consulta si un estudiante puede o no reservar mediante `/api/v1/compliance/people/{code}`.
  3. El nivel de confianza se clasifica dinámicamente según el score:
     * **ALTO**: 80 - 100 pts.
     * **MEDIO**: 50 - 79 pts.
     * **BAJO**: 1 - 49 pts.
     * **BLOQUEADO**: 0 pts.

#### Representación en Código:
**Cálculo del Estado de Cumplimiento ([`ComplianceQueryService.java`](file:///C:/Users/joseg/Desktop/I_Software/GestionUnimag/backend/src/main/java/com/university/sanctions/business/service/ComplianceQueryService.java)):**
```java
public ComplianceSummaryResponse getSummary(String code) {
    TrustScore score = scores.findById(code)
        .orElseGet(() -> new TrustScore(code, 100));

    List<Sanction> activeSanctions = sanctions.findByStudentCodeAndStatusInOrderByStartsOnDesc(code, List.of("VIGENTE"));
    List<ScoreHistory> history = scoreHistoryRepository.findByStudentCodeOrderByCreatedAtDesc(code);

    String level;
    if (score.getScore() == 0) level = "BLOQUEADO";
    else if (score.getScore() >= 80) level = "ALTO";
    else if (score.getScore() >= 50) level = "MEDIO";
    else level = "BAJO";

    return new ComplianceSummaryResponse(
        code,
        score.getScore(),
        level,
        activeSanctions.isEmpty(), // isEligibleToReserve
        activeSanctions,
        history
    );
}
```

---

### CU-6: Analítica Institucional y Exportación
* **Actor Principal**: Personal Administrativo y Directivos.
* **Objetivo**: Medir la efectividad del uso de recursos universitarios, identificar patrones de incumplimiento y exportar datos para informes institucionales.
* **Reglas de Negocio Clave**:
  1. Permite filtrar métricas por rango de fechas, facultad o tipo de recurso.
  2. Calcula: tasa de devoluciones a tiempo vs retrasadas, total de inasistencias, valor total cobrado vs pagado.
  3. Ranking de los estudiantes con peor comportamiento para intervención pedagógica.
  4. Generación y descarga directa de un informe en formato estándar **CSV**.

#### Representación en Código:
**Exportación en CSV ([`AdminController.java`](file:///C:/Users/joseg/Desktop/I_Software/GestionUnimag/backend/src/main/java/com/university/sanctions/presentation/controller/AdminController.java)):**
```java
@GetMapping(value = "/export", produces = "text/csv")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public ResponseEntity<String> exportCsv(
    @RequestParam(required = false) LocalDate from,
    @RequestParam(required = false) LocalDate to,
    @RequestParam(required = false) String resourceType,
    @RequestParam(required = false) String faculty
) {
    StringBuilder csv = new StringBuilder();
    csv.append("Tipo,Identificador,Estudiante,Fecha,Resultado,Detalle\n");

    // Llenar filas de Checkouts
    checkouts.findAll().forEach(c -> 
        csv.append(String.format("CHECKOUT,%s,%s,%s,%s,%d min retraso\n",
            c.getReservationId(), c.getStudentCode(), c.getCheckedOutAt(), c.getResult(), c.getDelayMinutes())));

    // Llenar filas de Inasistencias
    absences.findAll().forEach(a -> 
        csv.append(String.format("INASISTENCIA,%s,%s,%s,%s,%s\n",
            a.getReservationId(), a.getStudentCode(), a.getCreatedAt(), a.getStatus(), a.getNote())));

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_analitica_m3.csv\"")
        .body(csv.toString());
}
```

---

## 6. Banco de Preguntas Clave para Repaso y Sustentación

Estas son preguntas típicas de evaluación o revisión de arquitectura que te pueden hacer sobre este proyecto:

### 1. ¿Por qué se utilizó el Patrón Transactional Outbox en lugar de publicar directamente a Kafka desde el Servicio?
> **Respuesta**: Para evitar inconsistencias ante fallos de red. Si guardamos en MySQL y luego llamamos a Kafka, si Kafka no responde, la base de datos ya cambió pero el evento se perdió. Con el Outbox, el evento se guarda en la misma transacción de MySQL; si la transacción falla, nada se guarda. Un worker independiente reintenta el envío a Kafka hasta que se confirme la entrega sin bloquear al usuario.

### 2. ¿Cómo se garantiza que no haya cobros dobles o deducciones repetidas si Kafka reenvía un mensaje?
> **Respuesta**: Mediante el patrón **Inbox / Deduplicación Idempotente**. Al recibir un mensaje, se extrae su `eventId` único y se verifica si ya existe en la tabla `processed_event`. Si ya fue procesado, se descarta de inmediato.

### 3. ¿Cómo se evitan inconsistencias cuando dos procesos intentan modificar el score del mismo estudiante al mismo tiempo?
> **Respuesta**: En `TrustScoreRepository`, el método `findLocked` aplica un bloqueo pesimista a nivel de base de datos (`@Lock(LockModeType.PESSIMISTIC_WRITE)`). Esto genera un `SELECT ... FOR UPDATE` en MySQL, haciendo que cualquier otra transacción espere hasta que termine la primera.

### 4. ¿Por qué las contraseñas de demostración y el secreto JWT tienen restricciones estrictas?
> **Respuesta**: Spring Security y la librería Nimbus exigen que para el algoritmo `HMAC-SHA256` la clave tenga al menos **256 bits (32 caracteres UTF-8)**; de lo contrario, la aplicación se rehúsa a iniciar por motivos de seguridad criptográfica.

### 5. ¿Qué sucede con las reservas canceladas si llega una petición de Check-out?
> **Respuesta**: `CheckOutService` valida que la reserva esté en estado `ACTIVA`. Si la reserva fue cancelada o finalizada previamente, lanza una excepción de dominio con código `RESERVATION_ALREADY_CLOSED` (HTTP 409 Conflict), impidiendo transacciones inválidas.

---
*Fin del documento de estudio.*
