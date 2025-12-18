package backend.aihkt.youtube.repository;

import backend.aihkt.youtube.entity.YoutubeUploadSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface YoutubeUploadSessionRepository extends JpaRepository<YoutubeUploadSession, String> {
}
