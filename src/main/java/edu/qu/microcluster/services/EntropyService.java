package edu.qu.microcluster.services;

import java.util.Base64;

public class EntropyService implements Service {

    @Override
    public String execute(String action, String payload) {
        String a = norm(action);
        if (a.equals("DEFAULT")) a = "ANALYZE";
        if (!a.equals("ANALYZE")) throw new IllegalArgumentException("ENTROPY supports ANALYZE only (payload must be Base64)");

        byte[] data = Base64.getDecoder().decode(payload);
        double e = shannonEntropy(data);
        return "entropy=" + e;
    }

    private double shannonEntropy(byte[] data) {
        if (data.length == 0) return 0.0;

        int[] freq = new int[256];
        for (byte b : data) freq[b & 0xFF]++;

        double entropy = 0.0;
        double n = data.length;

        for (int f : freq) {
            if (f == 0) continue;
            double p = f / n;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }

    private String norm(String s) {
        return (s == null) ? "DEFAULT" : s.trim().toUpperCase();
    }
}
