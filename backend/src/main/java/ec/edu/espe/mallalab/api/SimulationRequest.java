package ec.edu.espe.mallalab.api;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Escenario de simulacion. Es un request sin estado: el mismo grafo produce
 * siempre el mismo plan, lo que hace la API facil de cachear y testear.
 *
 * @param save si es true, el escenario se guarda en {@code plan_run} para historial
 */
public record SimulationRequest(
        @NotBlank String careerCode,
        List<String> approved,
        List<String> failed,
        Integer maxCreditsPerTerm,
        Integer maxTerms,
        Boolean save) {
}
