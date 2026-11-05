package ec.edu.espe.mallalab.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "OPTIONS")
                .maxAge(3600);
    }

    @Bean
    public OpenAPI mallaLabOpenApi() {
        return new OpenAPI().info(new Info()
                .title("MallaLab API")
                .version("1.0.0")
                .description("Simulador de trayectoria academica sobre un DAG de prerrequisitos"));
    }
}
