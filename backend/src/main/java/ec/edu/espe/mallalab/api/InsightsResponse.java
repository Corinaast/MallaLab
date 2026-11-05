package ec.edu.espe.mallalab.api;

import java.util.List;

public record InsightsResponse(
        String careerCode,
        int minTerms,
        int courseCount,
        int totalCredits,
        List<BottleneckDto> bottlenecks) {
}
