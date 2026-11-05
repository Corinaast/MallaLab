# Algoritmos de MallaLab

Todo el motor vive en `backend/src/main/java/ec/edu/espe/mallalab/planner` y no depende de Spring ni de JPA.

```
CourseSpec        -> materia inmutable (codigo, creditos, termino sugerido, prerrequisitos)
CurriculumGraph   -> DAG con validacion, orden topologico y metricas
PlannerEngine     -> simulacion de semestres con tope de creditos
PlanResult        -> plan + materias sin ubicar
```

## 1. Construccion del grafo

`CurriculumGraph.of(specs)` valida:

- codigos duplicados,
- prerrequisitos que no existen en el catalogo,
- auto-prerrequisitos (`A requiere A`),
- creditos negativos.

Complejidad: `O(V + E)`.

## 2. Deteccion de ciclos (Kahn)

Se calcula el grado de entrada de cada nodo y se procesan los que tienen grado cero. Si al final se procesaron menos nodos que `V`, existe un ciclo. Los nodos no visitados son los atrapados (o dependientes de ellos) y se devuelven en `cycleNodes()` para que la UI pueda señalarlos.

Complejidad: `O(V + E)` con cola de prioridad `O(V log V + E)` para que el orden sea determinista (semestre sugerido, luego codigo).

## 3. Minimo de semestres (camino critico)

No es `creditos / 15`. Es la cadena de prerrequisitos mas larga:

```
nivel(materia) = 1 + max(nivel(prerrequisito))
minTerms       = max(nivel(materia))
```

Se calcula en una pasada sobre el orden topologico. Complejidad `O(V + E)`.

Ejemplo con la malla de TI:

```
Fundamentos de Programacion (1)
  -> POO (2)
    -> Estructura de Datos (3)
      -> Sistemas de Bases de Datos (4)
        -> Practicas Laborales I (5)
          -> Practicas Laborales II (6)
            -> Trabajo de Integracion Curricular (7)
```

Aunque la malla tenga 8 semestres sugeridos, el minimo teorico es **7**.

## 4. Profundidad critica

Para planificar mejor que "por semestre sugerido", cada materia se puntua con la cadena mas larga que queda por debajo:

```
profundidad(materia) = 1 + max(profundidad(dependiente))
```

Se calcula recorriendo el orden topologico al reves. Complejidad `O(V + E)`.

Agendar primero lo que tiene mayor profundidad critica es la heuristica clasica para no alargar el plan (es el mismo espiritu del *critical path method*).

## 5. Cuellos de botella (clausura transitiva)

`unlockCount(materia)` = cuantas materias se desbloquean directa o indirectamente (BFS sobre dependientes, sin contar la propia materia).

Se calcula una vez por simulacion y se cachea en un `Map`. Para 46 nodos el costo es irrelevante; con miles de nodos convendria usar bitsets (`O(V·E/64)`) o memoizacion por SCC.

## 6. Planificacion por semestre

```
pendientes = todas - aprobadas
mientras haya pendientes y queden semestres:
    elegibles = pendientes con todos sus prerrequisitos aprobados
    ordenar elegibles por (profundidad critica desc, unlocks desc, semestre asc, codigo)
    llenar el semestre mientras la suma de creditos <= tope
    confirmar el semestre
```

Casos borde resueltos:

| Caso | Decision |
|---|---|
| Materia con mas creditos que el tope | Se agenda sola en el semestre (no hay deadlock) |
| Materia perdida | Se recursa desde el semestre siguiente al que le tocaba, y se marca `retake: true` |
| Semestre sin materias por recursado | Se conserva el hueco en el indice y se salta al siguiente semestre util |
| Materia aprobada y perdida a la vez | Gana `approved` (se elimina de `failed`) |
| Sin elegibles pero con pendientes | Se corta y se devuelven en `unscheduled` |
| Tope de semestres alcanzado | Se corta y se reporta lo no agendado |

### Modelo de "perder una materia"

Una materia perdida no es simplemente "no aprobada": se pierde en el semestre en
que el plan base la habria cursado. Por eso el motor hace dos pasadas:

1. Calcula el plan base (todo aprobado) para saber **cuando** tocaba cada materia.
2. Replanifica con `retakeFrom = semestreBase + 1`.

Si la materia esta en la cadena critica, el retraso se propaga a todos sus
dependientes. Si es una materia hoja y hay holgura de creditos, el retraso es 0.

Complejidad aproximada: `O(T · V log V)`, donde `T` es el numero de semestres.

## 7. Por que no es optimo (y esta bien)

Planificar materias con prerrequisitos y capacidad es un problema de scheduling. Si se agregan ventanas de horario, cupos y profesores se vuelve **NP-hard** (contiene *bin packing* y *job shop scheduling* como casos particulares).

Alternativas que se descartaron para la v1:

- **Programacion lineal entera (ILP):** optima, pero requiere solver y no explica el plan a un estudiante.
- **Busqueda exhaustiva con backtracking:** exponencial; inviable incluso con 46 materias y tope de creditos.
- **Algoritmo genetico:** produce planes dificiles de reproducir; malo para tests.

La heuristica voraz es **determinista, explicable y suficientemente buena**, y la limitacion queda documentada en vez de escondida.
