package org.fractalx.admin.user;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Database-backed implementation of {@link UserStoreService}.
 * Active when the {@code db} Spring profile is enabled.
 *
 * <p>Seeds {@code admin / admin123} (ROLE_ADMIN) and {@code viewer / viewer123}
 * (ROLE_VIEWER) on first startup when the table is empty.
 *
 * <p>Activate: {@code -Dspring.profiles.active=db}
 * or set {@code SPRING_PROFILES_ACTIVE=db} in the environment.
 */
@Component
@Profile("db")
public class JpaUserStore implements UserStoreService {

    private final AdminUserRepository repo;
    private final PasswordEncoder     encoder = new BCryptPasswordEncoder();

    public JpaUserStore(AdminUserRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    @Transactional
    public void init() {
        if (repo.count() == 0) {
            create("admin",  "admin123",  Set.of("ROLE_ADMIN"));
            create("viewer", "viewer123", Set.of("ROLE_VIEWER"));
        }
    }

    @Override
    public Optional<AdminUser> findByUsername(String username) {
        return repo.findById(username);
    }

    @Override
    public List<AdminUser> findAll() {
        return repo.findAll();
    }

    @Override
    @Transactional
    public AdminUser create(String username, String rawPassword, Set<String> roles) {
        AdminUser user = new AdminUser(
                username,
                encoder.encode(rawPassword),
                new HashSet<>(roles),
                Instant.now().toString());
        return repo.save(user);
    }

    @Override
    @Transactional
    public boolean changePassword(String username, String rawPassword) {
        return repo.findById(username).map(u -> {
            u.setPasswordHash(encoder.encode(rawPassword));
            repo.save(u);
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional
    public boolean updateRoles(String username, Set<String> roles) {
        return repo.findById(username).map(u -> {
            u.setRoles(new HashSet<>(roles));
            repo.save(u);
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional
    public boolean setActive(String username, boolean active) {
        return repo.findById(username).map(u -> {
            u.setActive(active);
            repo.save(u);
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional
    public boolean delete(String username) {
        if (!repo.existsById(username)) return false;
        repo.deleteById(username);
        return true;
    }

    @Override
    @Transactional
    public void recordLogin(String username) {
        repo.findById(username).ifPresent(u -> {
            u.setLastLoginAt(Instant.now().toString());
            repo.save(u);
        });
    }

    @Override
    public int count() {
        return (int) repo.count();
    }
}
