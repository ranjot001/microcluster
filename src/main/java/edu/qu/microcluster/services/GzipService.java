package edu.qu.microcluster.services;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GzipService implements Service {

    @Override
    public String execute(String action, String payload) throws Exception {
        String a = norm(action);
        if (a.equals("DEFAULT")) a = "COMPRESS";

        if (a.equals("COMPRESS")) {
            byte[] input = Base64.getDecoder().decode(payload);
            byte[] gz = compress(input);
            return Base64.getEncoder().encodeToString(gz);
        }

        if (a.equals("DECOMPRESS")) {
            byte[] input = Base64.getDecoder().decode(payload);
            byte[] out = decompress(input);
            return Base64.getEncoder().encodeToString(out);
        }

        if (a.equals("COMPRESS_FILE")) {
            JSONObject p = new JSONObject(payload);
            String fileName = p.getString("fileName");
            byte[] inputBytes = Base64.getDecoder().decode(p.getString("fileContentBase64"));

            byte[] gzBytes = compress(inputBytes);

            JSONObject result = new JSONObject();
            result.put("status", "FILE_SAVED");
            result.put("outputFileName", fileName + ".gz");
            result.put("outputContentBase64", Base64.getEncoder().encodeToString(gzBytes));
            return result.toString();
        }

        if (a.equals("DECOMPRESS_FILE")) {
            JSONObject p = new JSONObject(payload);
            String fileName = p.getString("fileName");
            byte[] inputBytes = Base64.getDecoder().decode(p.getString("fileContentBase64"));

            byte[] outBytes = decompress(inputBytes);

            JSONObject result = new JSONObject();
            result.put("status", "FILE_SAVED");
            result.put("outputFileName", fileName + ".out");
            result.put("outputContentBase64", Base64.getEncoder().encodeToString(outBytes));
            return result.toString();
        }

        throw new IllegalArgumentException("GZIP supports COMPRESS, DECOMPRESS, COMPRESS_FILE, DECOMPRESS_FILE");
    }

    private byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(data);
        }
        return baos.toByteArray();
    }

    private byte[] decompress(byte[] gz) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(gz))) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = gis.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
        }
        return baos.toByteArray();
    }

    private String norm(String s) {
        return (s == null) ? "DEFAULT" : s.trim().toUpperCase();
    }
}