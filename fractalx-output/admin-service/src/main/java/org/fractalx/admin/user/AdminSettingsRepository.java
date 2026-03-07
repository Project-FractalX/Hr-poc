package org.fractalx.admin.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the singleton {@link AdminSettings} row (id=1).
 * Active only when the {@code db} Spring profile is enabled.
 */
@Repository
public interface AdminSettingsRepository extends JpaRepository<AdminSettings, Integer> {
}
