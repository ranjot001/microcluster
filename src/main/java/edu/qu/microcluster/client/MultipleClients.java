package edu.qu.microcluster.client;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MultipleClients {

    private static final AtomicInteger REQUEST_ID = new AtomicInteger(1);
    private static final int SOCKET_TIMEOUT_MS = 300_000;

    private static final Path CLIENT_OUTPUT_DIR =
            Path.of(System.getProperty("user.dir"), "outfiles");

    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            System.out.println("Usage:");
            System.out.println("java MultipleClients <routerIP> <routerPort> <service> <action> <localFilePath> <requestCount> [threadCount] [extra1] [extra2]");
            System.out.println();
            System.out.println("Examples:");
            System.out.println("java MultipleClients 127.0.0.1 5050 CSV STATS_FILE testfiles/medium_csv_10mb.csv 5 2");
            System.out.println("java MultipleClients 127.0.0.1 5050 BASE64 ENCODE_FILE testfiles/medium_text_10mb.txt 3 1");
            System.out.println("java MultipleClients 127.0.0.1 5050 BASE64 DECODE_FILE testfiles/medium_text_10mb.txt.b64 3 1");
            System.out.println("java MultipleClients 127.0.0.1 5050 GZIP COMPRESS_FILE testfiles/medium_text_10mb.txt 3 1");
            System.out.println("java MultipleClients 127.0.0.1 5050 GZIP DECOMPRESS_FILE testfiles/medium_text_10mb.txt.gz 3 1");
            System.out.println("java MultipleClients 127.0.0.1 5050 ENTROPY ANALYZE_FILE testfiles/medium_binary_10mb.bin 3 1");
            System.out.println("java MultipleClients 127.0.0.1 5050 HMAC SIGN_FILE testfiles/medium_binary_10mb.bin 3 1 secret123");
            System.out.println("java MultipleClients 127.0.0.1 5050 HMAC VERIFY_FILE testfiles/medium_binary_10mb.bin 1 1 secret123 <BASE64_TAG>");
            System.exit(1);
        }

        String routerIP = args[0];
        int routerPort = Integer.parseInt(args[1]);
        String service = args[2].toUpperCase().trim();
        String action = args[3].toUpperCase().trim();
        String localFilePath = args[4];
        int requestCount = Integer.parseInt(args[5]);
        int threadCount = args.length >= 7 ? Integer.parseInt(args[6]) : 1;

        String extra1 = args.length >= 8 ? args[7] : null;
        String extra2 = args.length >= 9 ? args[8] : null;

        Files.createDirectories(CLIENT_OUTPUT_DIR);

        String runName = service + "_" + action + "_" + System.currentTimeMillis();
        Path summaryFile = CLIENT_OUTPUT_DIR.resolve(runName + ".clientlog.txt");

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        long overallStart = System.currentTimeMillis();

        for (int i = 0; i < requestCount; i++) {
            pool.submit(() -> sendRequest(
                    routerIP, routerPort, service, action, localFilePath, extra1, extra2, summaryFile
            ));
        }

        pool.shutdown();
        pool.awaitTermination(60, TimeUnit.MINUTES);

        long overallEnd = System.currentTimeMillis();
        String finalSummary = "\nCompleted " + requestCount + " requests in " + (overallEnd - overallStart)
                + " ms | service=" + service + " | action=" + action + "\n";
        System.out.println(finalSummary);
        append(summaryFile, finalSummary);
    }

    private static void sendRequest(
            String routerIP,
            int routerPort,
            String service,
            String action,
            String localFilePath,
            String extra1,
            String extra2,
            Path summaryFile
    ) {
        int requestId = REQUEST_ID.getAndIncrement();
        long start = System.currentTimeMillis();

        try (
                Socket socket = new Socket(routerIP, routerPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        ) {
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);

            String payload = buildPayload(service, action, localFilePath, extra1, extra2);

            JSONObject request = new JSONObject();
            request.put("type", "SERVICE_REQUEST");
            request.put("requestId", requestId);
            request.put("service", service);
            request.put("action", action);
            request.put("payload", payload);

            out.println(request.toString());

            String responseLine = in.readLine();
            long end = System.currentTimeMillis();

            if (responseLine == null) {
                String line = "[FAIL] requestId=" + requestId + " | no response\n";
                System.out.print(line);
                append(summaryFile, line);
                return;
            }

            JSONObject response = new JSONObject(responseLine);
            boolean success = response.optBoolean("success", false);

            if (!success) {
                String error = response.optString("error", "unknown");
                String line = "[ERR] requestId=" + requestId + " | time=" + (end - start)
                        + " ms | error=" + error + "\n";
                System.out.print(line);
                append(summaryFile, line);
                return;
            }

            String result = response.optString("result", "");

            if (looksLikeJson(result)) {
                JSONObject resultJson = new JSONObject(result);
                String status = resultJson.optString("status", "");

                if ("FILE_SAVED".equals(status)) {
                    String outputFileName = resultJson.getString("outputFileName");
                    String outputContentBase64 = resultJson.getString("outputContentBase64");

                    Path outputPath = uniqueOutputPath(requestId, outputFileName);
                    saveBase64ToFile(outputContentBase64, outputPath);

                    String line = "[OK] requestId=" + requestId + " | time=" + (end - start)
                            + " ms | saved=" + outputPath.toAbsolutePath() + "\n";
                    System.out.print(line);
                    append(summaryFile, line);
                    return;
                }
            }

            String line = "[OK] requestId=" + requestId + " | time=" + (end - start)
                    + " ms | result=" + result + "\n";
            System.out.print(line);
            append(summaryFile, line);

        } catch (Exception e) {
            long end = System.currentTimeMillis();
            String line = "[FAIL] requestId=" + requestId + " | time=" + (end - start)
                    + " ms | " + e.getMessage() + "\n";
            System.out.print(line);
            append(summaryFile, line);
        }
    }

    private static String buildPayload(
            String service,
            String action,
            String localFilePath,
            String extra1,
            String extra2
    ) throws Exception {

        boolean fileAction =
                action.equals("STATS_FILE") ||
                        action.equals("ENCODE_FILE") ||
                        action.equals("DECODE_FILE") ||
                        action.equals("COMPRESS_FILE") ||
                        action.equals("DECOMPRESS_FILE") ||
                        action.equals("SIGN_FILE") ||
                        action.equals("VERIFY_FILE") ||
                        action.equals("ANALYZE_FILE");

        if (fileAction) {
            Path input = Path.of(localFilePath);
            if (!Files.exists(input)) {
                throw new IllegalArgumentException("Local file not found: " + input.toAbsolutePath());
            }

            String fileName = input.getFileName().toString();
            String fileContentBase64 = Base64.getEncoder().encodeToString(Files.readAllBytes(input));

            JSONObject p = new JSONObject();
            p.put("fileName", fileName);
            p.put("fileContentBase64", fileContentBase64);

            if ("HMAC".equals(service)) {
                if ("SIGN_FILE".equals(action)) {
                    if (extra1 == null) {
                        throw new IllegalArgumentException("HMAC SIGN_FILE requires extra1 = key");
                    }
                    p.put("key", extra1);

                } else if ("VERIFY_FILE".equals(action)) {
                    if (extra1 == null || extra2 == null) {
                        throw new IllegalArgumentException("HMAC VERIFY_FILE requires extra1 = key and extra2 = tag");
                    }
                    p.put("key", extra1);
                    p.put("tag", extra2);
                }
            }

            return p.toString();
        }

        return Files.readString(Path.of(localFilePath), StandardCharsets.UTF_8);
    }

    private static Path uniqueOutputPath(int requestId, String outputFileName) {
        String uniqueName = requestId + "_" + outputFileName;
        return CLIENT_OUTPUT_DIR.resolve(uniqueName);
    }

    private static boolean looksLikeJson(String s) {
        if (s == null) return false;
        String t = s.trim();
        return t.startsWith("{") && t.endsWith("}");
    }

    private static void saveBase64ToFile(String base64, Path outputPath) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64);
        Files.write(outputPath, bytes);
    }

    private static void append(Path summaryFile, String line) {
        synchronized (MultipleClients.class) {
            try {
                Files.writeString(summaryFile, line, StandardCharsets.UTF_8,
                        Files.exists(summaryFile)
                                ? java.nio.file.StandardOpenOption.APPEND
                                : java.nio.file.StandardOpenOption.CREATE);
            } catch (Exception e) {
                System.err.println("Failed to write client log: " + e.getMessage());
            }
        }
    }
}