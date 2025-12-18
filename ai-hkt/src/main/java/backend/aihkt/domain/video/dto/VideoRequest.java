package backend.aihkt.domain.video.dto;

public class VideoRequest {

    public record Create(
            Long userId,
            String title,
            String authorName
    ) {
    }
}
