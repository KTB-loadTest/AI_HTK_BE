package backend.aihkt.youtube.dto;

/**
 * 유튜브 업로드 결과 DTO.
 */
public record YoutubeUploadResponse(
        String videoId,
        String youtubeUrl,
        String resumableUploadUrl,
        int statusCode
) {
}
