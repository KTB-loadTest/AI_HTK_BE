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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Video")
public class VideoController {
    private final VideoService videoService;

    @Operation(
            summary = "트레일러 생성 후 유튜브 업로드",
            description = "책 제목/저자를 AI 트레일러 API에 전달해 MP4를 생성하고, 유튜브에 업로드 후 URL을 반환합니다. "
                    + "처리 시간이 길어질 수 있습니다."
    )
    @ApiResponse(responseCode = "200", description = "업로드 성공",
            content = @Content(schema = @Schema(implementation = VideoResponse.Create.class)))
    @PostMapping("/videos")
    public ResponseEntity<VideoResponse.Create> createVideos(@RequestBody VideoRequest.Create request) {
        var result = videoService.createVideos(request.userName(), request.title(), request.authorName());

        return ResponseEntity.ok(result);
    }
}
