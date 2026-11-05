package ec.edu.espe.mallalab.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CareerRepository extends JpaRepository<Career, Long> {

    Optional<Career> findByCode(String code);

    List<Career> findAllByOrderByNameAsc();
}
