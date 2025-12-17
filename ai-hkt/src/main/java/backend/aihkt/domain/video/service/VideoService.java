package backend.aihkt.domain.video.service;

import backend.aihkt.domain.video.dto.VideoResponse;
import backend.aihkt.domain.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VideoService {
    private final VideoRepository videoRepository;

    public VideoResponse.Create createVideos(String userName, String title, String authorName) {

        return null;
    }

}
