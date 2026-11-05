package ec.edu.espe.mallalab.api;

import java.util.List;

/**
 * @param minTerms      minimo teorico (camino critico)
 * @param baselineTerms semestres sin aplicar las materias perdidas
 * @param delay         semestres extra causados por las materias perdidas
 */
public record SimulationResponse(
        String careerCode,
        int minTerms,
        int baselineTerms,
        int totalTerms,
        int delay,
        int totalCredits,
        List<String> unscheduled,
        List<TermDto> terms) {

    public record TermDto(int index, int credits, List<PlannedCourseDto> courses) {
    }

    public record PlannedCourseDto(String code, String name, int credits, int suggestedTerm, boolean retake) {
    }
}
