package backend.aihkt.domain.video.dto;

import backend.aihkt.domain.video.entity.VideoJobStatus;

public class VideoResponse {

    public record CreateJob(
            Long jobId,
            VideoJobStatus status
    ) {
    }

    public record JobStatus(
            Long jobId,
            VideoJobStatus status,
            String videoId,
            String youtubeUrl,
            String title,
            String authorName,
            String message
    ) {
    }
}
