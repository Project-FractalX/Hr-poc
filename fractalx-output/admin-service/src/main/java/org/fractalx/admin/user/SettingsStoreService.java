package org.fractalx.admin.user;

/**
 * Abstraction over admin settings storage.
 * <p>Two implementations:
 * <ul>
 *   <li>{@link SettingsStore}    — in-memory (default)</li>
 *   <li>{@code JpaSettingsStore} — Spring Data JPA, active with {@code @Profile("db")}</li>
 * </ul>
 */
public interface SettingsStoreService {

    AdminSettings get();
    void          update(AdminSettings settings);
}
