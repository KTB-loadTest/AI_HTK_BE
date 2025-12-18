package backend.aihkt.youtube.service;

import backend.aihkt.domain.user.entity.Users;
import backend.aihkt.domain.user.repository.UserRepository;
import backend.aihkt.infra.google.OAuthService;
import backend.aihkt.youtube.dto.VideoAnalyticsResponse;
import backend.aihkt.youtube.dto.VideoStatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class YoutubeStatsService {

    private static final String DATA_API_VIDEOS =
            "https://www.googleapis.com/youtube/v3/videos?part=statistics,contentDetails&id=%s";

    private static final String ANALYTICS_API_REPORTS =
            "https://youtubeanalytics.googleapis.com/v2/reports";

    private final UserRepository userRepository;
    private final OAuthService oAuthService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public VideoStatResponse getVideoStats(Long userId, String videoId) {
        String accessToken = refreshAccessToken(userId);
        HttpRequest request = HttpRequest.newBuilder(URI.create(DATA_API_VIDEOS.formatted(videoId)))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("YouTube Data API 실패: HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode item = root.path("items").path(0);
            JsonNode statistics = item.path("statistics");
            JsonNode contentDetails = item.path("contentDetails");
            long durationSeconds = parseDurationSeconds(contentDetails.path("duration").asText(""));
            List<VideoStatResponse.DailyMetric> dailyMetrics = fetchDailyMetrics(accessToken, videoId);
            VideoStatResponse.SummaryAnalytics summaryAnalytics = fetchSummaryAnalytics(accessToken, videoId);
            List<VideoStatResponse.CountryMetric> countryMetrics = fetchCountryMetrics(accessToken, videoId);
            List<VideoStatResponse.TrafficMetric> trafficMetrics = fetchTrafficMetrics(accessToken, videoId);
            List<VideoStatResponse.DeviceMetric> deviceMetrics = fetchDeviceMetrics(accessToken, videoId);
            List<VideoStatResponse.OsMetric> osMetrics = fetchOsMetrics(accessToken, videoId);
            List<VideoStatResponse.AgeGenderMetric> ageGenderMetrics = fetchAgeGenderMetrics(accessToken, videoId);
            return new VideoStatResponse(
                    videoId,
                    statistics.path("viewCount").asLong(0),
                    statistics.path("likeCount").asLong(0),
                    statistics.path("commentCount").asLong(0),
                    statistics.path("favoriteCount").asLong(0),
                    durationSeconds,
                    dailyMetrics,
                    summaryAnalytics,
                    countryMetrics,
                    trafficMetrics,
                    deviceMetrics,
                    osMetrics,
                    ageGenderMetrics
            );
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("YouTube Data API 호출 실패", e);
        }
    }

    public VideoAnalyticsResponse getBulkAnalytics(Long userId,
                                                   List<String> videoIds,
                                                   LocalDate startDate,
                                                   LocalDate endDate) {
        String accessToken = refreshAccessToken(userId);

        String idsFilter = videoIds == null || videoIds.isEmpty()
                ? null
                : videoIds.stream().collect(Collectors.joining(","));

        URI uri = URI.create(buildAnalyticsUri(idsFilter, startDate, endDate));

        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("YouTube Analytics API 실패: HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            List<VideoAnalyticsResponse.Row> rows = new ArrayList<>();
            for (JsonNode row : root.path("rows")) {
                String videoId = row.path(0).asText();
                long views = row.path(1).asLong(0);
                long minutes = row.path(2).asLong(0);
                double avgDurationSec = row.path(3).asDouble(0);
                rows.add(new VideoAnalyticsResponse.Row(videoId, views, minutes, avgDurationSec));
            }
            return new VideoAnalyticsResponse(rows);
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("YouTube Analytics API 호출 실패", e);
        }
    }

    private String buildAnalyticsUri(String videoIds, LocalDate startDate, LocalDate endDate) {
        LocalDate start = Optional.ofNullable(startDate).orElse(LocalDate.now().minusDays(7));
        LocalDate end = Optional.ofNullable(endDate).orElse(LocalDate.now());

        StringBuilder sb = new StringBuilder(ANALYTICS_API_REPORTS)
                .append("?ids=channel==MINE")
                .append("&startDate=").append(start)
                .append("&endDate=").append(end)
                .append("&metrics=views,estimatedMinutesWatched,averageViewDuration")
                .append("&dimensions=video");

        if (StringUtils.hasText(videoIds)) {
            sb.append("&filters=video==").append(videoIds);
        }
        return sb.toString();
    }

    private List<VideoStatResponse.DailyMetric> fetchDailyMetrics(String accessToken, String videoId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6); // 최근 7일
        String uri = analyticsUri("day",
                "views,estimatedMinutesWatched,averageViewDuration,averageViewPercentage",
                "video==" + videoId,
                start, end);
        JsonNode root = executeAnalytics(accessToken, uri);
        if (root == null) return List.of();
        List<VideoStatResponse.DailyMetric> rows = new ArrayList<>();
        for (JsonNode row : root.path("rows")) {
            String date = row.path(0).asText();
            long views = row.path(1).asLong(0);
            long minutes = row.path(2).asLong(0);
            double avgDurationSec = row.path(3).asDouble(0);
            double avgPercentage = row.path(4).asDouble(0);
            rows.add(new VideoStatResponse.DailyMetric(date, views, minutes, avgDurationSec, avgPercentage));
        }
        return rows;
    }

    private VideoStatResponse.SummaryAnalytics fetchSummaryAnalytics(String accessToken, String videoId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        String metrics = "impressions,impressionsCtr,views,averageViewDuration,averageViewPercentage,"
                + "estimatedMinutesWatched,subscribersGained,subscribersLost,likes,comments,shares";
        String uri = analyticsUri("video", metrics, "video==" + videoId, start, end);
        JsonNode root = executeAnalytics(accessToken, uri);
        if (root == null) {
            return new VideoStatResponse.SummaryAnalytics(0,0,0,0,0,0,0,0,0,0,0);
        }
        JsonNode row = root.path("rows").path(0);
        return new VideoStatResponse.SummaryAnalytics(
                row.path(1).asLong(0),   // impressions
                row.path(2).asDouble(0), // impressionsCtr
                row.path(3).asLong(0),   // views
                row.path(4).asDouble(0), // avg view duration
                row.path(5).asDouble(0), // avg view percentage
                row.path(6).asLong(0),   // est minutes
                row.path(7).asLong(0),   // subs gained
                row.path(8).asLong(0),   // subs lost
                row.path(9).asLong(0),   // likes
                row.path(10).asLong(0),  // comments
                row.path(11).asLong(0)   // shares
        );
    }

    private List<VideoStatResponse.CountryMetric> fetchCountryMetrics(String accessToken, String videoId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        String uri = analyticsUri("country", "views,impressions,impressionsCtr",
                "video==" + videoId, start, end);
        JsonNode root = executeAnalytics(accessToken, uri);
        if (root == null) return List.of();
        List<VideoStatResponse.CountryMetric> rows = new ArrayList<>();
        for (JsonNode r : root.path("rows")) {
            rows.add(new VideoStatResponse.CountryMetric(
                    r.path(0).asText(),
                    r.path(1).asLong(0),
                    r.path(2).asLong(0),
                    r.path(3).asDouble(0)
            ));
        }
        return rows;
    }

    private List<VideoStatResponse.TrafficMetric> fetchTrafficMetrics(String accessToken, String videoId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        String uri = analyticsUri("trafficSourceType", "views,impressions,impressionsCtr",
                "video==" + videoId, start, end);
        JsonNode root = executeAnalytics(accessToken, uri);
        if (root == null) return List.of();
        List<VideoStatResponse.TrafficMetric> rows = new ArrayList<>();
        for (JsonNode r : root.path("rows")) {
            rows.add(new VideoStatResponse.TrafficMetric(
                    r.path(0).asText(),
                    r.path(1).asLong(0),
                    r.path(2).asLong(0),
                    r.path(3).asDouble(0)
            ));
        }
        return rows;
    }

    private List<VideoStatResponse.DeviceMetric> fetchDeviceMetrics(String accessToken, String videoId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        String uri = analyticsUri("deviceType", "views,impressions,impressionsCtr",
                "video==" + videoId, start, end);
        JsonNode root = executeAnalytics(accessToken, uri);
        if (root == null) return List.of();
        List<VideoStatResponse.DeviceMetric> rows = new ArrayList<>();
        for (JsonNode r : root.path("rows")) {
            rows.add(new VideoStatResponse.DeviceMetric(
                    r.path(0).asText(),
                    r.path(1).asLong(0),
                    r.path(2).asLong(0),
                    r.path(3).asDouble(0)
            ));
        }
        return rows;
    }

    private List<VideoStatResponse.OsMetric> fetchOsMetrics(String accessToken, String videoId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        String uri = analyticsUri("operatingSystem", "views,impressions,impressionsCtr",
                "video==" + videoId, start, end);
        JsonNode root = executeAnalytics(accessToken, uri);
        if (root == null) return List.of();
        List<VideoStatResponse.OsMetric> rows = new ArrayList<>();
        for (JsonNode r : root.path("rows")) {
            rows.add(new VideoStatResponse.OsMetric(
                    r.path(0).asText(),
                    r.path(1).asLong(0),
                    r.path(2).asLong(0),
                    r.path(3).asDouble(0)
            ));
        }
        return rows;
    }

    private List<VideoStatResponse.AgeGenderMetric> fetchAgeGenderMetrics(String accessToken, String videoId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        String uri = analyticsUri("ageGroup,gender", "viewerPercentage",
                "video==" + videoId, start, end);
        JsonNode root = executeAnalytics(accessToken, uri);
        if (root == null) return List.of();
        List<VideoStatResponse.AgeGenderMetric> rows = new ArrayList<>();
        for (JsonNode r : root.path("rows")) {
            rows.add(new VideoStatResponse.AgeGenderMetric(
                    r.path(0).asText(),
                    r.path(1).asText(),
                    r.path(2).asDouble(0)
            ));
        }
        return rows;
    }

    private String analyticsUri(String dimensions, String metrics, String filters,
                                LocalDate start, LocalDate end) {
        StringBuilder sb = new StringBuilder(ANALYTICS_API_REPORTS)
                .append("?ids=channel==MINE")
                .append("&startDate=").append(start)
                .append("&endDate=").append(end)
                .append("&metrics=").append(metrics)
                .append("&dimensions=").append(dimensions);
        if (StringUtils.hasText(filters)) {
            sb.append("&filters=").append(filters);
        }
        return sb.toString();
    }

    private JsonNode executeAnalytics(String accessToken, String uri) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return null;
            }
            return objectMapper.readTree(response.body());
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }

    private long parseDurationSeconds(String iso8601) {
        if (!StringUtils.hasText(iso8601)) {
            return 0;
        }
        // Simple ISO8601 duration parser for PT#H#M#S
        long hours = 0, minutes = 0, seconds = 0;
        String t = iso8601.replace("PT", "");
        String[] hSplit = t.split("H");
        if (hSplit.length == 2) {
            hours = parseNumber(hSplit[0]);
            t = hSplit[1];
        }
        String[] mSplit = t.split("M");
        if (mSplit.length == 2) {
            minutes = parseNumber(mSplit[0]);
            t = mSplit[1];
        }
        String[] sSplit = t.split("S");
        if (sSplit.length >= 1 && StringUtils.hasText(sSplit[0])) {
            seconds = parseNumber(sSplit[0]);
        }
        return hours * 3600 + minutes * 60 + seconds;
    }

    private long parseNumber(String num) {
        try {
            return Long.parseLong(num);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String refreshAccessToken(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        String refreshToken = user.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            if (user.getAccessToken() == null || user.getAccessToken().isBlank()) {
                throw new IllegalStateException("액세스 토큰이 없습니다. 다시 로그인해주세요.");
            }
            return user.getAccessToken();
        }
        String newAccess = oAuthService.refreshAccessToken(refreshToken);
        user.setAccessToken(newAccess);
        userRepository.save(user);
        return newAccess;
    }
}
