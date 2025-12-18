package backend.aihkt.domain.book.service;

import backend.aihkt.domain.book.dto.BookResponse;
import backend.aihkt.domain.book.entity.Book;
import backend.aihkt.domain.book.repository.BookRepository;
import backend.aihkt.domain.user.entity.Users;
import backend.aihkt.domain.user.repository.UserRepository;
import backend.aihkt.domain.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;

    public List<BookResponse.BookInfo> getRecentBooks(Long userId) {
        List<Book> books = (userId == null)
                ? bookRepository.findTop4ByOrderByIdDesc()
                : bookRepository.findTop4ByUserOrderByIdDesc(getUser(userId));
        return toBookInfoList(books);
    }

    public List<BookResponse.BookInfo> getAllBooks(Long userId) {
        List<Book> books = (userId == null)
                ? bookRepository.findAll()
                : bookRepository.findAllByUserOrderByIdDesc(getUser(userId));
        return toBookInfoList(books);
    }

    public BookResponse.BookWithUrls getYoutubeUrlsByBookId(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("책을 찾을 수 없습니다: " + bookId));

        List<BookResponse.VideoInfo> videos = videoRepository.findAllByBook_Id(bookId).stream()
                .map(v -> new BookResponse.VideoInfo(v.getId(), v.getYoutubeUrl()))
                .collect(Collectors.toList());

        return new BookResponse.BookWithUrls(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getUser() == null ? null : book.getUser().getId(),
                videos
        );
    }

    private Users getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    private List<BookResponse.BookInfo> toBookInfoList(List<Book> books) {
        return books.stream()
                .map(b -> new BookResponse.BookInfo(
                        b.getId(),
                        b.getTitle(),
                        b.getAuthor(),
                        b.getUser() == null ? null : b.getUser().getId()))
                .collect(Collectors.toList());
    }
}
