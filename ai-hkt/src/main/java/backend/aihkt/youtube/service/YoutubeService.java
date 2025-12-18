package backend.aihkt.youtube.service;

import backend.aihkt.domain.user.entity.Users;
import backend.aihkt.domain.user.repository.UserRepository;
import backend.aihkt.infra.google.OAuthService;
import backend.aihkt.youtube.dto.YoutubeUploadRequest;
import backend.aihkt.youtube.dto.YoutubeUploadResponse;
import backend.aihkt.youtube.entity.YoutubeUploadSession;
import backend.aihkt.youtube.repository.YoutubeUploadSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class YoutubeService {

    private static final String RESUMABLE_ENDPOINT =
            "https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status,contentDetails";
    private static final String VIDEOS_ENDPOINT = "https://www.googleapis.com/youtube/v3/videos";
    private static final int CHUNK_SIZE = 256 * 1024; // 256KB 단위

    private final YoutubeUploadSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final OAuthService oAuthService;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public YoutubeUploadResponse upload(Long userId,
                                        YoutubeUploadRequest request,
                                        MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("업로드 파일이 비어 있습니다.");
            }

            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

            String accessToken = refreshAccessToken(user);

            InitiateResult initiation = initiateSession(accessToken, request, file);

            YoutubeUploadSession session = sessionRepository.save(
                    YoutubeUploadSession.create(
                            initiation.uploadUrl(),
                            file.getOriginalFilename(),
                            file.getSize(),
                            initiation.contentType(),
                            user
                    )
            );

            UploadResult uploadResult = uploadContent(accessToken, session.getUploadUrl(), session.getContentType(), file);

            sessionRepository.deleteById(session.getSessionId());

            String videoId = extractVideoId(uploadResult.responseBody());
            return new YoutubeUploadResponse(videoId, session.getUploadUrl(), uploadResult.statusCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("업로드 도중 인터럽트가 발생했습니다.", e);
        } catch (IOException e) {
            throw new IllegalStateException("유튜브 업로드 중 오류가 발생했습니다.", e);
        }
    }

    public void deleteVideo(Long userId, String videoId) {
        String accessToken = refreshAccessToken(userId);
        URI uri = URI.create(VIDEOS_ENDPOINT + "?id=" + videoId);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .DELETE()
                .build();
        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("영상 삭제 실패: HTTP " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("영상 삭제 중 오류", e);
        }
    }

    public void updatePrivacy(Long userId, String videoId, String privacyStatus) {
        String normalized = normalizePrivacy(privacyStatus);
        String accessToken = refreshAccessToken(userId);
        URI uri = URI.create(VIDEOS_ENDPOINT + "?part=status");

        ObjectNode root = objectMapper.createObjectNode();
        root.put("id", videoId);
        ObjectNode status = root.putObject("status");
        status.put("privacyStatus", normalized);

        try {
            String payload = objectMapper.writeValueAsString(root);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .PUT(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("영상 공개범위 변경 실패: HTTP " + response.statusCode() + " - " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("영상 공개범위 변경 중 오류", e);
        }
    }

    private String refreshAccessToken(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        return refreshAccessToken(user);
    }

    private String refreshAccessToken(Users user) {
        String refreshToken = user.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            String accessToken = user.getAccessToken();
            if (accessToken == null || accessToken.isBlank()) {
                throw new IllegalStateException("액세스 토큰이 없습니다. 다시 로그인해주세요.");
            }
            return accessToken;
        }

        String newAccessToken = oAuthService.refreshAccessToken(refreshToken);
        user.setAccessToken(newAccessToken);
        userRepository.save(user);
        return newAccessToken;
    }

    private InitiateResult initiateSession(String accessToken,
                                           YoutubeUploadRequest request,
                                           MultipartFile file) throws IOException, InterruptedException {
        String payload = buildMetadataPayload(request);
        String contentType = resolveContentType(file);

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(RESUMABLE_ENDPOINT))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("X-Upload-Content-Length", String.valueOf(file.getSize()))
                .header("X-Upload-Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<Void> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Resumable 세션 생성 실패: HTTP " + response.statusCode());
        }

        String uploadUrl = response.headers().firstValue("Location")
                .orElseThrow(() -> new IllegalStateException("Location 헤더가 없습니다."));

        return new InitiateResult(uploadUrl, contentType);
    }

    private UploadResult uploadContent(String accessToken,
                                       String uploadUrl,
                                       String contentType,
                                       MultipartFile file) throws IOException, InterruptedException {
        long fileSize = file.getSize();
        long offset = 0;
        String lastBody = null;
        int lastStatus = 0;

        try (InputStream input = file.getInputStream()) {
            while (offset < fileSize) {
                int bytesToRead = (int) Math.min(CHUNK_SIZE, fileSize - offset);
                byte[] buffer = input.readNBytes(bytesToRead);
                if (buffer.length == 0) {
                    break;
                }

                long start = offset;
                long end = offset + buffer.length - 1;

                HttpRequest request = HttpRequest.newBuilder(URI.create(uploadUrl))
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", contentType)
                        .header("Content-Range", "bytes %d-%d/%d".formatted(start, end, fileSize))
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(buffer))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                lastStatus = response.statusCode();
                lastBody = response.body();

                if (lastStatus == 308) {
                    offset = nextOffset(response.headers().firstValue("Range"), end + 1);
                    continue;
                }

                if (lastStatus == 200 || lastStatus == 201) {
                    break;
                }

                throw new IllegalStateException("업로드 실패 (HTTP %d): %s".formatted(lastStatus, response.body()));
            }
        }

        return new UploadResult(lastStatus, lastBody);
    }

    private long nextOffset(Optional<String> rangeHeader, long defaultOffset) {
        return rangeHeader.map(header -> parseRangeHeader(header, defaultOffset)).orElse(defaultOffset);
    }

    private long parseRangeHeader(String rangeHeader, long defaultOffset) {
        String[] parts = rangeHeader.split("=");
        if (parts.length != 2) {
            return defaultOffset;
        }
        String[] values = parts[1].split("-");
        if (values.length != 2) {
            return defaultOffset;
        }
        try {
            long lastByte = Long.parseLong(values[1]);
            return lastByte + 1;
        } catch (NumberFormatException ex) {
            return defaultOffset;
        }
    }

    private String buildMetadataPayload(YoutubeUploadRequest request) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode snippet = root.putObject("snippet");
        snippet.put("title", request.title() == null || request.title().isBlank() ? "Untitled upload" : request.title());
        if (request.description() != null) {
            snippet.put("description", request.description());
        }
        if (request.tags() != null && !request.tags().isEmpty()) {
            ArrayNode tags = snippet.putArray("tags");
            request.tags().forEach(tags::add);
        }
        if (request.categoryId() != null) {
            snippet.put("categoryId", request.categoryId());
        }

        ObjectNode status = root.putObject("status");
        status.put("privacyStatus", request.privacyStatus() == null ? "private" : request.privacyStatus());
        if (request.embeddable() != null) {
            status.put("embeddable", request.embeddable());
        }
        if (request.license() != null) {
            status.put("license", request.license());
        }

        return objectMapper.writeValueAsString(root);
    }

    private String extractVideoId(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(body).path("id").asText(null);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
    }

    private String normalizePrivacy(String privacy) {
        if (privacy == null) {
            return "private";
        }
        String p = privacy.toLowerCase();
        if (p.equals("public") || p.equals("private") || p.equals("unlisted")) {
            return p;
        }
        throw new IllegalArgumentException("privacyStatus는 public/private/unlisted 중 하나여야 합니다.");
    }

    private record InitiateResult(String uploadUrl, String contentType) {
    }

    private record UploadResult(int statusCode, String responseBody) {
    }
}
