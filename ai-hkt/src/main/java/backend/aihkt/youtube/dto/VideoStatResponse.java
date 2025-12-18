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
        java.util.List<DailyMetric> dailyMetrics,
        SummaryAnalytics summaryAnalytics,
        java.util.List<CountryMetric> countryMetrics,
        java.util.List<TrafficMetric> trafficMetrics,
        java.util.List<DeviceMetric> deviceMetrics,
        java.util.List<OsMetric> osMetrics,
        java.util.List<AgeGenderMetric> ageGenderMetrics
) {

    public record DailyMetric(
            String date, // yyyy-MM-dd
            long views,
            long estimatedMinutesWatched,
            double averageViewDurationSeconds,
            double averageViewPercentage
    ) {
    }

    public record SummaryAnalytics(
            long impressions,
            double impressionsCtr,
            long views,
            double averageViewDurationSeconds,
            double averageViewPercentage,
            long estimatedMinutesWatched,
            long subscribersGained,
            long subscribersLost,
            long likes,
            long comments,
            long shares
    ) {
    }

    public record CountryMetric(
            String country,
            long views,
            long impressions,
            double impressionsCtr
    ) {
    }

    public record TrafficMetric(
            String trafficSourceType,
            long views,
            long impressions,
            double impressionsCtr
    ) {
    }

    public record DeviceMetric(
            String deviceType,
            long views,
            long impressions,
            double impressionsCtr
    ) {
    }

    public record OsMetric(
            String operatingSystem,
            long views,
            long impressions,
            double impressionsCtr
    ) {
    }

    public record AgeGenderMetric(
            String ageGroup,
            String gender,
            double viewerPercentage
    ) {
    }
}
