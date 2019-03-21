package com.comxa.universo42.gettunnel.modelo;

public class ConnectionConfig {
    private String hostHeader;
    private String pass;
    private String bodyInject;

    public String getHostHeader() {
        return hostHeader;
    }

    public void setHostHeader(String hostHeader) {
        this.hostHeader = hostHeader;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getBodyInject() {
        return bodyInject;
    }

    public void setBodyInject(String bodyInject) {
        this.bodyInject = bodyInject;
    }
}