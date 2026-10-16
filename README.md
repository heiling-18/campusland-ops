# Campusland Ops — Sistema Central de Gestión Operativa

Plataforma centralizada de gestión operativa que unifica espacios, recursos tecnológicos, solicitudes y reportes de incidentes en un solo flujo asistido por Inteligencia Artificial.

## Integrantes del Equipo

| Nombre Completo | Rol en el Equipo |
| :--- | :--- |
| **Heiling Gisselle León Bernal** | Arquitectura Backend, Reglas de Negocio e Integración de IA |
| **Sebastián Feo Murillo** | Desarrollo Frontend, Experiencia de Usuario (Modo Oscuro/Claro) y Módulos |
| **Pedro Díaz** | Modelado de Datos, Inventario de Espacios y Persistencia |
| **Julián Agudelo** | Seguridad, Control de Acceso por Roles y Validaciones de Horarios |
| **Juan Arias Patiño** | QA Operativo, Casos Borde, Documentación y Sustentación |

## Problema

Campuslands crece rápido: aulas, préstamos de equipos e incidencias viven en registros sueltos. El MVP concentra disponibilidad de espacios, inventario/préstamos y triaje de fallas, de modo que **no se asigne un salón o equipo inhabilitado o en conflicto**.

## Stack

- **Backend:** Java 17, Spring Boot 3.2.5, Spring Web, Spring Data JPA
- **Base de datos:** MySQL (oficial) con respaldo automático **H2 en modo MySQL** si el puerto 3306 no está disponible
- **IA:** OpenAI `gpt-4o-mini` (JSON estructurado). Clave solo en `.env` (`AI_API_KEY`)
- **Auth:** JWT HS256; el rol y el usuario salen del token, no del body del cliente
- **Frontend:** HTML + Tailwind (CDN) + JavaScript modular

## Cómo opera la IA (no hay respuestas quemadas)

Tres usos, todos sobre datos vivos de la base:

1. **Triaje de incidencias (flujo vertical):** el modelo clasifica categoría y prioridad. Si queda `CRITICA` tras el control de salida, el salón se bloquea (`origenBloqueo = IA`) y ya no se puede reservar. Soporte al resolver solo rehabilita bloqueos de origen IA, no los manuales.
2. **Recomendación / reubicación de aula:** el modelo elige un `espacioId` del catálogo **ya filtrado** (libre, aforo suficiente). El backend descarta IDs inválidos y reajusta si viola el 50% de ocupación.
3. **Copiloto:** responde con el resumen actual de salones, bloqueos, reservas e incidencias. Tiene instrucción de no inventar datos que no vengan en ese resumen.

Si la API falla o no hay clave, el sistema **no simula un JSON de demo**: usa un motor de reglas local y lo etiqueta como `MOTOR_REGLAS_LOCAL` en pantalla. En la demostración, con `AI_API_KEY` válida, la fuente debe verse como `MODELO_IA`.

Archivo clave: `src/main/java/com/campuslands/ops/service/AiOperationalService.java`.

## Reglas de negocio (reto en vivo: `BusinessRulesConfig.java`)

Para el cambio de 3 minutos, editar solo:

`src/main/java/com/campuslands/ops/config/BusinessRulesConfig.java`

| Constante | Efecto inmediato |
| :--- | :--- |
| `PORCENTAJE_MINIMO_OCUPACION_TRAINER` | Umbral anti-subutilización del Trainer (hoy 50%) |
| `CAPACIDAD_MAXIMA_SALAS_CAMPER` | Tope de salas que ve/reserva un Camper (hoy 12) |
| `MAX_PRESTAMOS_ACTIVOS_CAMPER` | Equipos activos simultáneos por Camper (hoy 1) |

Otras reglas de dominio (no son validación de formulario):

1. **Aforo físico:** nadie reserva por encima de la capacidad del salón (tampoco Admin).
2. **50% Trainer + excepción:** si hay un salón menor libre donde cabe el grupo, se rechaza el aula grande.
3. **No solape** en `[horaInicio, horaFin)` el mismo día; las solicitudes `PENDIENTE` también ocupan el hueco. Al **aprobar**, se revalida bloqueo y solape.
4. **No reservar `BLOQUEADO`.** Crítica de IA bloquea; resolución no abre un bloqueo **MANUAL**.
5. **Camper:** solo salas ≤ 12; 1 préstamo activo; no lotes; no estaciones fijas de salón; no equipos `NO_DISPONIBLE`.
6. **Borrado de salón:** prohibido si hay reservas/solicitudes no rechazadas; se eliminan sus implementos.

## Datos oficiales

`DataInitializer` carga, si la base está vacía:

- 10 espacios (Auditorio, Spooki, Apolo, Artemis, Cosmos, Skilab, salas Blanca/Azul/Amarilla/Verde)
- Seriales de estaciones Spooki (35), Apolo (34) y Artemis (32), TV/HDMI y pool de préstamos Camper
- Usuarios de demostración por rol (credenciales de laboratorio, no cuentas reales de estudiantes)

Las operaciones (reservas, préstamos, incidencias, bloqueos) se **persisten** en MySQL o en `./data/` (H2).

## Instalación y ejecución

Requisitos: **Java 17+**. MySQL en `3306` es opcional (si no está, arranca H2).

1. Copia `.env.example` a `.env` y completa `DB_PASSWORD`, `AI_API_KEY` y `JWT_SECRET`. **No subas `.env`.**
2. En PowerShell:

```powershell
mvn spring-boot:run
```

3. Abre `http://localhost:8080`

### Usuarios de laboratorio

| Rol | Correo | Contraseña |
| :--- | :--- | :--- |
| ADMIN | `admin@campuslands.com` | `admin123` |
| TRAINER | `trainer@campuslands.com` | `trainer123` |
| SOPORTE | `soporte@campuslands.com` | `soporte123` |
| CAMPER | `camper@campuslands.com` | `camper123` |

### Demo sugerida de las tres reglas

1. Trainer pide Spooki para 5 personas → rechazo del 50% (o excepción si no hay aula menor libre).
2. Segunda reserva al mismo salón y horario → conflicto de solape.
3. Incidencia “huele a quemado en Apolo” → `CRITICA` + bloqueo → la reserva a Apolo falla → Soporte resuelve → Apolo vuelve a disponible **solo si el bloqueo fue por IA**.

## Arquitectura

```
Navegador (index.html + js/modules)
        │  JWT Bearer
        ▼
CampusController  →  JwtAuthFilter / RequestAuth
        ▼
OperacionesService (reglas + persistencia JPA)
        ├── AiOperationalService (OpenAI + control de salida)
        └── Repositorios (espacios, reservas, implementos, préstamos, incidencias)
```

Punto de cambio rápido ante el jurado: `BusinessRulesConfig.java`.
