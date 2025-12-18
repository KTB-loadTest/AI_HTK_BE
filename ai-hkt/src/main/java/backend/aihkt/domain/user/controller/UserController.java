package backend.aihkt.domain.user.controller;

import backend.aihkt.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "User/OAuth")
public class UserController {
    @Value("${cors.allowed.origin}")
    private String front_url;

    private final UserService userService;

    @Operation(summary = "구글 OAuth 로그인 리다이렉트", description = "구글 동의 화면으로 리다이렉트 URL을 반환합니다.")
    @GetMapping("/login")
    public RedirectView login() {
        String authUrl = userService.getLoginUrl();
        return new RedirectView(authUrl);
    }

    @Operation(summary = "구글 OAuth 콜백", description = "code를 받아 토큰/사용자 정보를 저장하고 프론트로 리다이렉트합니다.")
    @GetMapping("/callback")
    public RedirectView callback(@RequestParam String code) {
        userService.handleCallback(code);
        return new RedirectView(front_url + "/?login=success");
    }

    @Operation(summary = "리프레시 토큰으로 액세스 토큰 갱신",
            description = "DB에 저장된 refresh token으로 새 access token을 발급합니다.",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ))
    @PostMapping("/refresh-token")
    public Map<String, String> refreshToken(@RequestParam Long userId) {
        String newAccessToken = userService.refreshAccessTokenForUser(userId);
        return Map.of("accessToken", newAccessToken);
    }
}
