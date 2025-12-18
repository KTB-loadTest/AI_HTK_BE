package backend.aihkt.domain.video.repository;

import backend.aihkt.domain.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VideoRepository extends JpaRepository<Video, String> {
    List<Video> findAllByBook_Id(Long bookId);
}
