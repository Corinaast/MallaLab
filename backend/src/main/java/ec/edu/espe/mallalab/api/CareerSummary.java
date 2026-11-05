package ec.edu.espe.mallalab.api;

public record CareerSummary(
        String code,
        String name,
        String institution,
        int totalCredits,
        int courseCount) {
}
