package backend.aihkt.youtube.controller;

import backend.aihkt.youtube.dto.VideoAnalyticsResponse;
import backend.aihkt.youtube.dto.VideoStatResponse;
import backend.aihkt.youtube.dto.YoutubeUploadRequest;
import backend.aihkt.youtube.dto.YoutubeUploadResponse;
import backend.aihkt.youtube.service.YoutubeStatsService;
import backend.aihkt.youtube.service.YoutubeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/youtube")
public class YoutubeController {

    private final YoutubeService youtubeService;
    private final YoutubeStatsService youtubeStatsService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<YoutubeUploadResponse> upload(@RequestParam("userId") Long userId,
                                                        @RequestPart("metadata") YoutubeUploadRequest metadata,
                                                        @RequestPart("file") MultipartFile file) {
        YoutubeUploadResponse response = youtubeService.upload(userId, metadata, file);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<VideoStatResponse> getVideoStats(@RequestParam("userId") Long userId,
                                                           @RequestParam("videoId") String videoId) {
        return ResponseEntity.ok(youtubeStatsService.getVideoStats(userId, videoId));
    }

    @GetMapping("/analytics")
    public ResponseEntity<VideoAnalyticsResponse> getAnalytics(@RequestParam("userId") Long userId,
                                                               @RequestParam(value = "videoIds", required = false) List<String> videoIds,
                                                               @RequestParam(value = "startDate", required = false)
                                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                               @RequestParam(value = "endDate", required = false)
                                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(youtubeStatsService.getBulkAnalytics(userId, videoIds, startDate, endDate));
    }
}
