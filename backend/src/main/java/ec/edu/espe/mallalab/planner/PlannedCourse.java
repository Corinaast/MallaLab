package ec.edu.espe.mallalab.planner;

/** Materia colocada dentro de un semestre del plan simulado. */
public record PlannedCourse(String code, String name, int credits, int suggestedTerm, boolean retake) {
}
