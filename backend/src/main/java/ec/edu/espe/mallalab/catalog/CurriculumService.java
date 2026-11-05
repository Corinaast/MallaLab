package ec.edu.espe.mallalab.catalog;

import ec.edu.espe.mallalab.planner.CourseSpec;
import ec.edu.espe.mallalab.planner.CurriculumGraph;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Carga una carrera desde la base de datos y la convierte en un grafo en memoria. */
@Service
public class CurriculumService {

    private final CareerRepository careers;
    private final CourseRepository courses;

    public CurriculumService(CareerRepository careers, CourseRepository courses) {
        this.careers = careers;
        this.courses = courses;
    }

    @Transactional(readOnly = true)
    public Snapshot load(String careerCode) {
        Career career = careers.findByCode(careerCode)
                .orElseThrow(() -> new NotFoundException("No existe la carrera '" + careerCode + "'"));

        List<Course> rows = courses.findByCareerCodeOrderBySuggestedTermAscCodeAsc(careerCode);
        List<CourseSpec> specs = rows.stream().map(this::toSpec).toList();

        return new Snapshot(career, specs, CurriculumGraph.of(specs));
    }

    private CourseSpec toSpec(Course course) {
        Set<String> requires = new LinkedHashSet<>();
        course.getPrerequisites().forEach(p -> requires.add(p.getRequiredCourse().getCode()));
        return new CourseSpec(course.getCode(), course.getName(), course.getCredits(),
                course.getSuggestedTerm(), requires);
    }

    public record Snapshot(Career career, List<CourseSpec> specs, CurriculumGraph graph) {
    }
}
