package backend.aihkt.domain.book.dto;

public class BookResponse {

    public record BookInfo(
            Long id,
            String title,
            String author,
            Long userId
    ) {
    }
}
