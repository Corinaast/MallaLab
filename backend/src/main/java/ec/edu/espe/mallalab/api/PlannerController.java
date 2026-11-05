package ec.edu.espe.mallalab.api;

import ec.edu.espe.mallalab.history.PlanRun;
import ec.edu.espe.mallalab.history.PlanRunRepository;
import ec.edu.espe.mallalab.planner.SimulationService;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/planner")
public class PlannerController {

    private final SimulationService simulations;
    private final PlanRunRepository planRuns;

    public PlannerController(SimulationService simulations, PlanRunRepository planRuns) {
        this.simulations = simulations;
        this.planRuns = planRuns;
    }

    @PostMapping("/simulate")
    public SimulationResponse simulate(@Valid @RequestBody SimulationRequest request) {
        return simulations.simulate(request);
    }

    @GetMapping("/runs")
    @Transactional(readOnly = true)
    public List<RunSummary> runs(@RequestParam String career) {
        return planRuns.findTop10ByCareerCodeOrderByCreatedAtDesc(career).stream()
                .map(PlannerController::summary)
                .toList();
    }

    private static RunSummary summary(PlanRun run) {
        return new RunSummary(run.getId(), run.getCreatedAt(), run.getApprovedCount(),
                run.getFailedCount(), run.getTotalTerms(), run.getDelay());
    }

    public record RunSummary(Long id, LocalDateTime createdAt, int approvedCount, int failedCount,
                             int totalTerms, int delay) {
    }
}
