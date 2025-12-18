package backend.aihkt.domain.video.entity;

import backend.aihkt.domain.user.entity.Users;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class VideoJob {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "title")
    private String title;

    @Column(name = "author_name")
    private String authorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VideoJobStatus status;

    @Column(name = "video_id")
    private String videoId;

    @Column(name = "youtube_url")
    private String youtubeUrl;

    @Column(name = "message", length = 1000)
    private String message;

    public static VideoJob pending(Users user, String title, String authorName) {
        VideoJob job = new VideoJob();
        job.user = user;
        job.title = title;
        job.authorName = authorName;
        job.status = VideoJobStatus.PENDING;
        return job;
    }

    public void markProcessing() {
        this.status = VideoJobStatus.PROCESSING;
        this.message = null;
    }

    public void markSuccess(String videoId, String youtubeUrl) {
        this.status = VideoJobStatus.SUCCESS;
        this.videoId = videoId;
        this.youtubeUrl = youtubeUrl;
        this.message = null;
    }

    public void markFailure(String message) {
        this.status = VideoJobStatus.FAILED;
        this.message = truncate(message);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
