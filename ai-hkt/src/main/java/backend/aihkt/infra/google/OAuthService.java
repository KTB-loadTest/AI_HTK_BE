package backend.aihkt.infra.google;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class OAuthService {
    private final WebClient webClient = WebClient.create();

    @Value("${GOOGLE_CLIENT_ID}")
    private String clientId;

    @Value("${GOOGLE_CLIENT_SECRET}")
    private String clientSecret;

    public Map<String, String> exchangeCodeForTokens(String code, String redirectUri) {
        Map response = webClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .bodyValue("client_id=" + clientId +
                        "&client_secret=" + clientSecret +
                        "&code=" + code +
                        "&grant_type=authorization_code" +
                        "&redirect_uri=" + redirectUri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return Map.of(
            "accessToken", (String) response.get("access_token"),
            "refreshToken", (String) response.get("refresh_token")
        );
    }

    public Map<String, Object> getUserInfo(String accessToken) {
        return webClient.get()
                .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    public String refreshAccessToken(String refreshToken) {
        Map<String, Object> response = webClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .bodyValue("client_id=" + clientId +
                           "&client_secret=" + clientSecret +
                           "&refresh_token=" + refreshToken +
                           "&grant_type=refresh_token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return (String) response.get("access_token");
    }
}
