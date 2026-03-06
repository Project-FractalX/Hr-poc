package org.fractalx.admin.user;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe in-memory holder for the singleton {@link AdminSettings} instance.
 * Active when the {@code db} profile is NOT active.
 *
 * <p>Activate the {@code db} profile for persistent settings storage.
 */
@Component
@Profile("!db")
public class SettingsStore implements SettingsStoreService {

    private final AtomicReference<AdminSettings> settings =
            new AtomicReference<>(new AdminSettings());

    @Override
    public AdminSettings get()                    { return settings.get(); }

    @Override
    public void          update(AdminSettings s)  { settings.set(s); }
}
