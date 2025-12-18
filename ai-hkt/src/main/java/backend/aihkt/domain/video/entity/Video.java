package backend.aihkt.domain.video.entity;

import backend.aihkt.domain.book.entity.Book;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Video {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id; // YouTube videoId를 PK로 사용

    @Column(name = "youtube_url", nullable = false)
    private String youtubeUrl; // https://www.youtube.com/watch?v=<videoId>

    @Column(name = "resumable_upload_url", length = 2000)
    private String resumableUploadUrl; // 업로드 재개 세션 URL

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(name = "is_activated", nullable= false)
    private Boolean isActivated;

}
