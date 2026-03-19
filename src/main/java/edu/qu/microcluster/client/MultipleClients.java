package edu.qu.microcluster.client;

import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MultipleClients {

    private static final int TIMEOUT_MS = 300_000;
    private static final AtomicInteger REQ_ID = new AtomicInteger(1);

    private static final Path OUTPUT_DIR =
            Path.of(System.getProperty("user.dir"), "outfiles");

    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.out.println("Usage: java MultipleClients <routerIP> <port>");
            return;
        }

        String routerIP = args[0];
        int port = Integer.parseInt(args[1]);

        Files.createDirectories(OUTPUT_DIR);

        Scanner sc = new Scanner(System.in);

        System.out.println("Service (BASE64, GZIP, HMAC, CSV, ENTROPY): ");
        String service = sc.nextLine().trim().toUpperCase();

        String action = selectAction(service, sc);

        System.out.print("Enter file path (client machine): ");
        String filePath = sc.nextLine().trim();

        System.out.print("Number of requests: ");
        int totalRequests = Integer.parseInt(sc.nextLine());

        System.out.print("Concurrent clients (threads): ");
        int threads = Integer.parseInt(sc.nextLine());

        String payload = buildPayload(service, action, filePath, sc);

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        long start = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    sendRequest(routerIP, port, service, action, payload);
                } catch (Exception e) {
                    System.out.println("[ERR] " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.sleep(100);
        }

        long end = System.currentTimeMillis();
        System.out.println("\nCompleted in " + (end - start) + " ms");
    }

    private static void sendRequest(String ip, int port, String service,
                                    String action, String payload) throws Exception {

        int id = REQ_ID.getAndIncrement();

        try (Socket socket = new Socket(ip, port)) {
            socket.setSoTimeout(TIMEOUT_MS);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            JSONObject req = new JSONObject();
            req.put("type", "SERVICE_REQUEST");
            req.put("requestId", id);
            req.put("service", service);
            req.put("action", action);
            req.put("payload", payload);

            long start = System.currentTimeMillis();

            out.println(req.toString());

            String response = in.readLine();

            long time = System.currentTimeMillis() - start;

            JSONObject resp = new JSONObject(response);

            if (resp.optBoolean("success")) {
                String result = resp.optString("result");
                saveOutput(id, service, action, result);
                System.out.println("[OK] requestId=" + id + " | time=" + time + " ms");
            } else {
                System.out.println("[ERR] requestId=" + id + " | error=" + resp.optString("error"));
            }
        }
    }

    private static void saveOutput(int id, String service, String action, String data) throws Exception {

        String fileName = service + "_" + action + "_" + id + ".out";
        Path output = OUTPUT_DIR.resolve(fileName);

        // decode base64 if needed
        if (action.contains("DECODE") || action.contains("DECOMPRESS")) {
            byte[] bytes = Base64.getDecoder().decode(data);
            Files.write(output, bytes);
        } else {
            Files.writeString(output, data);
        }
    }

    private static String selectAction(String service, Scanner sc) {

        System.out.println("Select Action:");

        switch (service) {
            case "BASE64":
                System.out.println("1) ENCODE_FILE");
                System.out.println("2) DECODE_FILE");
                break;

            case "GZIP":
                System.out.println("1) COMPRESS_FILE");
                System.out.println("2) DECOMPRESS_FILE");
                break;

            case "HMAC":
                System.out.println("1) SIGN_FILE");
                System.out.println("2) VERIFY_FILE");
                break;

            case "CSV":
                System.out.println("1) STATS_FILE");
                break;

            case "ENTROPY":
                System.out.println("1) ANALYZE_FILE");
                break;
        }

        String choice = sc.nextLine();

        return switch (service) {
            case "BASE64" -> choice.equals("2") ? "DECODE_FILE" : "ENCODE_FILE";
            case "GZIP" -> choice.equals("2") ? "DECOMPRESS_FILE" : "COMPRESS_FILE";
            case "HMAC" -> choice.equals("2") ? "VERIFY_FILE" : "SIGN_FILE";
            case "CSV" -> "STATS_FILE";
            case "ENTROPY" -> "ANALYZE_FILE";
            default -> "DEFAULT";
        };
    }

    private static String buildPayload(String service, String action, String filePath, Scanner sc) throws Exception {

        byte[] fileBytes = Files.readAllBytes(Path.of(filePath));
        String fileB64 = Base64.getEncoder().encodeToString(fileBytes);

        if (service.equals("HMAC")) {
            System.out.print("Enter key: ");
            String key = sc.nextLine();
            String keyB64 = Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));

            if (action.equals("VERIFY_FILE")) {
                System.out.print("Enter expected tag: ");
                String tag = sc.nextLine();
                return keyB64 + "|" + fileB64 + "|" + tag;
            }
            return keyB64 + "|" + fileB64;
        }

        return fileB64;
    }
}