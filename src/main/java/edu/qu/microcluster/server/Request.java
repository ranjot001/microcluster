package edu.qu.microcluster.server;

public class Request {
    public final String service;
    public final String action;
    public final String payload;

    public Request(String service, String action, String payload) {
        this.service = service;
        this.action = action;
        this.payload = payload;
    }
}