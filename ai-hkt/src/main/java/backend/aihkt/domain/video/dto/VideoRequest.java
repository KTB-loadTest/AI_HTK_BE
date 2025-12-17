package backend.aihkt.domain.video.dto;

public class VideoRequest {

    public record Create(
            String userName,
            String title,
            String authorName
    ) {
    }
}
