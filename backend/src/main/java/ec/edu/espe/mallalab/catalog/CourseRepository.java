package ec.edu.espe.mallalab.catalog;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @EntityGraph(attributePaths = {"prerequisites", "prerequisites.requiredCourse"})
    List<Course> findByCareerCodeOrderBySuggestedTermAscCodeAsc(String careerCode);

    long countByCareerCode(String careerCode);
}
