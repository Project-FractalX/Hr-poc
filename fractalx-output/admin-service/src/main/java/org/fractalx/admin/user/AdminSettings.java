package org.fractalx.admin.user;

import jakarta.persistence.*;

/**
 * Admin-wide settings for the FractalX admin dashboard.
 * Annotated as a JPA {@code @Entity} — active in DB mode, harmless in memory mode.
 *
 * <p>A singleton row with {@code id=1} is maintained in the database.
 * In memory mode the {@code id} field is ignored.
 */
@Entity
@Table(name = "admin_settings")
public class AdminSettings {

    @Id
    @Column(name = "id")
    private int     id                = 1;   // singleton row — always 1

    @Column(name = "site_name")
    private String  siteName          = "FractalX Admin";

    @Column(name = "theme")
    private String  theme             = "light";   // light | dark

    @Column(name = "session_timeout_min")
    private int     sessionTimeoutMin = 30;

    @Column(name = "default_alert_email")
    private String  defaultAlertEmail = "";

    @Column(name = "maintenance_mode")
    private boolean maintenanceMode   = false;

    public int     getId()                         { return id; }
    public void    setId(int id)                   { this.id = id; }

    public String  getSiteName()                   { return siteName; }
    public void    setSiteName(String s)           { this.siteName = s; }

    public String  getTheme()                      { return theme; }
    public void    setTheme(String t)              { this.theme = t; }

    public int     getSessionTimeoutMin()          { return sessionTimeoutMin; }
    public void    setSessionTimeoutMin(int m)     { this.sessionTimeoutMin = m; }

    public String  getDefaultAlertEmail()          { return defaultAlertEmail; }
    public void    setDefaultAlertEmail(String e)  { this.defaultAlertEmail = e; }

    public boolean isMaintenanceMode()             { return maintenanceMode; }
    public void    setMaintenanceMode(boolean m)   { this.maintenanceMode = m; }
}
