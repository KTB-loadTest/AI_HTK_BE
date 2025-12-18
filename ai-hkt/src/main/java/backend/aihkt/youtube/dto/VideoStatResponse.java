package backend.aihkt.youtube.dto;

/**
 * Data API 단건 조회 응답 DTO.
 */
public record VideoStatResponse(
        String videoId,
        long viewCount,
        long likeCount,
        long commentCount,
        long favoriteCount,
        long durationSeconds,
        java.util.List<DailyMetric> dailyMetrics
) {

    public record DailyMetric(
            String date, // yyyy-MM-dd
            long views,
            long estimatedMinutesWatched,
            double averageViewDurationSeconds
    ) {
    }
}
