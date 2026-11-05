package ec.edu.espe.mallalab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MallaLabApplicationTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void seedsTheCareerAndServesItsCurriculum() throws Exception {
        mvc.perform(get("/api/careers/TI-ESPE/curriculum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.career.code").value("TI-ESPE"))
                .andExpect(jsonPath("$.courses.length()").value(46))
                .andExpect(jsonPath("$.courses[0].code").exists());
    }

    @Test
    void computesInsightsWithBottlenecks() throws Exception {
        mvc.perform(get("/api/careers/TI-ESPE/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minTerms").value(7))
                .andExpect(jsonPath("$.totalCredits").value(120))
                .andExpect(jsonPath("$.bottlenecks.length()").value(8))
                .andExpect(jsonPath("$.bottlenecks[0].unlocks").isNumber());
    }

    @Test
    void simulatesAPlanFromScratch() throws Exception {
        String body = """
                {
                  "careerCode": "TI-ESPE",
                  "approved": [],
                  "failed": [],
                  "maxCreditsPerTerm": 18
                }
                """;

        mvc.perform(post("/api/planner/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careerCode").value("TI-ESPE"))
                .andExpect(jsonPath("$.minTerms").value(7))
                .andExpect(jsonPath("$.totalTerms").isNumber())
                .andExpect(jsonPath("$.terms.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void returns404ForAnUnknownCareer() throws Exception {
        mvc.perform(get("/api/careers/NOPE/curriculum"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns400ForAnUnknownCourseInTheScenario() throws Exception {
        String body = """
                {"careerCode":"TI-ESPE","approved":["NO-EXISTE"],"failed":[]}
                """;

        mvc.perform(post("/api/planner/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
