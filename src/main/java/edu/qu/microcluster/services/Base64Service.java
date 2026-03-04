package edu.qu.microcluster.services;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Base64Service implements Service {

    @Override
    public String execute(String action, String payload) {
        String a = norm(action);

        if (a.equals("DEFAULT")) a = "ENCODE";

        if (a.equals("ENCODE")) {
            return Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        }

        if (a.equals("DECODE")) {
            byte[] decoded = Base64.getDecoder().decode(payload);
            return new String(decoded, StandardCharsets.UTF_8);
        }

        throw new IllegalArgumentException("BASE64 supports ENCODE or DECODE");
    }

    private String norm(String s) {
        return (s == null) ? "DEFAULT" : s.trim().toUpperCase();
    }
}