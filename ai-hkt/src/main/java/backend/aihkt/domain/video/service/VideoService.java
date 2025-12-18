package backend.aihkt.domain.video.service;

import backend.aihkt.domain.user.entity.Users;
import backend.aihkt.domain.user.repository.UserRepository;
import backend.aihkt.domain.video.dto.VideoResponse;
import backend.aihkt.domain.video.entity.Video;
import backend.aihkt.domain.video.repository.VideoRepository;
import backend.aihkt.domain.book.entity.Book;
import backend.aihkt.domain.book.repository.BookRepository;
import backend.aihkt.youtube.dto.YoutubeUploadRequest;
import backend.aihkt.youtube.dto.YoutubeUploadResponse;
import backend.aihkt.youtube.service.YoutubeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {
    private final WebClient webClient;
    private final WebClient reactorWebClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector())
            .build();
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final BookRepository bookRepository;
    private final YoutubeService youtubeService;
    private final ObjectMapper objectMapper;

    @Value("${trailer.api.url}")
    private String trailerApiUrl;

    public VideoResponse.Create createVideos(Long userId, String title, String authorName) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        log.info("영상 생성 시작 - userId={}, title={}, author={}", userId, title, authorName);

        MultipartFile multipartFile = requestTrailer(title, authorName);
        log.info("트레일러 생성 완료 - sizeBytes={}", multipartFile == null ? 0 : multipartFile.getSize());

        YoutubeUploadRequest youtubeUploadForm = buildUploadRequest(title,authorName);
        YoutubeUploadResponse uploadResponse;
        try {
            uploadResponse = youtubeService.upload(
                    user.getId(),
                    youtubeUploadForm,
                    multipartFile
            );
        } finally {
            // 임시 파일 정리
            if (multipartFile instanceof PathMultipartFile pmf) {
                pmf.deleteQuietly();
            }
        }

        log.info("유튜브 업로드 완료 - status={}, videoId={}, youtubeUrl={}",
                uploadResponse.statusCode(), uploadResponse.videoId(), uploadResponse.youtubeUrl());

        String videoId = uploadResponse.videoId();
        String youtubeUrl = uploadResponse.youtubeUrl();
        Book book = upsertBook(user, title, authorName);

        if (videoId != null && youtubeUrl != null) {
            Video video = Video.create(
                    videoId,
                    youtubeUrl,
                    uploadResponse.resumableUploadUrl(),
                    book,
                    true
            );
            videoRepository.save(video);
            log.info("비디오 저장 완료 - videoId={}, bookId={}", videoId, book.getId());
        } else {
            log.warn("유튜브 업로드 결과에 videoId/youtubeUrl 없음 - 저장 생략");
        }

        VideoResponse.Create info = new VideoResponse.Create(videoId, youtubeUrl, youtubeUploadForm.title());
        log.info("응답 생성 완료 - videoId={}, youtubeUrl={}, videoTitle={}", videoId, youtubeUrl, youtubeUploadForm.title());
        return info;
    }

    private Book upsertBook(Users user, String title, String authorName) {
        String safeTitle = (title == null || title.isBlank()) ? "제목 없음" : title;
        String safeAuthor = (authorName == null || authorName.isBlank()) ? "저자 미상" : authorName;
        return bookRepository.findFirstByTitleAndAuthorAndUser(safeTitle, safeAuthor, user)
                .orElseGet(() -> bookRepository.save(Book.create(safeTitle, safeAuthor, user)));
    }

    private MultipartFile requestTrailer(String title, String authorName) {
        if (!StringUtils.hasText(trailerApiUrl)) {
            throw new IllegalStateException("trailer.api.url 설정이 없습니다.");
        }

        String filename = buildFileName(title);

        try {
            Path tempFile = Files.createTempFile("trailer_", ".mp4");
            log.info("[Trailer] request start - url={}, tempFile={}, filename={}", trailerApiUrl, tempFile, filename);

            Flux<DataBuffer> bodyFlux = reactorWebClient.post()
                    .uri(trailerApiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.valueOf("video/mp4"), MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("title", title, "author", authorName))
                    .retrieve()
                    .onStatus(
                            s -> s.value() == 422,
                            resp -> resp.bodyToMono(String.class)
                                    .defaultIfEmpty("422인데 응답 바디 없음")
                                    .map(body -> new IllegalArgumentException("FastAPI 422: " + body))
                    )
                    .bodyToFlux(DataBuffer.class);

            // 스트리밍으로 파일에 기록 (메모리 버퍼 제한 회피)
            Mono<Path> written = DataBufferUtils.write(bodyFlux, tempFile)
                    .timeout(Duration.ofMinutes(10))
                    .thenReturn(tempFile);

            Path writtenPath = written.block();
            if (writtenPath == null) {
                throw new IllegalStateException("트레일러 파일 저장 실패: 경로가 null입니다.");
            }

            long size = Files.size(writtenPath);
            log.info("[Trailer] saved - path={}, sizeBytes={}", writtenPath, size);

            return new PathMultipartFile("file", filename, "video/mp4", writtenPath);
        } catch (WebClientResponseException ex) {
            throw new IllegalStateException("트레일러 생성 API 실패: HTTP " + ex.getStatusCode().value(), ex);
        } catch (IOException e) {
            throw new IllegalStateException("트레일러 파일 저장/조회 중 I/O 오류", e);
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

    private static class PathMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final Path path;

        PathMultipartFile(String name, String originalFilename, String contentType, Path path) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.path = path;
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
            return getSize() == 0;
        }

        @Override
        public long getSize() {
            try {
                return Files.size(path);
            } catch (IOException e) {
                return 0;
            }
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(path);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Files.newInputStream(path);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.copy(path, dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        void deleteQuietly() {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
        }
    }
}
