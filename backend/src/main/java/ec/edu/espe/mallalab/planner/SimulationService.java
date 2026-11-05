package ec.edu.espe.mallalab.planner;

import ec.edu.espe.mallalab.api.SimulationRequest;
import ec.edu.espe.mallalab.api.SimulationResponse;
import ec.edu.espe.mallalab.catalog.CurriculumService;
import ec.edu.espe.mallalab.history.PlanRun;
import ec.edu.espe.mallalab.history.PlanRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class SimulationService {

    private static final int MIN_CREDITS = 6;
    private static final int MAX_CREDITS = 40;
    private static final int MAX_TERMS = 40;

    private final CurriculumService curriculum;
    private final PlanRunRepository planRuns;

    public SimulationService(CurriculumService curriculum, PlanRunRepository planRuns) {
        this.curriculum = curriculum;
        this.planRuns = planRuns;
    }

    @Transactional
    public SimulationResponse simulate(SimulationRequest request) {
        CurriculumService.Snapshot snapshot = curriculum.load(request.careerCode());
        PlannerEngine engine = new PlannerEngine(snapshot.graph());

        Set<String> approved = toSet(request.approved());
        Set<String> failed = toSet(request.failed());
        int maxCredits = clamp(request.maxCreditsPerTerm(), PlannerEngine.DEFAULT_MAX_CREDITS, MIN_CREDITS, MAX_CREDITS);
        int maxTerms = clamp(request.maxTerms(), PlannerEngine.DEFAULT_MAX_TERMS, 1, MAX_TERMS);

        PlanResult plan = engine.plan(approved, failed, maxCredits, maxTerms);
        PlanResult baseline = engine.plan(approved, Set.of(), maxCredits, maxTerms);
        int delay = Math.max(0, plan.totalTerms() - baseline.totalTerms());

        if (Boolean.TRUE.equals(request.save())) {
            planRuns.save(new PlanRun(snapshot.career(), approved.size(), failed.size(),
                    String.join(",", failed), maxCredits, plan.totalTerms(), delay));
        }

        return new SimulationResponse(
                snapshot.career().getCode(),
                plan.minTerms(),
                baseline.totalTerms(),
                plan.totalTerms(),
                delay,
                plan.totalCredits(),
                plan.unscheduled(),
                mapTerms(plan.terms()));
    }

    private List<SimulationResponse.TermDto> mapTerms(List<PlannedTerm> terms) {
        return terms.stream()
                .map(term -> new SimulationResponse.TermDto(
                        term.index(),
                        term.credits(),
                        term.courses().stream()
                                .map(c -> new SimulationResponse.PlannedCourseDto(
                                        c.code(), c.name(), c.credits(), c.suggestedTerm(), c.retake()))
                                .toList()))
                .toList();
    }

    private Set<String> toSet(List<String> codes) {
        return codes == null ? Set.of() : new LinkedHashSet<>(codes);
    }

    private int clamp(Integer value, int fallback, int min, int max) {
        int candidate = value == null ? fallback : value;
        return Math.max(min, Math.min(max, candidate));
    }
}
