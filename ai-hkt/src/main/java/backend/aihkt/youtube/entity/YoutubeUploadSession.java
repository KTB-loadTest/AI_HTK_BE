package backend.aihkt.youtube.entity;

import backend.aihkt.domain.user.entity.Users;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "youtube_upload_session")
public class YoutubeUploadSession {

    @Id
    @Column(name = "session_id", nullable = false, updatable = false, length = 64)
    private String sessionId;

    @Column(name = "upload_url", nullable = false, length = 2000)
    private String uploadUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_length")
    private long contentLength;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    protected YoutubeUploadSession() {
        // JPA 기본 생성자
    }

    private YoutubeUploadSession(String sessionId,
                                 String uploadUrl,
                                 String fileName,
                                 long contentLength,
                                 String contentType,
                                 Users user,
                                 Instant createdAt) {
        this.sessionId = sessionId;
        this.uploadUrl = uploadUrl;
        this.fileName = fileName;
        this.contentLength = contentLength;
        this.contentType = contentType;
        this.user = user;
        this.createdAt = createdAt;
    }

    public static YoutubeUploadSession create(String uploadUrl,
                                              String fileName,
                                              long contentLength,
                                              String contentType,
                                              Users user) {
        return new YoutubeUploadSession(
                UUID.randomUUID().toString(),
                uploadUrl,
                fileName,
                contentLength,
                contentType,
                user,
                Instant.now()
        );
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Users getUser() {
        return user;
    }
}
