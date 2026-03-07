package org.fractalx.admin.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Alert configuration properties bound from {@code fractalx.alerting.*}.
 * Defaults match the generated {@code alerting.yml}.
 */
@Component
@ConfigurationProperties(prefix = "fractalx.alerting")
public class AlertConfigProperties {

    private boolean       enabled         = true;
    private long          evalIntervalMs  = 30_000;
    private List<AlertRule> rules         = defaultRules();
    private Channels      channels        = new Channels();

    public boolean         isEnabled()                        { return enabled; }
    public void            setEnabled(boolean e)              { this.enabled = e; }
    public long            getEvalIntervalMs()                { return evalIntervalMs; }
    public void            setEvalIntervalMs(long m)          { this.evalIntervalMs = m; }
    public List<AlertRule> getRules()                         { return rules; }
    public void            setRules(List<AlertRule> r)        { this.rules = r; }
    public Channels        getChannels()                      { return channels; }
    public void            setChannels(Channels c)            { this.channels = c; }

    private static List<AlertRule> defaultRules() {
        List<AlertRule> list = new ArrayList<>();
        AlertRule down = new AlertRule();
        down.setName("service-down"); down.setCondition("health");
        down.setThreshold(1); down.setSeverity(AlertSeverity.CRITICAL);
        down.setConsecutiveFailures(2);
        list.add(down);

        AlertRule rt = new AlertRule();
        rt.setName("high-response-time"); rt.setCondition("response-time");
        rt.setThreshold(2000); rt.setSeverity(AlertSeverity.WARNING);
        rt.setConsecutiveFailures(3);
        list.add(rt);

        AlertRule er = new AlertRule();
        er.setName("error-rate"); er.setCondition("error-rate");
        er.setThreshold(10); er.setSeverity(AlertSeverity.WARNING);
        er.setConsecutiveFailures(3);
        list.add(er);
        return list;
    }

    public static class Channels {
        private AdminUI  adminUi = new AdminUI();
        private Webhook  webhook = new Webhook();
        private Email    email   = new Email();
        private Slack    slack   = new Slack();

        public AdminUI getAdminUi() { return adminUi; }
        public void    setAdminUi(AdminUI a) { this.adminUi = a; }
        public Webhook getWebhook() { return webhook; }
        public void    setWebhook(Webhook w) { this.webhook = w; }
        public Email   getEmail()   { return email; }
        public void    setEmail(Email e) { this.email = e; }
        public Slack   getSlack()   { return slack; }
        public void    setSlack(Slack s) { this.slack = s; }
    }

    public static class AdminUI {
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean e) { this.enabled = e; }
    }

    public static class Webhook {
        private boolean enabled = false;
        private String  url     = "";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean e) { this.enabled = e; }
        public String getUrl() { return url; }
        public void setUrl(String u) { this.url = u; }
    }

    public static class Email {
        private boolean      enabled  = false;
        private String       smtpHost = "";
        private int          smtpPort = 587;
        private String       from     = "";
        private List<String> to       = new ArrayList<>();
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean e) { this.enabled = e; }
        public String getSmtpHost() { return smtpHost; }
        public void setSmtpHost(String h) { this.smtpHost = h; }
        public int getSmtpPort() { return smtpPort; }
        public void setSmtpPort(int p) { this.smtpPort = p; }
        public String getFrom() { return from; }
        public void setFrom(String f) { this.from = f; }
        public List<String> getTo() { return to; }
        public void setTo(List<String> t) { this.to = t; }
    }

    public static class Slack {
        private boolean enabled    = false;
        private String  webhookUrl = "";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean e) { this.enabled = e; }
        public String getWebhookUrl() { return webhookUrl; }
        public void setWebhookUrl(String u) { this.webhookUrl = u; }
    }
}
