package backend.aihkt.domain.video.controller;

import backend.aihkt.domain.video.dto.VideoRequest;
import backend.aihkt.domain.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VideoController {
    private final VideoService videoService;

    @PostMapping("/videos")
    public ResponseEntity<?> createVideos(@RequestBody VideoRequest.Create request) {
        var result = videoService.createVideos(request.userName(), request.title(), request.authorName());

        return ResponseEntity.ok();
    }
}
