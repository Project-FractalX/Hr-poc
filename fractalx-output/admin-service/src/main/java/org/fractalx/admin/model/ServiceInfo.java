package org.fractalx.admin.model;

public class ServiceInfo {
    private String name;
    private String url;
    private boolean running;

    public ServiceInfo(String name, String url, boolean running) {
        this.name    = name;
        this.url     = url;
        this.running = running;
    }

    public String  getName()    { return name; }
    public void    setName(String name)       { this.name = name; }
    public String  getUrl()     { return url; }
    public void    setUrl(String url)         { this.url = url; }
    public boolean isRunning()  { return running; }
    public void    setRunning(boolean running){ this.running = running; }
    public String  getStatus()      { return running ? "Running" : "Stopped"; }
    public String  getStatusClass() { return running ? "success" : "danger"; }
}
