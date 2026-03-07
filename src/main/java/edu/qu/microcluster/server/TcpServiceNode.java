package edu.qu.microcluster.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpServiceNode {

    private final int port;

    public TcpServiceNode(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                Socket client = server.accept();
                handleClient(client);

                Thread t = new Thread(() -> handleClient(client));
                t.setDaemon(true);
                t.start();
            }
        }
    }

    private void handleClient(Socket client) {
        String remote = client.getRemoteSocketAddress().toString();
        System.out.println("[TcpServiceNode] Client connected: " + remote);

        // Fix 2: catch exceptions inside the handler and send an error response instead
        // of crashing
        try (Socket c = client;
                BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
                PrintWriter out = new PrintWriter(c.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.equalsIgnoreCase("QUIT")) {
                    out.println("OK|bye");
                    break;
                }
                try {
                    out.println(ServiceRouter.handle(trimmed));
                } catch (Exception e) {
                    // Return the error to the client rather than killing the connection
                    out.println("ERR|Internal error: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("[TcpServiceNode] Connection error with " + remote + ": " + e.getMessage());
        } finally {
            System.out.println("[TcpServiceNode] Client disconnected: " + remote);
        }
    }
}
