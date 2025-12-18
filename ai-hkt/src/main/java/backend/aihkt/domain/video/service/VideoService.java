package backend.aihkt.domain.video.service;

import backend.aihkt.domain.user.entity.Users;
import backend.aihkt.domain.user.repository.UserRepository;
import backend.aihkt.domain.video.dto.VideoResponse;
import backend.aihkt.youtube.dto.YoutubeUploadRequest;
import backend.aihkt.youtube.dto.YoutubeUploadResponse;
import backend.aihkt.youtube.service.YoutubeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoService {
    private final WebClient webClient = WebClient.create();
    private final UserRepository userRepository;
    private final YoutubeService youtubeService;

    @Value("${trailer.api.url}")
    private String trailerApiUrl;

    public VideoResponse.Create createVideos(String userName, String title, String authorName) {
        Users user = userRepository.findByName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userName));

        byte[] videoBytes = requestTrailer(title, authorName);
        MultipartFile multipartFile = toMultipartFile(videoBytes, buildFileName(title));

        YoutubeUploadResponse uploadResponse = youtubeService.upload(
                user.getId(),
                buildUploadRequest(title, authorName),
                multipartFile
        );

        String youtubeUrl = uploadResponse.videoId() == null
                ? null
                : "https://www.youtube.com/watch?v=" + uploadResponse.videoId();

        VideoResponse.VideoInfo info = new VideoResponse.VideoInfo(null, youtubeUrl);
        return new VideoResponse.Create(Collections.singletonList(info));
    }

    private byte[] requestTrailer(String title, String authorName) {
        if (!StringUtils.hasText(trailerApiUrl)) {
            throw new IllegalStateException("trailer.api.url 설정이 없습니다.");
        }

        try {
            return webClient.post()
                    .uri(UriComponentsBuilder.fromUriString(trailerApiUrl).build(true).toUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.valueOf("video/mp4"))
                    .body(BodyInserters.fromValue(
                            java.util.Map.of(
                                    "title", title,
                                    "author", authorName
                            )))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block(Duration.ofMinutes(10)); // AI 생성이 오래 걸리므로 타임아웃 넉넉히
        } catch (WebClientResponseException ex) {
            throw new IllegalStateException("트레일러 생성 API 실패: HTTP " + ex.getStatusCode().value(), ex);
        }
    }

    private MultipartFile toMultipartFile(byte[] bytes, String filename) {
        return new SimpleMultipartFile("file", filename, "video/mp4", bytes);
    }

    private String buildFileName(String title) {
        String base = StringUtils.hasText(title) ? title : "trailer";
        String cleaned = base.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        cleaned = StringUtils.hasText(cleaned) ? cleaned : "trailer";
        return cleaned + "_trailer.mp4";
    }

    private YoutubeUploadRequest buildUploadRequest(String title, String authorName) {
        String finalTitle = (title == null || title.isBlank()) ? "트레일러" : title + " 트레일러";
        String description = "저자: " + (authorName == null ? "" : authorName);
        return new YoutubeUploadRequest(
                finalTitle,
                description,
                List.of(title, authorName),
                null,
                "private",
                true,
                null
        );
    }

    private static class SimpleMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] bytes;

        SimpleMultipartFile(String name, String originalFilename, String contentType, byte[] bytes) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.bytes = bytes == null ? new byte[0] : bytes;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public java.io.InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(java.io.File dest) throws java.io.IOException {
            java.nio.file.Files.write(dest.toPath(), bytes);
        }
    }
}
