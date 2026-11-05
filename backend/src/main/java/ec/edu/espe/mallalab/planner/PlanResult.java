package ec.edu.espe.mallalab.planner;

import java.util.List;

/**
 * Resultado de una simulacion de trayectoria.
 *
 * @param terms        semestres con al menos una materia (puede haber huecos en {@code index})
 * @param totalTerms   semestres transcurridos hasta terminar el plan, contando huecos
 * @param minTerms     minimo teorico (camino critico del DAG, sin tope de creditos)
 * @param unscheduled  materias que no se pudieron ubicar
 * @param totalCredits creditos totales planificados
 */
public record PlanResult(
        List<PlannedTerm> terms,
        int totalTerms,
        int minTerms,
        List<String> unscheduled,
        int totalCredits) {
}
