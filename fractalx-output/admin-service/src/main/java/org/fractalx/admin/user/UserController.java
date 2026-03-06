package org.fractalx.admin.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API for user management and admin settings.
 *
 * <pre>
 * GET    /api/users                        — list all users (passwords masked)
 * POST   /api/users                        — create a new user
 * PUT    /api/users/{username}             — update user (active flag)
 * DELETE /api/users/{username}             — delete a user
 * PUT    /api/users/{username}/password    — change password
 * PUT    /api/users/{username}/roles       — update roles
 * GET    /api/settings                     — get admin settings
 * PUT    /api/settings                     — update admin settings
 * GET    /api/auth/profile                 — current authenticated user info
 * </pre>
 *
 * <p>Injected via {@link UserStoreService} / {@link SettingsStoreService} interfaces,
 * so both in-memory (default) and JPA (db profile) stores are transparently supported.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserStoreService     userStore;
    private final SettingsStoreService settingsStore;

    public UserController(UserStoreService userStore, SettingsStoreService settingsStore) {
        this.userStore     = userStore;
        this.settingsStore = settingsStore;
    }

    // ---- Users ---------------------------------------------------------------

    @GetMapping("/api/users")
    public ResponseEntity<List<Map<String, Object>>> listUsers() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AdminUser u : userStore.findAll()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("username",    u.getUsername());
            entry.put("passwordHash","***");         // never expose hash
            entry.put("roles",       u.getRoles());
            entry.put("active",      u.isActive());
            entry.put("createdAt",   u.getCreatedAt());
            entry.put("lastLoginAt", u.getLastLoginAt());
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/users")
    public ResponseEntity<Map<String, Object>> createUser(
            @RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        @SuppressWarnings("unchecked")
        List<String> roleList = (List<String>) body.getOrDefault("roles",
                List.of("ROLE_VIEWER"));

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "username and password are required"));
        }
        if (userStore.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "User already exists: " + username));
        }
        userStore.create(username, password, new HashSet<>(roleList));
        return ResponseEntity.ok(Map.of("created", username, "roles", roleList));
    }

    @PutMapping("/api/users/{username}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable("username") String username,
            @RequestBody Map<String, Object> body) {
        if (body.containsKey("active")) {
            boolean active = Boolean.parseBoolean(body.get("active").toString());
            userStore.setActive(username, active);
        }
        return ResponseEntity.ok(Map.of("updated", username));
    }

    @DeleteMapping("/api/users/{username}")
    public ResponseEntity<Map<String, Object>> deleteUser(
            @PathVariable("username") String username) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName().equals(username)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot delete your own account"));
        }
        boolean deleted = userStore.delete(username);
        return deleted
            ? ResponseEntity.ok(Map.of("deleted", username))
            : ResponseEntity.notFound().build();
    }

    @PutMapping("/api/users/{username}/password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @PathVariable("username") String username,
            @RequestBody Map<String, Object> body) {
        String newPassword = (String) body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "newPassword is required"));
        }
        boolean changed = userStore.changePassword(username, newPassword);
        return changed
            ? ResponseEntity.ok(Map.of("updated", username))
            : ResponseEntity.notFound().build();
    }

    @PutMapping("/api/users/{username}/roles")
    public ResponseEntity<Map<String, Object>> updateRoles(
            @PathVariable("username") String username,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> roleList = (List<String>) body.getOrDefault("roles", List.of());
        boolean updated = userStore.updateRoles(username, new HashSet<>(roleList));
        return updated
            ? ResponseEntity.ok(Map.of("updated", username, "roles", roleList))
            : ResponseEntity.notFound().build();
    }

    // ---- Settings ------------------------------------------------------------

    @GetMapping("/api/settings")
    public ResponseEntity<AdminSettings> getSettings() {
        return ResponseEntity.ok(settingsStore.get());
    }

    @PutMapping("/api/settings")
    public ResponseEntity<AdminSettings> updateSettings(
            @RequestBody AdminSettings settings) {
        settingsStore.update(settings);
        return ResponseEntity.ok(settingsStore.get());
    }

    // ---- Profile -------------------------------------------------------------

    @GetMapping("/api/auth/profile")
    public ResponseEntity<Map<String, Object>> getProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return ResponseEntity.ok(Map.of("authenticated", false));
        return userStore.findByUsername(auth.getName()).map(u -> {
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("username",    u.getUsername());
            profile.put("roles",       u.getRoles());
            profile.put("lastLoginAt", u.getLastLoginAt());
            profile.put("active",      u.isActive());
            return ResponseEntity.ok(profile);
        }).orElse(ResponseEntity.ok(Map.of("username", auth.getName())));
    }
}
