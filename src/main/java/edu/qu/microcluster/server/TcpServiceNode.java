package edu.qu.microcluster.server;

import java.io.BufferedReader;
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
            }
        }
    }

    private void handleClient(Socket client) throws Exception {
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
                out.println(ServiceRouter.handle(trimmed));
            }
        }
    }
}