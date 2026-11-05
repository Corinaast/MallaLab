package ec.edu.espe.mallalab.planner;

import java.util.List;

/** Un semestre del plan: {@code index} empieza en 1 y es relativo al inicio de la simulacion. */
public record PlannedTerm(int index, int credits, List<PlannedCourse> courses) {
}
