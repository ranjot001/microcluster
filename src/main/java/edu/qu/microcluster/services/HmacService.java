package edu.qu.microcluster.services;

import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HmacService implements Service {

    @Override
    public String execute(String action, String payload) throws Exception {
        String a = norm(action);
        if (a.equals("DEFAULT")) a = "SIGN";

        if (a.equals("SIGN")) {
            String[] parts = payload.split("\\|", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("HMAC SIGN payload: keyBase64|messageBase64");
            }

            byte[] key = Base64.getDecoder().decode(parts[0]);
            byte[] msg = Base64.getDecoder().decode(parts[1]);

            byte[] tag = hmacSha256(key, msg);
            return Base64.getEncoder().encodeToString(tag);
        }

        if (a.equals("VERIFY")) {
            String[] parts = payload.split("\\|", -1);
            if (parts.length != 3) {
                throw new IllegalArgumentException("HMAC VERIFY payload: keyBase64|messageBase64|tagBase64");
            }

            byte[] key = Base64.getDecoder().decode(parts[0]);
            byte[] msg = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);

            byte[] actual = hmacSha256(key, msg);
            return constantTimeEquals(actual, expected) ? "true" : "false";
        }

        if (a.equals("SIGN_FILE")) {
            JSONObject p = new JSONObject(payload);
            String fileName = p.getString("fileName");
            byte[] inputBytes = Base64.getDecoder().decode(p.getString("fileContentBase64"));
            String key = p.getString("key");

            byte[] tag = hmacSha256(key.getBytes(StandardCharsets.UTF_8), inputBytes);
            String tagB64 = Base64.getEncoder().encodeToString(tag);

            JSONObject result = new JSONObject();
            result.put("status", "FILE_SAVED");
            result.put("outputFileName", fileName + ".hmac.txt");
            result.put("outputContentBase64",
                    Base64.getEncoder().encodeToString(tagB64.getBytes(StandardCharsets.UTF_8)));
            return result.toString();
        }

        if (a.equals("VERIFY_FILE")) {
            JSONObject p = new JSONObject(payload);
            byte[] inputBytes = Base64.getDecoder().decode(p.getString("fileContentBase64"));
            String key = p.getString("key");
            byte[] expected = Base64.getDecoder().decode(p.getString("tag"));

            byte[] actual = hmacSha256(key.getBytes(StandardCharsets.UTF_8), inputBytes);
            return constantTimeEquals(actual, expected) ? "true" : "false";
        }

        throw new IllegalArgumentException("HMAC supports SIGN, VERIFY, SIGN_FILE, VERIFY_FILE");
    }

    private byte[] hmacSha256(byte[] key, byte[] message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(message);
    }

    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= (a[i] ^ b[i]);
        }
        return result == 0;
    }

    private String norm(String s) {
        return (s == null) ? "DEFAULT" : s.trim().toUpperCase();
    }
}