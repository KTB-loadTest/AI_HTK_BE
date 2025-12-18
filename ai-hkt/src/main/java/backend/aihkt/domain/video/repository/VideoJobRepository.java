package backend.aihkt.domain.video.repository;

import backend.aihkt.domain.video.entity.VideoJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoJobRepository extends JpaRepository<VideoJob, Long> {
}
