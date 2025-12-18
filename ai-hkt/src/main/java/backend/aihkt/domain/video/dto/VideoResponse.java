package backend.aihkt.domain.video.dto;

import java.util.List;

public class VideoResponse {

    public record Create(
            List<VideoInfo> videoInfos
    ) {
    }

    public record VideoInfo(
            String id,
            String youtubeUrl
    ) {
    }
}
