package ec.edu.espe.mallalab.planner;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlannerEngineTest {

    private static CourseSpec course(String code, int term, int credits, String... requires) {
        return new CourseSpec(code, code, credits, term, Set.of(requires));
    }

    private static PlanResult plan(PlannerEngine engine, Set<String> approved, Set<String> failed,
                                   int maxCredits, int maxTerms) {
        return engine.plan(approved, failed, maxCredits, maxTerms);
    }

    @Test
    void respectsPrerequisiteChainsEvenWithUnlimitedCredits() {
        PlannerEngine engine = new PlannerEngine(CurriculumGraph.of(List.of(
                course("A", 1, 3),
                course("B", 2, 3, "A"),
                course("C", 3, 3, "B"))));

        PlanResult result = plan(engine, Set.of(), Set.of(), 40, 10);

        assertThat(result.totalTerms()).isEqualTo(3);
        assertThat(result.unscheduled()).isEmpty();
        assertThat(result.terms().get(0).courses()).extracting(PlannedCourse::code).containsExactly("A");
        assertThat(result.terms().get(2).courses()).extracting(PlannedCourse::code).containsExactly("C");
    }

    @Test
    void neverExceedsTheCreditCap() {
        PlannerEngine engine = new PlannerEngine(CurriculumGraph.of(List.of(
                course("A", 1, 4),
                course("B", 1, 4))));

        PlanResult result = plan(engine, Set.of(), Set.of(), 4, 10);

        assertThat(result.totalTerms()).isEqualTo(2);
        assertThat(result.terms()).allSatisfy(term -> assertThat(term.credits()).isLessThanOrEqualTo(4));
    }

    @Test
    void anOversizedCourseStillGetsScheduledAloneInsteadOfDeadlocking() {
        PlannerEngine engine = new PlannerEngine(CurriculumGraph.of(List.of(course("A", 1, 6))));

        PlanResult result = plan(engine, Set.of(), Set.of(), 4, 10);

        assertThat(result.totalTerms()).isEqualTo(1);
        assertThat(result.terms().get(0).credits()).isEqualTo(6);
    }

    @Test
    void approvedCoursesAreNotPlannedAgain() {
        PlannerEngine engine = new PlannerEngine(CurriculumGraph.of(List.of(
                course("A", 1, 3),
                course("B", 2, 3, "A"))));

        PlanResult result = plan(engine, Set.of("A"), Set.of(), 20, 10);

        assertThat(result.totalTerms()).isEqualTo(1);
        assertThat(result.terms().get(0).courses()).extracting(PlannedCourse::code).containsExactly("B");
    }

    @Test
    void aFailedCourseIsPlannedAgainAndMarkedAsRetake() {
        PlannerEngine engine = new PlannerEngine(CurriculumGraph.of(List.of(
                course("A", 1, 3),
                course("B", 2, 3, "A"))));

        PlanResult result = plan(engine, Set.of("A"), Set.of("B"), 20, 10);

        assertThat(result.totalTerms()).isEqualTo(2);
        assertThat(result.terms()).singleElement()
                .satisfies(term -> {
                    assertThat(term.index()).isEqualTo(2);
                    assertThat(term.courses()).singleElement()
                            .satisfies(course -> {
                                assertThat(course.code()).isEqualTo("B");
                                assertThat(course.retake()).isTrue();
                            });
                });
    }

    @Test
    void failingACourseOnTheCriticalPathDelaysTheWholePlan() {
        CurriculumGraph graph = CurriculumGraph.of(List.of(
                course("A", 1, 3),
                course("B", 2, 3, "A"),
                course("C", 3, 3, "B")));
        PlannerEngine engine = new PlannerEngine(graph);

        PlanResult onTrack = plan(engine, Set.of(), Set.of(), 20, 10);
        PlanResult failingB = plan(engine, Set.of(), Set.of("B"), 20, 10);

        assertThat(onTrack.totalTerms()).isEqualTo(3);
        assertThat(failingB.totalTerms()).isEqualTo(4);
        assertThat(failingB.totalTerms() - onTrack.totalTerms()).isEqualTo(1);
        assertThat(failingB.terms()).extracting(PlannedTerm::index).containsExactly(1, 3, 4);
    }

    @Test
    void everyCourseAppearsExactlyOnceAndAfterItsPrerequisites() {
        CurriculumGraph graph = CurriculumGraph.of(List.of(
                course("A", 1, 3),
                course("B", 2, 3, "A"),
                course("C", 2, 2, "A"),
                course("D", 3, 3, "B", "C"),
                course("E", 1, 4)));

        PlanResult result = plan(new PlannerEngine(graph), Set.of(), Set.of(), 18, 20);

        List<String> scheduled = result.terms().stream()
                .flatMap(term -> term.courses().stream())
                .map(PlannedCourse::code)
                .toList();

        assertThat(scheduled).hasSize(5).doesNotHaveDuplicates();
        assertThat(result.unscheduled()).isEmpty();

        for (PlannedTerm term : result.terms()) {
            Set<String> doneBefore = result.terms().stream()
                    .filter(t -> t.index() < term.index())
                    .flatMap(t -> t.courses().stream())
                    .map(PlannedCourse::code)
                    .collect(java.util.stream.Collectors.toSet());
            for (PlannedCourse planned : term.courses()) {
                assertThat(doneBefore).containsAll(graph.prerequisiteCodes(planned.code()));
            }
        }
    }

    @Test
    void rejectsUnknownCourseCodes() {
        PlannerEngine engine = new PlannerEngine(CurriculumGraph.of(List.of(course("A", 1, 3))));

        assertThatThrownBy(() -> plan(engine, Set.of("NOPE"), Set.of(), 18, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NOPE");
    }

    @Test
    void stopsWhenTheTermBudgetRunsOut() {
        PlannerEngine engine = new PlannerEngine(CurriculumGraph.of(List.of(
                course("A", 1, 4), course("B", 1, 4))));

        PlanResult result = plan(engine, Set.of(), Set.of(), 4, 1);

        assertThat(result.totalTerms()).isEqualTo(1);
        assertThat(result.unscheduled()).hasSize(1);
    }
}
