package ec.edu.espe.mallalab.api;

import java.util.List;

public record CurriculumResponse(CareerSummary career, List<CourseDto> courses) {
}
