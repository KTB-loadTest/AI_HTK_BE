package backend.aihkt.domain.user.repository;

import backend.aihkt.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Long> {

    Optional<Users> findByGoogleId(String googleId);

    Optional<Users> findByName(String name);
}
