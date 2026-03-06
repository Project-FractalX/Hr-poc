package org.fractalx.admin.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link AdminUser}.
 * Active only when the {@code db} Spring profile is enabled.
 */
@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, String> {
}
