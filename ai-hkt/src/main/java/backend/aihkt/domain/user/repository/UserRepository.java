package backend.aihkt.domain.user.repository;

import backend.aihkt.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Users, Long> {
}
