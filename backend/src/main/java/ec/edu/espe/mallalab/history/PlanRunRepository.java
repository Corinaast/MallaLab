package ec.edu.espe.mallalab.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanRunRepository extends JpaRepository<PlanRun, Long> {

    List<PlanRun> findTop10ByCareerCodeOrderByCreatedAtDesc(String careerCode);
}
