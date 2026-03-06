package org.fractalx.admin.user;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database-backed implementation of {@link SettingsStoreService}.
 * Persists a singleton settings row with {@code id=1} in {@code admin_settings}.
 * Active when the {@code db} Spring profile is enabled.
 */
@Component
@Profile("db")
public class JpaSettingsStore implements SettingsStoreService {

    private final AdminSettingsRepository repo;

    public JpaSettingsStore(AdminSettingsRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    @Transactional
    public void init() {
        if (!repo.existsById(1)) {
            AdminSettings defaults = new AdminSettings();
            defaults.setId(1);
            repo.save(defaults);
        }
    }

    @Override
    public AdminSettings get() {
        return repo.findById(1).orElseGet(() -> {
            AdminSettings s = new AdminSettings();
            s.setId(1);
            return repo.save(s);
        });
    }

    @Override
    @Transactional
    public void update(AdminSettings settings) {
        settings.setId(1);   // always singleton row
        repo.save(settings);
    }
}
