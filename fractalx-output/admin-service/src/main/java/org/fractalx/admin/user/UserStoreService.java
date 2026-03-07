package org.fractalx.admin.user;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Abstraction over admin user storage.
 * <p>Two implementations:
 * <ul>
 *   <li>{@link UserStore}    — in-memory (default, no datasource required)</li>
 *   <li>{@code JpaUserStore} — Spring Data JPA, active with {@code @Profile("db")}</li>
 * </ul>
 */
public interface UserStoreService {

    Optional<AdminUser> findByUsername(String username);
    List<AdminUser>     findAll();
    AdminUser           create(String username, String rawPassword, Set<String> roles);
    boolean             changePassword(String username, String rawPassword);
    boolean             updateRoles(String username, Set<String> roles);
    boolean             setActive(String username, boolean active);
    boolean             delete(String username);
    void                recordLogin(String username);
    int                 count();
}
