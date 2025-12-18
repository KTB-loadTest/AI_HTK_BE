package backend.aihkt.domain.book.repository;

import backend.aihkt.domain.book.entity.Book;
import backend.aihkt.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findFirstByTitleAndAuthorAndUser(String title, String author, Users user);
    List<Book> findTop4ByOrderByIdDesc();
    List<Book> findTop4ByUserOrderByIdDesc(Users user);
    List<Book> findAllByUserOrderByIdDesc(Users user);
}
