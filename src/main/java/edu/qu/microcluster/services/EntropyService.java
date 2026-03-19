package edu.qu.microcluster.services;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EntropyService implements Service {

    @Override
    public String execute(String action, String payload) throws Exception {
        String a = norm(action);
        if (a.equals("DEFAULT")) a = "ANALYZE";

        if (a.equals("ANALYZE")) {
            byte[] data = Base64.getDecoder().decode(payload);
            double e = shannonEntropy(data);
            return "entropy=" + e;
        }

        if (a.equals("ANALYZE_FILE")) {
            JSONObject p = new JSONObject(payload);
            String fileName = p.getString("fileName");
            byte[] data = Base64.getDecoder().decode(p.getString("fileContentBase64"));

            double e = shannonEntropy(data);
            String report = "Entropy: " + e;

            JSONObject result = new JSONObject();
            result.put("status", "FILE_SAVED");
            result.put("outputFileName", fileName + ".entropy.txt");
            result.put("outputContentBase64",
                    Base64.getEncoder().encodeToString(report.getBytes(StandardCharsets.UTF_8)));
            return result.toString();
        }

        throw new IllegalArgumentException("ENTROPY supports ANALYZE and ANALYZE_FILE");
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