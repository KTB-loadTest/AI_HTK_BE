package backend.aihkt.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "AI HKT API", version = "v1"),
        servers = {
                @Server(description = "Local", url = "http://localhost:8080")
        }
)
public class OpenApiConfig {
}
