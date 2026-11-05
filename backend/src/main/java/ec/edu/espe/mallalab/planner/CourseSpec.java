package ec.edu.espe.mallalab.planner;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Representacion inmutable de una materia dentro del grafo.
 *
 * @param code          codigo unico dentro de la carrera (ej. COMP-A0J09)
 * @param name          nombre visible
 * @param credits       creditos academicos
 * @param suggestedTerm semestre sugerido por la malla (1..N)
 * @param requires      codigos de materias que deben estar aprobadas antes
 */
public record CourseSpec(String code, String name, int credits, int suggestedTerm, Set<String> requires) {

    public CourseSpec {
        requires = requires == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(requires));
    }

    public CourseSpec(String code, String name, int credits, int suggestedTerm) {
        this(code, name, credits, suggestedTerm, Set.of());
    }
}
