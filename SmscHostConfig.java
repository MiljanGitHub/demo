package com.example.demo;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class SmscHostConfig {

    public String ip;
    public int port;
    public String systemId;
    public String password;

    public SmscHostConfig() {}

    public SmscHostConfig(String ip, int port, String systemId, String password) {
        this.ip = ip;
        this.port = port;
        this.systemId = systemId;
        this.password = password;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isDefined() {
        return ip != null && port > 0 && systemId != null && password != null;
    }
}

