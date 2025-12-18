package backend.aihkt.youtube.controller;

import backend.aihkt.youtube.dto.YoutubeUploadRequest;
import backend.aihkt.youtube.dto.YoutubeUploadResponse;
import backend.aihkt.youtube.service.YoutubeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/youtube")
public class YoutubeController {

    private final YoutubeService youtubeService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<YoutubeUploadResponse> upload(@RequestParam("userId") Long userId,
                                                        @RequestPart("metadata") YoutubeUploadRequest metadata,
                                                        @RequestPart("file") MultipartFile file) {
        YoutubeUploadResponse response = youtubeService.upload(userId, metadata, file);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
}
