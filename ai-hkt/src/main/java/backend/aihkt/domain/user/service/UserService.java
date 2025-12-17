package backend.aihkt.domain.user.service;

import backend.aihkt.domain.user.entity.Users;
import backend.aihkt.domain.user.repository.UserRepository;
import backend.aihkt.infra.google.OAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final OAuthService oauthService;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    public String getLoginUrl() {
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + "YOUR_CLIENT_ID" +
                "&redirect_uri=" + redirectUri +
                "&scope=openid%20profile%20email%20https://www.googleapis.com/auth/youtube.upload%20https://www.googleapis.com/auth/youtube.readonly" +
                "&response_type=code" +
                "&access_type=offline";
    }

    public void handleCallback(String code) {
        Map<String, String> tokens = oauthService.exchangeCodeForTokens(code, redirectUri);
        String accessToken = tokens.get("accessToken");
        String refreshToken = tokens.get("refreshToken");

        Map<String, Object> userInfo = oauthService.getUserInfo(accessToken);

        findOrCreateUser(userInfo, accessToken, refreshToken);
    }

    private Users findOrCreateUser(Map<String, Object> userInfo, String accessToken, String refreshToken) {
        String googleId = (String) userInfo.get("id");
        String name = (String) userInfo.get("name");

        return userRepository.findByGoogleId(googleId)
                .orElseGet(() -> {
                    Users newUser = new Users(name, googleId, accessToken, refreshToken);
                    return userRepository.save(newUser);
                });
    }

    public String refreshAccessToken(String refreshToken) {
        return oauthService.refreshAccessToken(refreshToken);
    }

    public String refreshAccessTokenForUser(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String newAccessToken = refreshAccessToken(user.getRefreshToken());
        user.setAccessToken(newAccessToken);
        userRepository.save(user);
        return newAccessToken;
    }
}
