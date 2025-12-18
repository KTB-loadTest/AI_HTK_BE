package backend.aihkt.youtube.dto;

import java.util.List;

/**
 * Analytics API 집계 응답 DTO.
 */
public record VideoAnalyticsResponse(
        List<Row> rows
) {
    public record Row(
            String videoId,
            long views,
            long estimatedMinutesWatched,
            double averageViewDurationSeconds
    ) {
    }
}
