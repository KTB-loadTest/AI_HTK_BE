package backend.aihkt.domain.book.controller;

import backend.aihkt.domain.book.dto.BookResponse;
import backend.aihkt.domain.book.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/books")
@Tag(name = "Book")
public class BookController {

    private final BookService bookService;

    @Operation(summary = "최근 생성된 책 4개 조회", description = "userId가 없으면 전체, 있으면 해당 사용자의 최신 4권을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = BookResponse.BookInfo.class))))
    @GetMapping("/recent")
    public ResponseEntity<List<BookResponse.BookInfo>> getRecentBooks(
            @RequestParam(value = "userId", required = false) Long userId) {
        return ResponseEntity.ok(bookService.getRecentBooks(userId));
    }

    @Operation(summary = "모든 책 조회", description = "userId가 없으면 전체, 있으면 해당 사용자의 모든 책을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = BookResponse.BookInfo.class))))
    @GetMapping
    public ResponseEntity<List<BookResponse.BookInfo>> getAllBooks(
            @RequestParam(value = "userId", required = false) Long userId) {
        return ResponseEntity.ok(bookService.getAllBooks(userId));
    }

    @Operation(summary = "책별 유튜브 URL 조회", description = "bookId에 매핑된 모든 유튜브 URL을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))
    @GetMapping("/{bookId}/youtube-urls")
    public ResponseEntity<List<String>> getYoutubeUrls(@PathVariable("bookId") Long bookId) {
        return ResponseEntity.ok(bookService.getYoutubeUrlsByBookId(bookId));
    }
}
