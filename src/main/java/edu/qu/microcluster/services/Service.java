package edu.qu.microcluster.services;

public interface Service {
    String execute(String action, String payload) throws Exception;
}