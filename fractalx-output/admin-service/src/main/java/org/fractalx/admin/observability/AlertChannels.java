package org.fractalx.admin.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Delivery implementations for all four alert notification channels.
 *
 * <ul>
 *   <li><b>Admin UI</b>  — Server-Sent Events ({@link SseEmitter})</li>
 *   <li><b>Webhook</b>   — HTTP POST JSON payload to a configured URL</li>
 *   <li><b>Email</b>     — HTML email via JavaMailSender (SMTP)</li>
 *   <li><b>Slack</b>     — Slack Incoming Webhook POST</li>
 * </ul>
 */
@Component
public class AlertChannels {

    private static final Logger log = LoggerFactory.getLogger(AlertChannels.class);

    private final List<SseEmitter> sseEmitters = new CopyOnWriteArrayList<>();
    private final RestTemplate      rest        = new RestTemplate();

    // ---- Admin UI (SSE) ----

    /**
     * Returns a new long-lived {@link SseEmitter} for the admin alerts stream.
     * Registered emitters receive every subsequent alert in real time.
     */
    public SseEmitter subscribeAdminUi() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        sseEmitters.add(emitter);
        emitter.onCompletion(() -> sseEmitters.remove(emitter));
        emitter.onTimeout(()    -> sseEmitters.remove(emitter));
        emitter.onError(e       -> sseEmitters.remove(emitter));
        return emitter;
    }

    public void publishToAdminUi(AlertEvent event) {
        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : sseEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("alert")
                        .data(toJson(event)));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        sseEmitters.removeAll(dead);
    }

    // ---- Webhook ----

    public void sendToWebhook(AlertEvent event, String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        Map<String, Object> payload = Map.of(
                "id",        event.getId(),
                "timestamp", event.getTimestamp().toString(),
                "service",   event.getService(),
                "severity",  event.getSeverity().name(),
                "message",   event.getMessage()
        );
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                rest.postForEntity(webhookUrl, payload, Void.class);
                return;
            } catch (Exception e) {
                log.warn("Webhook delivery attempt {} failed: {}", i + 1, e.getMessage());
                if (i < maxRetries - 1) {
                    try { Thread.sleep(1_000L * (i + 1)); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt(); return;
                    }
                }
            }
        }
        log.error("Webhook delivery failed after {} attempts for alert {}", maxRetries, event.getId());
    }

    // ---- Email (SMTP) ----

    public void sendEmail(AlertEvent event, AlertConfigProperties.Email emailCfg) {
        if (emailCfg.getTo().isEmpty() || emailCfg.getSmtpHost().isBlank()) return;
        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(emailCfg.getSmtpHost());
            sender.setPort(emailCfg.getSmtpPort());
            Properties props = sender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.starttls.enable", "true");

            String subject = "[FractalX Alert] " + event.getSeverity().name()
                    + ": " + event.getService() + " — " + event.getMessage();
            String body = buildEmailBody(event);

            for (String to : emailCfg.getTo()) {
                jakarta.mail.internet.MimeMessage msg = sender.createMimeMessage();
                org.springframework.mail.javamail.MimeMessageHelper helper =
                        new org.springframework.mail.javamail.MimeMessageHelper(msg, false, "UTF-8");
                helper.setFrom(emailCfg.getFrom());
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(body, true);
                sender.send(msg);
            }
            log.info("Alert email sent for event {}", event.getId());
        } catch (Exception e) {
            log.error("Failed to send alert email: {}", e.getMessage());
        }
    }

    private String buildEmailBody(AlertEvent event) {
        String color = switch (event.getSeverity()) {
            case CRITICAL -> "#dc3545";
            case WARNING  -> "#ffc107";
            case INFO     -> "#0dcaf0";
        };
        return "<html><body style=\"font-family:sans-serif;padding:24px\">"
                + "<h2 style=\"color:" + color + "\">[" + event.getSeverity().name()
                + "] " + event.getService() + "</h2>"
                + "<table><tr><td><b>Service:</b></td><td>" + event.getService() + "</td></tr>"
                + "<tr><td><b>Time:</b></td><td>" + event.getTimestamp() + "</td></tr>"
                + "<tr><td><b>Message:</b></td><td>" + event.getMessage() + "</td></tr></table>"
                + "<p style=\"color:#6c757d;font-size:12px\">Sent by FractalX AlertManager</p>"
                + "</body></html>";
    }

    // ---- Slack ----

    public void sendToSlack(AlertEvent event, String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        String color = switch (event.getSeverity()) {
            case CRITICAL -> "danger";
            case WARNING  -> "warning";
            case INFO     -> "good";
        };
        Map<String, Object> payload = Map.of(
                "text", ":bell: *FractalX Alert* — " + event.getSeverity().name(),
                "attachments", List.of(Map.of(
                        "color",  color,
                        "fields", List.of(
                                Map.of("title", "Service",   "value", event.getService(),  "short", true),
                                Map.of("title", "Severity",  "value", event.getSeverity().name(), "short", true),
                                Map.of("title", "Message",   "value", event.getMessage(), "short", false),
                                Map.of("title", "Timestamp", "value", event.getTimestamp().toString(), "short", false)
                        )
                ))
        );
        try {
            rest.postForEntity(webhookUrl, payload, Void.class);
        } catch (Exception e) {
            log.error("Slack alert delivery failed: {}", e.getMessage());
        }
    }

    // ---- Helpers ----

    private String toJson(AlertEvent e) {
        return "{\"id\":\"%s\",\"timestamp\":\"%s\",\"service\":\"%s\",\"severity\":\"%s\",\"message\":\"%s\",\"resolved\":%b}"
                .formatted(e.getId(), e.getTimestamp(), e.getService(),
                        e.getSeverity().name(), e.getMessage(), e.isResolved());
    }
}
