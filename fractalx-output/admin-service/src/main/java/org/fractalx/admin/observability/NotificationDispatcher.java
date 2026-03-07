package org.fractalx.admin.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Routes a fired {@link AlertEvent} to all configured notification channels.
 * Each channel self-guards with an {@code enabled} flag.
 */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final AlertConfigProperties config;
    private final AlertChannels         channels;

    public NotificationDispatcher(AlertConfigProperties config, AlertChannels channels) {
        this.config   = config;
        this.channels = channels;
    }

    public void dispatch(AlertEvent event) {
        if (config.getChannels().getAdminUi().isEnabled()) {
            channels.publishToAdminUi(event);
        }
        if (config.getChannels().getWebhook().isEnabled()) {
            channels.sendToWebhook(event, config.getChannels().getWebhook().getUrl());
        }
        if (config.getChannels().getEmail().isEnabled()) {
            channels.sendEmail(event, config.getChannels().getEmail());
        }
        if (config.getChannels().getSlack().isEnabled()) {
            channels.sendToSlack(event, config.getChannels().getSlack().getWebhookUrl());
        }
        log.debug("Dispatched alert {} to configured channels", event.getId());
    }
}
