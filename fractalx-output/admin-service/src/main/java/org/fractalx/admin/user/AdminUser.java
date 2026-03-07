package org.fractalx.admin.user;

import jakarta.persistence.*;
import java.util.Set;

/**
 * Represents an admin dashboard user.
 * Annotated as a JPA {@code @Entity} — active in DB mode (profile {@code db}),
 * harmless in memory mode (JPA autoconfiguration is excluded by default).
 *
 * <p>Passwords are stored as BCrypt hashes — never in plaintext.
 * <p>Supported roles: {@code ROLE_ADMIN}, {@code ROLE_OPERATOR}, {@code ROLE_VIEWER}
 */
@Entity
@Table(name = "admin_users")
public class AdminUser {

    @Id
    @Column(name = "username", nullable = false, length = 100)
    private String      username;

    @Column(name = "password_hash", nullable = false)
    private String      passwordHash;   // BCrypt encoded

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "admin_user_roles",
                     joinColumns = @JoinColumn(name = "username"))
    @Column(name = "role")
    private Set<String> roles;          // e.g. { "ROLE_ADMIN" }

    @Column(name = "created_at")
    private String      createdAt;

    @Column(name = "last_login_at")
    private String      lastLoginAt;

    @Column(name = "active")
    private boolean     active;

    public AdminUser() {}

    public AdminUser(String username, String passwordHash,
                     Set<String> roles, String createdAt) {
        this.username     = username;
        this.passwordHash = passwordHash;
        this.roles        = roles;
        this.createdAt    = createdAt;
        this.active       = true;
    }

    public String      getUsername()     { return username; }
    public void        setUsername(String u)  { this.username = u; }

    public String      getPasswordHash() { return passwordHash; }
    public void        setPasswordHash(String h) { this.passwordHash = h; }

    public Set<String> getRoles()        { return roles; }
    public void        setRoles(Set<String> r) { this.roles = r; }

    public String      getCreatedAt()    { return createdAt; }
    public void        setCreatedAt(String c) { this.createdAt = c; }

    public String      getLastLoginAt()  { return lastLoginAt; }
    public void        setLastLoginAt(String l) { this.lastLoginAt = l; }

    public boolean     isActive()        { return active; }
    public void        setActive(boolean a) { this.active = a; }
}
