package backend.aihkt.youtube.controller;

import backend.aihkt.youtube.dto.VideoAnalyticsResponse;
import backend.aihkt.youtube.dto.VideoStatResponse;
import backend.aihkt.youtube.dto.YoutubeUploadRequest;
import backend.aihkt.youtube.dto.YoutubeUploadResponse;
import backend.aihkt.youtube.service.YoutubeStatsService;
import backend.aihkt.youtube.service.YoutubeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/youtube")
@Tag(name = "YouTube")
public class YoutubeController {

    private final YoutubeService youtubeService;
    private final YoutubeStatsService youtubeStatsService;

    @Operation(
            summary = "유튜브 업로드 (Resumable)",
            description = "multipart(form-data)로 메타데이터(JSON)와 동영상 파일을 받아 유튜브에 업로드합니다."
    )
    @ApiResponse(responseCode = "200", description = "업로드 성공",
            content = @Content(schema = @Schema(implementation = YoutubeUploadResponse.class)))
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<YoutubeUploadResponse> upload(@RequestParam("userId") Long userId,
                                                        @RequestPart("metadata") YoutubeUploadRequest metadata,
                                                        @RequestPart("file") MultipartFile file) {
        YoutubeUploadResponse response = youtubeService.upload(userId, metadata, file);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @Operation(summary = "유튜브 영상 삭제")
    @ApiResponse(responseCode = "204", description = "삭제 완료")
    @DeleteMapping("/videos")
    public ResponseEntity<Void> deleteVideo(@RequestParam("userId") Long userId,
                                            @RequestParam("videoId") String videoId) {
        youtubeService.deleteVideo(userId, videoId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "유튜브 영상 공개 범위 변경", description = "privacyStatus: public | private | unlisted")
    @ApiResponse(responseCode = "200", description = "변경 완료")
    @PatchMapping("/videos/privacy")
    public ResponseEntity<Void> updatePrivacy(@RequestParam("userId") Long userId,
                                              @RequestParam("videoId") String videoId,
                                              @RequestParam("privacyStatus") String privacyStatus) {
        youtubeService.updatePrivacy(userId, videoId, privacyStatus);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "영상 단건 통계 조회", description = "views/likes/comments/favorites 및 재생시간(초)")
    @ApiResponse(responseCode = "200", description = "통계 조회 성공",
            content = @Content(schema = @Schema(implementation = VideoStatResponse.class)))
    @GetMapping("/stats")
    public ResponseEntity<VideoStatResponse> getVideoStats(@RequestParam("userId") Long userId,
                                                           @RequestParam("videoId") String videoId) {
        return ResponseEntity.ok(youtubeStatsService.getVideoStats(userId, videoId));
    }

    @Operation(summary = "영상 집계 통계 조회", description = "YouTube Analytics API로 기간/영상 목록에 대한 집계 조회수/시청지속시간/평균재생시간을 반환")
    @ApiResponse(responseCode = "200", description = "집계 조회 성공",
            content = @Content(schema = @Schema(implementation = VideoAnalyticsResponse.class)))
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
