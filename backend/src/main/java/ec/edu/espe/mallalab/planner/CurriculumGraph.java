package ec.edu.espe.mallalab.planner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Grafo dirigido aciclico (DAG) de una malla curricular.
 *
 * <p>Nodos = materias. Arista {@code A -> B} significa "B requiere A".
 * Todo el motor de planificacion trabaja sobre esta estructura, sin depender de
 * Spring ni de la base de datos, lo que permite testearlo de forma aislada.</p>
 */
public final class CurriculumGraph {

    private final Map<String, CourseSpec> courses;
    private final Map<String, Set<String>> prerequisites;
    private final Map<String, Set<String>> dependents;
    private final Comparator<String> catalogOrder;

    private CurriculumGraph(Map<String, CourseSpec> courses,
                            Map<String, Set<String>> prerequisites,
                            Map<String, Set<String>> dependents,
                            Comparator<String> catalogOrder) {
        this.courses = courses;
        this.prerequisites = prerequisites;
        this.dependents = dependents;
        this.catalogOrder = catalogOrder;
    }

    public static CurriculumGraph of(Collection<CourseSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            throw new IllegalArgumentException("La malla no tiene materias");
        }

        Map<String, CourseSpec> byCode = new LinkedHashMap<>();
        for (CourseSpec spec : specs) {
            if (byCode.putIfAbsent(spec.code(), spec) != null) {
                throw new IllegalArgumentException("Codigo de materia duplicado: " + spec.code());
            }
            if (spec.credits() < 0) {
                throw new IllegalArgumentException("Creditos invalidos en " + spec.code());
            }
        }

        Comparator<String> order = Comparator
                .comparingInt((String code) -> byCode.get(code).suggestedTerm())
                .thenComparing(Comparator.naturalOrder());

        Map<String, Set<String>> prerequisites = new LinkedHashMap<>();
        Map<String, Set<String>> dependents = new LinkedHashMap<>();
        byCode.keySet().forEach(code -> {
            prerequisites.put(code, new LinkedHashSet<>());
            dependents.put(code, new LinkedHashSet<>());
        });

        for (CourseSpec spec : byCode.values()) {
            for (String required : spec.requires()) {
                if (!byCode.containsKey(required)) {
                    throw new IllegalArgumentException(
                            "La materia " + spec.code() + " requiere " + required + ", que no existe en la malla");
                }
                if (required.equals(spec.code())) {
                    throw new IllegalArgumentException("La materia " + spec.code() + " se requiere a si misma");
                }
                prerequisites.get(spec.code()).add(required);
                dependents.get(required).add(spec.code());
            }
        }

        return new CurriculumGraph(byCode, prerequisites, dependents, order);
    }

    public int size() {
        return courses.size();
    }

    public Collection<CourseSpec> courses() {
        return courses.values();
    }

    public CourseSpec course(String code) {
        CourseSpec spec = courses.get(code);
        if (spec == null) {
            throw new IllegalArgumentException("Materia desconocida: " + code);
        }
        return spec;
    }

    public boolean contains(String code) {
        return courses.containsKey(code);
    }

    public Set<String> prerequisiteCodes(String code) {
        return Set.copyOf(prerequisites.getOrDefault(code, Set.of()));
    }

    public Set<String> dependentCodes(String code) {
        return Set.copyOf(dependents.getOrDefault(code, Set.of()));
    }

    public int totalCredits() {
        return courses.values().stream().mapToInt(CourseSpec::credits).sum();
    }

    /** Orden topologico determinista. Lanza excepcion si el grafo tiene ciclos. */
    public List<String> topologicalOrder() {
        List<String> order = kahn();
        if (order.size() != courses.size()) {
            throw new IllegalStateException("La malla contiene ciclos: " + cycleNodes());
        }
        return List.copyOf(order);
    }

    public boolean hasCycle() {
        return kahn().size() != courses.size();
    }

    /**
     * Devuelve los nodos que quedan atrapados por un ciclo (los que nunca llegan
     * a grado de entrada cero). Util para señalar el error en la UI.
     */
    public List<String> cycleNodes() {
        List<String> order = kahn();
        Set<String> visited = new HashSet<>(order);
        List<String> stuck = new ArrayList<>();
        for (String code : courses.keySet()) {
            if (!visited.contains(code)) {
                stuck.add(code);
            }
        }
        return List.copyOf(stuck);
    }

    /**
     * Minimo de semestres teoricos para cubrir la malla: la cadena de
     * prerrequisitos mas larga. Es el "camino critico" del DAG.
     */
    public int minTerms() {
        return minTermByCourse().values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    /**
     * Nivel minimo de cada materia: {@code 1 + max(nivel de sus prerrequisitos)}.
     * Es la distancia mas larga desde una raiz, no la mas corta.
     */
    public Map<String, Integer> minTermByCourse() {
        List<String> order = topologicalOrder();
        Map<String, Integer> level = new HashMap<>();
        for (String code : order) {
            int deepest = 0;
            for (String required : prerequisites.get(code)) {
                deepest = Math.max(deepest, level.getOrDefault(required, 1));
            }
            level.put(code, deepest + 1);
        }
        Map<String, Integer> byCatalogOrder = new LinkedHashMap<>();
        for (String code : courses.keySet()) {
            byCatalogOrder.put(code, level.get(code));
        }
        return byCatalogOrder;
    }

    /** Cuantas materias desbloquea directa e indirectamente una materia. */
    public int unlockCount(String code) {
        return descendants(code).size();
    }

    /**
     * Profundidad critica: longitud de la cadena mas larga que empieza en esta
     * materia y termina en una hoja del DAG. Planificar primero lo que tiene
     * mayor profundidad critica es la heuristica clasica para no alargar el plan.
     */
    public int criticalDepth(String code) {
        return criticalDepthByCourse().getOrDefault(code, 0);
    }

    public Map<String, Integer> criticalDepthByCourse() {
        List<String> order = topologicalOrder();
        Map<String, Integer> depth = new HashMap<>();
        for (int i = order.size() - 1; i >= 0; i--) {
            String code = order.get(i);
            int deepest = 0;
            for (String dependent : dependents.get(code)) {
                deepest = Math.max(deepest, depth.getOrDefault(dependent, 0));
            }
            depth.put(code, deepest + 1);
        }
        return depth;
    }

    /** Clausura transitiva sobre las aristas {@code course -> dependents}. */
    public Set<String> descendants(String code) {
        course(code);
        Set<String> seen = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>(dependents.get(code));
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (seen.add(current)) {
                queue.addAll(dependents.get(current));
            }
        }
        seen.remove(code);
        return seen;
    }

    private List<String> kahn() {
        Map<String, Integer> indegree = new HashMap<>();
        courses.keySet().forEach(code -> indegree.put(code, prerequisites.get(code).size()));

        PriorityQueue<String> ready = new PriorityQueue<>(catalogOrder);
        indegree.forEach((code, degree) -> {
            if (degree == 0) {
                ready.add(code);
            }
        });

        List<String> order = new ArrayList<>(courses.size());
        while (!ready.isEmpty()) {
            String code = ready.poll();
            order.add(code);
            for (String dependent : dependents.get(code)) {
                int remaining = indegree.merge(dependent, -1, Integer::sum);
                if (remaining == 0) {
                    ready.add(dependent);
                }
            }
        }
        return order;
    }
}
