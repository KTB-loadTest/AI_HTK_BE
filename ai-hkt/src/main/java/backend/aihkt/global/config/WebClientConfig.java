package backend.aihkt.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        HttpClient jdkHttpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        return WebClient.builder()
                .clientConnector(new JdkClientHttpConnector(jdkHttpClient))
                .build();
    }
}
