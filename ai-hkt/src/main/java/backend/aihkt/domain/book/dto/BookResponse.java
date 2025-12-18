package backend.aihkt.domain.book.dto;

public class BookResponse {

    public record BookInfo(
            Long id,
            String title,
            String author,
            Long userId
    ) {
    }

    public record BookWithUrls(
            Long id,
            String title,
            String author,
            Long userId,
            java.util.List<VideoInfo> videos
    ) {
    }

    public record VideoInfo(
            String videoId,
            String youtubeUrl
    ) {
    }
}
