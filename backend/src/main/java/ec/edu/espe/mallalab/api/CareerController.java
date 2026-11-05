package ec.edu.espe.mallalab.api;

import ec.edu.espe.mallalab.catalog.Career;
import ec.edu.espe.mallalab.catalog.CareerRepository;
import ec.edu.espe.mallalab.catalog.CourseRepository;
import ec.edu.espe.mallalab.catalog.CurriculumService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/careers")
public class CareerController {

    private final CareerRepository careers;
    private final CourseRepository courses;
    private final CurriculumService curriculum;

    public CareerController(CareerRepository careers, CourseRepository courses, CurriculumService curriculum) {
        this.careers = careers;
        this.courses = courses;
        this.curriculum = curriculum;
    }

    @GetMapping
    public List<CareerSummary> list() {
        return careers.findAllByOrderByNameAsc().stream().map(this::summary).toList();
    }

    @GetMapping("/{code}/curriculum")
    public CurriculumResponse curriculum(@PathVariable String code) {
        CurriculumService.Snapshot snapshot = curriculum.load(code);
        List<CourseDto> dtos = snapshot.specs().stream()
                .map(spec -> new CourseDto(
                        spec.code(),
                        spec.name(),
                        spec.credits(),
                        spec.suggestedTerm(),
                        List.copyOf(spec.requires()),
                        snapshot.graph().unlockCount(spec.code())))
                .toList();
        return new CurriculumResponse(
                new CareerSummary(snapshot.career().getCode(), snapshot.career().getName(),
                        snapshot.career().getInstitution(), snapshot.career().getTotalCredits(), dtos.size()),
                dtos);
    }

    @GetMapping("/{code}/insights")
    public InsightsResponse insights(@PathVariable String code) {
        CurriculumService.Snapshot snapshot = curriculum.load(code);
        List<BottleneckDto> bottlenecks = snapshot.graph().courses().stream()
                .map(spec -> new BottleneckDto(spec.code(), spec.name(), spec.suggestedTerm(),
                        snapshot.graph().unlockCount(spec.code()), spec.credits()))
                .sorted(Comparator.comparingInt(BottleneckDto::unlocks).reversed()
                        .thenComparingInt(BottleneckDto::term)
                        .thenComparing(BottleneckDto::code))
                .limit(8)
                .toList();

        return new InsightsResponse(code, snapshot.graph().minTerms(), snapshot.graph().size(),
                snapshot.graph().totalCredits(), bottlenecks);
    }

    private CareerSummary summary(Career career) {
        return new CareerSummary(career.getCode(), career.getName(), career.getInstitution(),
                career.getTotalCredits(), (int) courses.countByCareerCode(career.getCode()));
    }
}
