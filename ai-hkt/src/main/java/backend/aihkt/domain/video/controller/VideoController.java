package backend.aihkt.domain.video.controller;

import backend.aihkt.domain.video.dto.VideoRequest;
import backend.aihkt.domain.video.dto.VideoResponse;
import backend.aihkt.domain.video.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Video")
public class VideoController {
    private final VideoService videoService;

    @Operation(
            summary = "트레일러 생성 작업 시작",
            description = "책 제목/저자를 AI 트레일러 API에 전달해 MP4를 생성하고, 유튜브에 업로드하는 작업을 시작합니다. "
                    + "즉시 작업 ID를 반환하며, 상태는 /videos/jobs/{jobId} 로 폴링해 확인하세요."
    )
    @ApiResponse(responseCode = "202", description = "작업 접수",
            content = @Content(schema = @Schema(implementation = VideoResponse.CreateJob.class)))
    @PostMapping("/videos")
    public ResponseEntity<VideoResponse.CreateJob> createVideos(@RequestBody VideoRequest.Create request) {
        var result = videoService.createVideos(request.userId(), request.title(), request.authorName());

        return ResponseEntity.accepted().body(result);
    }

    @Operation(
            summary = "트레일러 생성 작업 상태 확인",
            description = "jobId 로 현재 상태를 조회합니다. status: PENDING/PROCESSING/SUCCESS/FAILED"
    )
    @ApiResponse(responseCode = "200", description = "상태 조회 성공",
            content = @Content(schema = @Schema(implementation = VideoResponse.JobStatus.class)))
    @GetMapping("/videos/jobs/{jobId}")
    public ResponseEntity<VideoResponse.JobStatus> getVideoJobStatus(@PathVariable Long jobId) {
        var result = videoService.getJobStatus(jobId);
        return ResponseEntity.ok(result);
    }
}
