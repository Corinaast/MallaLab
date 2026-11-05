package ec.edu.espe.mallalab.api;

import java.util.List;

/**
 * @param unlocks cuantas materias desbloquea esta materia (directa e indirectamente)
 */
public record CourseDto(
        String code,
        String name,
        int credits,
        int term,
        List<String> requires,
        int unlocks) {
}
