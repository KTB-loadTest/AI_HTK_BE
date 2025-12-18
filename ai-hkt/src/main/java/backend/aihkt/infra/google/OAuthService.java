package backend.aihkt.infra.google;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
public class OAuthService {
    private final WebClient webClient = WebClient.create();

    @Value("${GOOGLE_CLIENT_ID}")
    private String clientId;

    @Value("${GOOGLE_CLIENT_SECRET}")
    private String clientSecret;

    private static final List<String> SCOPES = List.of(
            "https://www.googleapis.com/auth/youtube.force-ssl",
            "https://www.googleapis.com/auth/yt-analytics.readonly",
            "https://www.googleapis.com/auth/yt-analytics-monetary.readonly"
    );

    public String buildGoogleAuthorizeUrl(String redirectUri, String state) {
        return UriComponentsBuilder
                .fromPath("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", String.join(" ", SCOPES))
                .queryParam("access_type", "offline")  // refresh token 받기
                .queryParam("prompt", "consent")       // scope 변경/재동의 시 refresh token 재발급 유도
                .queryParam("state", state)
                .build(true)
                .toUriString();
    }

    public Map<String, String> exchangeCodeForTokens(String code, String redirectUri) {
        Map response = webClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("code", code)
                        .with("grant_type", "authorization_code")
                        .with("redirect_uri", redirectUri))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return Map.of(
                "accessToken", (String) response.get("access_token"),
                "refreshToken", (String) response.get("refresh_token")
        );
    }

    public String refreshAccessToken(String refreshToken) {
        Map response = webClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("refresh_token", refreshToken)
                        .with("grant_type", "refresh_token"))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return (String) response.get("access_token");
    }

    public Map getUserInfo(String accessToken) {
        return webClient.get()
                .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}
