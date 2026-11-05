package ec.edu.espe.mallalab.planner;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurriculumGraphTest {

    private static CourseSpec course(String code, int term, int credits, String... requires) {
        return new CourseSpec(code, code, credits, term, Set.of(requires));
    }

    @Test
    void topologicalOrderPutsPrerequisitesFirst() {
        CurriculumGraph graph = CurriculumGraph.of(List.of(
                course("C", 3, 3, "B"),
                course("A", 1, 3),
                course("B", 2, 3, "A")));

        assertThat(graph.topologicalOrder()).containsSubsequence("A", "B", "C");
        assertThat(graph.hasCycle()).isFalse();
    }

    @Test
    void detectsCyclesInsteadOfLoopingForever() {
        CurriculumGraph graph = CurriculumGraph.of(List.of(
                course("A", 1, 3, "C"),
                course("B", 1, 3, "A"),
                course("C", 1, 3, "B")));

        assertThat(graph.hasCycle()).isTrue();
        assertThat(graph.cycleNodes()).containsExactlyInAnyOrder("A", "B", "C");
        assertThatThrownBy(graph::minTerms).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void minTermsIsTheLongestChainNotTheCourseCount() {
        CurriculumGraph graph = CurriculumGraph.of(List.of(
                course("A", 1, 3),
                course("B", 2, 3, "A"),
                course("C", 3, 3, "B"),
                course("D", 2, 3, "A"),
                course("E", 1, 3)));

        assertThat(graph.minTerms()).isEqualTo(3);
    }

    @Test
    void unlockCountCountsTransitiveDependentsOnly() {
        CurriculumGraph graph = CurriculumGraph.of(List.of(
                course("A", 1, 3),
                course("B", 2, 3, "A"),
                course("C", 3, 3, "B"),
                course("D", 2, 3, "A"),
                course("E", 1, 3)));

        assertThat(graph.unlockCount("A")).isEqualTo(3);
        assertThat(graph.unlockCount("B")).isEqualTo(1);
        assertThat(graph.unlockCount("C")).isZero();
        assertThat(graph.unlockCount("E")).isZero();
    }

    @Test
    void criticalDepthMeasuresTheLongestPathToALeaf() {
        CurriculumGraph graph = CurriculumGraph.of(List.of(
                course("A", 1, 3),
                course("B", 2, 3, "A"),
                course("C", 3, 3, "B")));

        assertThat(graph.criticalDepth("A")).isEqualTo(3);
        assertThat(graph.criticalDepth("C")).isEqualTo(1);
    }

    @Test
    void rejectsARequirementThatIsNotInTheCatalog() {
        assertThatThrownBy(() -> CurriculumGraph.of(List.of(course("A", 1, 3, "Z"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Z");
    }

    @Test
    void rejectsDuplicateCourseCodes() {
        assertThatThrownBy(() -> CurriculumGraph.of(List.of(course("A", 1, 3), course("A", 1, 3))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A");
    }

    @Test
    void totalsCreditsOfTheWholeCatalog() {
        CurriculumGraph graph = CurriculumGraph.of(List.of(
                course("A", 1, 4), course("B", 1, 3), course("C", 1, 2)));

        assertThat(graph.totalCredits()).isEqualTo(9);
        assertThat(graph.size()).isEqualTo(3);
    }
}
