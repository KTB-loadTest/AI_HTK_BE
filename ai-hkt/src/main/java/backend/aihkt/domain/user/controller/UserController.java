package backend.aihkt.domain.user.controller;

import backend.aihkt.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/login")
    public RedirectView login() {
        String authUrl = userService.getLoginUrl();
        return new RedirectView(authUrl);
    }

    @GetMapping("/callback")
    public RedirectView callback(@RequestParam String code) {
        userService.handleCallback(code);
        return new RedirectView("hkt.anyword-bigfestival.cloud/?login=success");
    }

    @PostMapping("/refresh-token")
    public Map<String, String> refreshToken(@RequestParam Long userId) {
        String newAccessToken = userService.refreshAccessTokenForUser(userId);
        return Map.of("accessToken", newAccessToken);
    }
}
