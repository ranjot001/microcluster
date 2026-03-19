package edu.qu.microcluster.services;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Base64Service implements Service {

    @Override
    public String execute(String action, String payload) throws Exception {
        String a = norm(action);

        if (a.equals("DEFAULT")) a = "ENCODE";

        if (a.equals("ENCODE")) {
            return Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        }

        if (a.equals("DECODE")) {
            byte[] decoded = Base64.getDecoder().decode(payload);
            return new String(decoded, StandardCharsets.UTF_8);
        }

        if (a.equals("ENCODE_FILE")) {
            JSONObject p = new JSONObject(payload);
            String fileName = p.getString("fileName");
            byte[] inputBytes = Base64.getDecoder().decode(p.getString("fileContentBase64"));

            byte[] encodedBytes = Base64.getEncoder().encode(inputBytes);

            JSONObject result = new JSONObject();
            result.put("status", "FILE_SAVED");
            result.put("outputFileName", fileName + ".b64");
            result.put("outputContentBase64", Base64.getEncoder().encodeToString(encodedBytes));
            return result.toString();
        }

        if (a.equals("DECODE_FILE")) {
            JSONObject p = new JSONObject(payload);
            String fileName = p.getString("fileName");
            byte[] inputBytes = Base64.getDecoder().decode(p.getString("fileContentBase64"));

            String base64Text = new String(inputBytes, StandardCharsets.UTF_8);
            byte[] decodedBytes = Base64.getMimeDecoder().decode(base64Text);

            JSONObject result = new JSONObject();
            result.put("status", "FILE_SAVED");
            result.put("outputFileName", fileName + ".decoded");
            result.put("outputContentBase64", Base64.getEncoder().encodeToString(decodedBytes));
            return result.toString();
        }

        throw new IllegalArgumentException("BASE64 supports ENCODE, DECODE, ENCODE_FILE, DECODE_FILE");
    }

    private String norm(String s) {
        return (s == null) ? "DEFAULT" : s.trim().toUpperCase();
    }
}