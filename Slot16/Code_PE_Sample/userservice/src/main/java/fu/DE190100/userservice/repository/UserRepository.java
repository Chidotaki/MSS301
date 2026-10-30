package fu.DE190100.userservice.repository;

import fu.DE190100.userservice.entity.Role;
import fu.DE190100.userservice.entity.User;
import fu.DE190100.userservice.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    @Query("""
            SELECT u FROM User u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:status IS NULL OR u.status = :status)
            """)
    Page<User> search(
            @Param("role") Role role,
            @Param("status") UserStatus status,
            Pageable pageable
    );
}