package edu.qu.microcluster.services;

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

        byte[] input = Base64.getDecoder().decode(payload);

        if (a.equals("COMPRESS")) {
            byte[] gz = compress(input);
            return Base64.getEncoder().encodeToString(gz);
        }

        if (a.equals("DECOMPRESS")) {
            byte[] out = decompress(input);
            return Base64.getEncoder().encodeToString(out);
        }

        throw new IllegalArgumentException("GZIP supports COMPRESS or DECOMPRESS (payload must be Base64)");
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
            while ((n = gis.read(buf)) != -1) baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }

    private String norm(String s) {
        return (s == null) ? "DEFAULT" : s.trim().toUpperCase();
    }
}
