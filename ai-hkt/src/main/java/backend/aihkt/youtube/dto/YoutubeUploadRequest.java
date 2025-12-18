package backend.aihkt.youtube.dto;

import java.util.List;

/**
 * 유튜브 업로드 시 메타데이터를 전달하는 DTO.
 */
public record YoutubeUploadRequest(
        String title,
        String description,
        List<String> tags,
        Integer categoryId,
        String privacyStatus,
        Boolean embeddable,
        String license
) {
}
