package backend.aihkt.domain.video.dto;

import java.util.List;

public class VideoResponse {

    public record Create(
            String id,
            String youtubeUrl,
            String title
    ) {
    }
}
