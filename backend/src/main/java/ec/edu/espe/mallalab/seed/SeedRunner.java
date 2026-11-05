package ec.edu.espe.mallalab.seed;

import ec.edu.espe.mallalab.catalog.Career;
import ec.edu.espe.mallalab.catalog.CareerRepository;
import ec.edu.espe.mallalab.catalog.Course;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga la malla semilla la primera vez que arranca la aplicacion.
 * Es idempotente: si la carrera ya existe, no hace nada.
 */
@Component
public class SeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);
    private static final String SEED_RESOURCE = "seed/malla-ti-espe.json";

    private final CareerRepository careers;
    private final ObjectMapper mapper;
    private final boolean enabled;

    public SeedRunner(CareerRepository careers, ObjectMapper mapper,
                      @Value("${mallalab.seed.enabled:true}") boolean enabled) {
        this.careers = careers;
        this.mapper = mapper;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) {
            return;
        }
        try (InputStream input = new ClassPathResource(SEED_RESOURCE).getInputStream()) {
            SeedPayload payload = mapper.readValue(input, SeedPayload.class);
            if (careers.findByCode(payload.code()).isPresent()) {
                log.info("La malla {} ya existe, no se vuelve a cargar", payload.code());
                return;
            }

            Career career = new Career(payload.code(), payload.name(), payload.institution(), payload.totalCredits());
            Map<String, Course> byCode = new LinkedHashMap<>();
            for (SeedCourse seed : payload.courses()) {
                Course course = new Course(seed.code(), seed.name(), seed.credits(), seed.term());
                byCode.put(seed.code(), course);
                career.addCourse(course);
            }
            for (SeedCourse seed : payload.courses()) {
                for (String required : seed.requires()) {
                    Course prerequisite = byCode.get(required);
                    if (prerequisite == null) {
                        throw new IllegalStateException(
                                "La malla semilla referencia una materia inexistente: " + required);
                    }
                    byCode.get(seed.code()).addPrerequisite(prerequisite);
                }
            }

            careers.save(career);
            log.info("Malla {} cargada: {} materias, {} creditos",
                    payload.code(), payload.courses().size(), payload.totalCredits());
        }
    }

    public record SeedPayload(String code, String name, String institution, int totalCredits,
                              List<SeedCourse> courses) {
    }

    public record SeedCourse(String code, String name, int credits, int term, List<String> requires) {

        public SeedCourse {
            requires = requires == null ? List.of() : List.copyOf(requires);
        }
    }
}
