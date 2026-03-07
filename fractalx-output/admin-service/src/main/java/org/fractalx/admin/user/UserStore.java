package org.fractalx.admin.user;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory user store. Active when the {@code db} profile is NOT active.
 *
 * <p>Pre-seeded at startup with:
 * <ul>
 *   <li>{@code admin / admin123} → ROLE_ADMIN</li>
 *   <li>{@code viewer / viewer123} → ROLE_VIEWER</li>
 * </ul>
 * <p><b>Note:</b> Changes are in-memory only and reset on restart.
 * Activate the {@code db} profile for persistent storage.
 */
@Component
@Profile("!db")
public class UserStore implements UserStoreService {

    private final List<AdminUser>   users   = new CopyOnWriteArrayList<>();
    private final PasswordEncoder   encoder = new BCryptPasswordEncoder();

    @PostConstruct
    public void init() {
        create("admin",  "admin123",  Set.of("ROLE_ADMIN"));
        create("viewer", "viewer123", Set.of("ROLE_VIEWER"));
    }

    @Override
    public Optional<AdminUser> findByUsername(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }

    @Override
    public List<AdminUser> findAll() {
        return Collections.unmodifiableList(users);
    }

    @Override
    public AdminUser create(String username, String rawPassword, Set<String> roles) {
        AdminUser user = new AdminUser(
                username,
                encoder.encode(rawPassword),
                new HashSet<>(roles),
                Instant.now().toString());
        users.add(user);
        return user;
    }

    @Override
    public boolean changePassword(String username, String rawPassword) {
        return findByUsername(username).map(u -> {
            u.setPasswordHash(encoder.encode(rawPassword));
            return true;
        }).orElse(false);
    }

    @Override
    public boolean updateRoles(String username, Set<String> roles) {
        return findByUsername(username).map(u -> {
            u.setRoles(new HashSet<>(roles));
            return true;
        }).orElse(false);
    }

    @Override
    public boolean setActive(String username, boolean active) {
        return findByUsername(username).map(u -> {
            u.setActive(active);
            return true;
        }).orElse(false);
    }

    @Override
    public boolean delete(String username) {
        return users.removeIf(u -> u.getUsername().equals(username));
    }

    @Override
    public void recordLogin(String username) {
        findByUsername(username).ifPresent(u ->
                u.setLastLoginAt(Instant.now().toString()));
    }

    @Override
    public int count() { return users.size(); }
}
