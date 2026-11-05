# Arquitectura

```mermaid
flowchart TD
    subgraph Frontend["frontend · React 18 + TS + Vite"]
        APP[App.tsx<br/>estado del escenario]
        GRAPH[CurriculumGraph<br/>React Flow]
        SIDE[Sidebar<br/>plan + cuellos de botella]
        API_CLIENT[api.ts<br/>fetch tipado]
        APP --> GRAPH
        APP --> SIDE
        APP --> API_CLIENT
    end

    subgraph Backend["backend · Spring Boot 3.5"]
        CTRL[CareerController<br/>PlannerController]
        SIM[SimulationService]
        CUR[CurriculumService]
        ENGINE[PlannerEngine<br/>Java puro]
        REPO[Repositorios JPA]
        CTRL --> SIM
        CTRL --> CUR
        SIM --> ENGINE
        SIM --> REPO
        CUR --> ENGINE
        CUR --> REPO
    end

    DB[(PostgreSQL / H2)]
    API_CLIENT -->|JSON| CTRL
    REPO --> DB
```

## Capas

| Capa | Paquete | Responsabilidad |
|---|---|---|
| API | `api` | DTOs, validacion, codigos HTTP, OpenAPI |
| Dominio | `planner` | Grafo, camino critico, planificador (sin frameworks) |
| Aplicacion | `catalog`, `history` | Carga de mallas, simulaciones guardadas |
| Persistencia | `catalog`, `history` | Entidades JPA y repositorios |
| Seed | `seed` | Carga idempotente de la malla desde JSON |

## Flujo de una simulacion

```mermaid
sequenceDiagram
    participant U as Usuario
    participant UI as React
    participant API as SimulationService
    participant DB as Repositorio
    participant E as PlannerEngine
    U->>UI: clic en "Perder" sobre Estructura de Datos
    UI->>API: POST /api/planner/simulate
    API->>DB: cargar carrera + prerrequisitos
    DB-->>API: 46 filas
    API->>E: construir grafo + planificar
    E-->>API: plan, minimo teorico, sin ubicar
    API-->>UI: JSON con totalTerms y delay
    UI-->>U: grafo recoloreado + "te retrasas 1 semestre"
```

## Reglas de dependencia

- `planner` no importa nada de Spring, JPA ni de `api`.
- `api` no expone entidades JPA; siempre DTOs.
- La UI nunca calcula el plan: solo pinta lo que devuelve la API (salvo el modo lectura, que solo dibuja el grafo).
