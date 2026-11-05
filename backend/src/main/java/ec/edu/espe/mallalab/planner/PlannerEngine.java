package ec.edu.espe.mallalab.planner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Motor de planificacion. Heuristica voraz por semestre:
 *
 * <ol>
 *   <li>Filtra las materias pendientes cuyos prerrequisitos ya estan aprobados.</li>
 *   <li>Las ordena por profundidad critica (cuanto queda por debajo), impacto y
 *       semestre sugerido, para que el plan sea estable y reproducible.</li>
 *   <li>Las coloca mientras no superen el tope de creditos del semestre.</li>
 * </ol>
 *
 * <p><b>Materias perdidas:</b> se asume que el estudiante pierde la materia en el
 * semestre en que la habria cursado (segun el plan base) y la recursa a partir del
 * semestre siguiente. Eso es lo que produce el retraso real: si la materia esta en
 * la cadena critica, arrastra a todos sus dependientes.</p>
 *
 * <p>No es una solucion optima (el problema general con horarios y cupos es
 * NP-hard), pero es determinista, explicable y suficientemente buena para un plan
 * de estudios. Ver docs/algorithms.md.</p>
 */
public final class PlannerEngine {

    public static final int DEFAULT_MAX_CREDITS = 18;
    public static final int DEFAULT_MAX_TERMS = 24;

    private final CurriculumGraph graph;
    private final Map<String, Integer> unlockCounts;
    private final Map<String, Integer> criticalDepths;

    public PlannerEngine(CurriculumGraph graph) {
        this.graph = graph;
        this.unlockCounts = graph.courses().stream()
                .collect(Collectors.toMap(CourseSpec::code, c -> graph.unlockCount(c.code())));
        this.criticalDepths = graph.criticalDepthByCourse();
    }

    public PlanResult plan(Set<String> approved, Set<String> failed, int maxCreditsPerTerm, int maxTerms) {
        if (maxCreditsPerTerm < 1) {
            throw new IllegalArgumentException("maxCreditsPerTerm debe ser mayor que cero");
        }
        if (maxTerms < 1) {
            throw new IllegalArgumentException("maxTerms debe ser mayor que cero");
        }

        Set<String> approvedNow = validated(approved, "approved");
        Set<String> failedNow = validated(failed, "failed");
        approvedNow.removeAll(failedNow);

        return simulate(approvedNow, failedNow, retakeSchedule(approvedNow, failedNow, maxCreditsPerTerm, maxTerms),
                maxCreditsPerTerm, maxTerms);
    }

    /**
     * Semestre a partir del cual se puede recursar cada materia perdida:
     * {@code semestre base + 1}. Si la materia ni siquiera entraba en el plan base,
     * se empuja fuera del horizonte.
     */
    private Map<String, Integer> retakeSchedule(Set<String> approved, Set<String> failed,
                                                int maxCreditsPerTerm, int maxTerms) {
        if (failed.isEmpty()) {
            return Map.of();
        }

        PlanResult baseline = simulate(approved, Set.of(), Map.of(), maxCreditsPerTerm, maxTerms);
        Map<String, Integer> termOf = new HashMap<>();
        for (PlannedTerm term : baseline.terms()) {
            for (PlannedCourse course : term.courses()) {
                termOf.put(course.code(), term.index());
            }
        }

        Map<String, Integer> retakeFrom = new HashMap<>();
        for (String code : failed) {
            retakeFrom.put(code, termOf.getOrDefault(code, maxTerms) + 1);
        }
        return retakeFrom;
    }

    private PlanResult simulate(Set<String> approved, Set<String> failed, Map<String, Integer> retakeFrom,
                                int maxCreditsPerTerm, int maxTerms) {
        Set<String> approvedNow = new LinkedHashSet<>(approved);
        Set<String> pending = new LinkedHashSet<>(graph.courses().stream().map(CourseSpec::code).toList());
        pending.removeAll(approvedNow);

        Comparator<String> priority = Comparator
                .comparingInt((String code) -> criticalDepths.getOrDefault(code, 0))
                .reversed()
                .thenComparing(Comparator.comparingInt((String code) -> unlockCounts.getOrDefault(code, 0)).reversed())
                .thenComparingInt(code -> graph.course(code).suggestedTerm())
                .thenComparing(Comparator.naturalOrder());

        List<PlannedTerm> terms = new ArrayList<>();
        int totalCredits = 0;
        int term = 0;

        while (!pending.isEmpty() && term < maxTerms) {
            term++;
            final int currentTerm = term;

            List<String> eligible = pending.stream()
                    .filter(code -> approvedNow.containsAll(graph.prerequisiteCodes(code)))
                    .filter(code -> currentTerm >= retakeFrom.getOrDefault(code, 0))
                    .sorted(priority)
                    .toList();

            List<PlannedCourse> chosen = new ArrayList<>();
            int termCredits = 0;
            for (String code : eligible) {
                CourseSpec spec = graph.course(code);
                boolean fits = chosen.isEmpty() || termCredits + spec.credits() <= maxCreditsPerTerm;
                if (fits) {
                    chosen.add(new PlannedCourse(
                            spec.code(), spec.name(), spec.credits(), spec.suggestedTerm(), failed.contains(code)));
                    termCredits += spec.credits();
                }
            }

            if (chosen.isEmpty()) {
                // Semestre bloqueado por recursado: saltar directo al siguiente
                // semestre en el que algo se pueda cursar (deja el hueco visible).
                int nextAllowed = pending.stream()
                        .mapToInt(code -> retakeFrom.getOrDefault(code, 0))
                        .filter(candidate -> candidate > currentTerm)
                        .min()
                        .orElse(-1);
                if (nextAllowed < 0) {
                    break;
                }
                term = Math.min(nextAllowed, maxTerms + 1) - 1;
                continue;
            }

            terms.add(new PlannedTerm(term, termCredits, List.copyOf(chosen)));
            totalCredits += termCredits;
            for (PlannedCourse course : chosen) {
                pending.remove(course.code());
                approvedNow.add(course.code());
            }
        }

        return new PlanResult(List.copyOf(terms), term, graph.minTerms(), List.copyOf(pending), totalCredits);
    }

    private Set<String> validated(Set<String> codes, String field) {
        Set<String> copy = new LinkedHashSet<>(codes == null ? Set.of() : codes);
        for (String code : copy) {
            if (!graph.contains(code)) {
                throw new IllegalArgumentException("Materia desconocida en '" + field + "': " + code);
            }
        }
        return copy;
    }
}
