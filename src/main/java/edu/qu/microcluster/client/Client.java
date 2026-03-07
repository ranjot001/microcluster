package edu.qu.microcluster.client;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * QU Micro-services Cluster — Interactive Client
 *
 * Usage:
 *   java Client <routerIP> <routerTCPPort>
 *   java Client 3.87.45.123 8000
 *
 * The client maintains a TCP connection to the Router.
 * From the menu the user can:
 *   1) List available services
 *   2) Choose any service with a chosen action + payload
 *   3) Exit
 */
public class Client {

    private static final int    TIMEOUT_MS  = 15_000;       // socket read timeout
    private static final AtomicInteger REQ_ID = new AtomicInteger(1);

    //TCP connection fields 
    private final String routerIP;
    private final int    routerPort;

    private Socket         socket;
    private PrintWriter    out;
    private BufferedReader in;

    //Constructor
    public Client(String routerIP, int routerPort) throws IOException {
        this.routerIP   = routerIP;
        this.routerPort = routerPort;
        connect();
    }

    private void connect() throws IOException {
        socket = new Socket(routerIP, routerPort);
        socket.setSoTimeout(TIMEOUT_MS);
        out = new PrintWriter(socket.getOutputStream(), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println("[Client] Connected to Router at " + routerIP + ":" + routerPort);
    }

    //Reconnect if the socket was closed (ex: after a timeout).
    private void ensureConnected() {
        if (socket == null || socket.isClosed()) {
            try {
                System.out.println("[Client] Reconnecting to Router...");
                connect();
            } catch (IOException e) {
                System.err.println("[Client] Reconnect failed: " + e.getMessage());
            }
        }
    }

    //Network helpers

    //Send a JSON request and return the parsed JSON response.
    private JSONObject sendRequest(JSONObject request) throws IOException {
        // Don't reconnect - reuse existing connection
        out.println(request.toString());

        try {
            String raw = in.readLine();
            if (raw == null) {
                throw new IOException("Router closed the connection.");
            }
            return new JSONObject(raw);
        } catch (SocketTimeoutException e) {
            throw new IOException("Router did not respond within " + (TIMEOUT_MS / 1000) + "s.");
        }
    }

    //Services
    //Ask the Router for the current list of available services.
    public void listServices() {
        try {
            JSONObject req = new JSONObject();
            req.put("type", "LIST_SERVICES");

            JSONObject resp = sendRequest(req);

            JSONArray services = resp.optJSONArray("services");
            if (services == null || services.length() == 0) {
                System.out.println("  (no services currently available)");
            } else {
                System.out.println("\n  Available services:");
                for (int i = 0; i < services.length(); i++) {
                    System.out.println("    [" + (i + 1) + "] " + services.getString(i));
                }
            }
        } catch (IOException e) {
            System.err.println("  ERROR listing services: " + e.getMessage());
        }
    }

    //Choose a service through the Router.
    public void invokeService(String service, String action, String payload) {
        try {
            int id = REQ_ID.getAndIncrement();

            JSONObject req = new JSONObject();
            req.put("type",      "SERVICE_REQUEST");
            req.put("requestId", id);
            req.put("service",   service.toUpperCase().trim());
            req.put("action",    action.isEmpty() ? "DEFAULT" : action.toUpperCase().trim());
            req.put("payload",   payload);

            System.out.println("\n Sending request #" + id + " to Router...");
            JSONObject resp = sendRequest(req);

            boolean success = resp.optBoolean("success", false);
            if (success) {
                System.out.println("Result:\n" + resp.optString("result", "(empty)"));
            } else {
                System.out.println("Error: " + resp.optString("error", "Unknown error"));
            }
        } catch (IOException e) {
            System.err.println("  ERROR invoking service: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                // Tell Router we're done
                JSONObject disconnect = new JSONObject();
                disconnect.put("type", "DISCONNECT");
                out.println(disconnect.toString());

                socket.close();
            }
        } catch (IOException ignored) {}
    }

    //Interactive menu

    private static void printBanner() {
        System.out.println("-----------------------------------------------");
        System.out.println("|      Micro-services Cluster Client           |");
        System.out.println("-----------------------------------------------");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("  1) List available services");
        System.out.println("  2) Invoke a service");
        System.out.println("  3) Test all services)");
        System.out.println("  0) Quit");
        System.out.print("  Choose: ");
    }

    // Walk the user through invoking a service.
    private static void interactiveInvoke(Client client, Scanner sc) {
        System.out.print("  Service name (e.g. BASE64, GZIP, HMAC, CSV, ENTROPY): ");
        String service = sc.nextLine().trim();

        System.out.println("  Actions per service:");
        System.out.println("    BASE64  → ENCODE | DECODE");
        System.out.println("    GZIP    → COMPRESS | DECOMPRESS  (payload must be Base64)");
        System.out.println("    HMAC    → SIGN (key|msg both Base64) | VERIFY (key|msg|tag all Base64)");
        System.out.println("    CSV     → STATS");
        System.out.println("    ENTROPY → ANALYZE  (payload must be Base64)");
        System.out.print("  Action (leave blank for service default): ");
        String action = sc.nextLine().trim();

        System.out.print("  Payload: ");
        String payload = sc.nextLine();

        client.invokeService(service, action, payload);
    }

    //Run a pre-built demo for every service so you can verify the cluster end-to-end.
    private static void runDemos(Client client) {
        System.out.println("\n── Demo 1: BASE64 ENCODE ──");
        client.invokeService("BASE64", "ENCODE", "Hello from QU Cluster!");

        System.out.println("\n── Demo 2: BASE64 DECODE ──");
        client.invokeService("BASE64", "DECODE", "SGVsbG8gZnJvbSBRVSBDbHVzdGVyIQ==");

        System.out.println("\n── Demo 3: CSV STATS ──");
        String csv = "age,score\n22,88\n25,92\n30,76\n28,95\n21,83";
        client.invokeService("CSV", "STATS", csv);

        System.out.println("\n── Demo 4: ENTROPY ANALYZE ──");
        client.invokeService("ENTROPY", "ANALYZE", "SGVsbG8gV29ybGQ=");

        System.out.println("\n── Demo 5: GZIP COMPRESS then DECOMPRESS ──");
        String b64Input = java.util.Base64.getEncoder()
                .encodeToString("compress me please".getBytes());
        client.invokeService("GZIP", "COMPRESS", b64Input);

        System.out.println("\n── Demo 6: HMAC SIGN ──");
        String key = java.util.Base64.getEncoder().encodeToString("secret".getBytes());
        String msg = java.util.Base64.getEncoder().encodeToString("hello".getBytes());
        client.invokeService("HMAC", "SIGN", key + "|" + msg);
    }

    private static void pause(Scanner sc) {
        System.out.print("\nPress Enter to continue...");
        sc.nextLine();
    }

    //Entry point
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Client <routerIP> <routerTCPPort>");
            System.out.println("Example: java Client 3.87.45.123 8000");
            System.exit(1);
        }

        String routerIP   = args[0];
        int    routerPort;
        try {
            routerPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port: " + args[1]);
            System.exit(1);
            return;
        }

        printBanner();

        Client client;
        try {
            client = new Client(routerIP, routerPort);
        } catch (IOException e) {
            System.err.println("Cannot connect to Router at "
                    + routerIP + ":" + routerPort + " — " + e.getMessage());
            System.exit(1);
            return;
        }

        Scanner sc = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    client.listServices();
                    pause(sc);  // ← Add
                    break;

                case "2":
                    interactiveInvoke(client, sc);
                    pause(sc);  // ← Add
                    break;

                case "3":
                    runDemos(client);
                    pause(sc);  // ← Add
                    break;

                case "0":
                    running = false;
                    break;

                default:
                    System.out.println("  Unknown option — try 0, 1, 2, or 3.");
                    pause(sc);  // ← Add
            }
        }

        client.close();
        System.out.println("Goodbye!");
    }
}

