package edu.qu.microcluster.core;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class Router {
    // Service registry - thread-safe
    private static ConcurrentHashMap<String, ServiceInfo> activeServices = new ConcurrentHashMap<>();

    // Service info class
    static class ServiceInfo {
        String serviceName;
        String ipAddress;
        int port;
        long lastHeartbeat;

        ServiceInfo(String name, String ip, int port) {
            this.serviceName = name;
            this.ipAddress = ip;
            this.port = port;
            this.lastHeartbeat = System.currentTimeMillis();
        }

        void updateHeartbeat() {
            this.lastHeartbeat = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return String.format("%s running @ socket %s:%d (last seen: %dms ago)",
                    serviceName, ipAddress, port,
                    System.currentTimeMillis() - lastHeartbeat);
        }
    }

    // UDP Heartbeat Listener Thread
    static class HeartbeatListener extends Thread {
        private DatagramSocket socket;
        private final int UDP_PORT = 9000;

        public HeartbeatListener() {
            try {
                socket = new DatagramSocket(UDP_PORT);
                System.out.println("Heartbeat listener started on UDP port " + UDP_PORT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] incomingData = new byte[1024];

            while (true) {
                try {
                    DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                    socket.receive(incomingPacket);

                    String message = new String(incomingPacket.getData(), 0, incomingPacket.getLength());
                    String senderIP = incomingPacket.getAddress().getHostAddress();

                    JSONObject json = new JSONObject(message);
                    String type = json.getString("type");

                    if (type.equals("HEARTBEAT")) {
                        String serviceName = json.getString("service");
                        int servicePort = json.getInt("port");

                        if (activeServices.containsKey(serviceName)) {
                            activeServices.get(serviceName).updateHeartbeat();
                            System.out.println("Updated: " + serviceName + " from " + senderIP);
                        } else {
                            ServiceInfo info = new ServiceInfo(serviceName, senderIP, servicePort);
                            activeServices.put(serviceName, info);
                            System.out.println("Registered: " + serviceName + " at " + senderIP + ":" + servicePort);
                        }
                    }

                    incomingData = new byte[1024];

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Heartbeat Checker Thread
    static class HeartbeatChecker extends Thread {
        private final long TIMEOUT_MS = 120000; // 120 seconds
        private final long CHECK_INTERVAL_MS = 30000; // Check every 30 seconds

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(CHECK_INTERVAL_MS);

                    long currentTime = System.currentTimeMillis();

                    for (String serviceName : activeServices.keySet()) {
                        ServiceInfo info = activeServices.get(serviceName);
                        long timeSinceLastHeartbeat = currentTime - info.lastHeartbeat;

                        if (timeSinceLastHeartbeat > TIMEOUT_MS) {
                            activeServices.remove(serviceName);
                            System.out.println("REMOVED (timeout): " + serviceName +
                                    " (no heartbeat for " + timeSinceLastHeartbeat + "ms)");
                        }
                    }

                    if (!activeServices.isEmpty()) {
                        System.out.println("\n=== Active Services ===");
                        for (ServiceInfo info : activeServices.values()) {
                            System.out.println("  " + info);
                        }
                        System.out.println("=======================\n");
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // TCP Server for Client Connections
    static class TCPServer extends Thread {
        private final int TCP_PORT = 5050;

        @Override
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(TCP_PORT);
                System.out.println("TCP server listening on port " + TCP_PORT);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress());

                    // Handle each client in separate thread
                    new Thread(() -> handleClient(clientSocket)).start();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void handleClient(Socket socket) {
            try {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                );
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                String requestLine;

                // Keep handling requests from this client
                while ((requestLine = in.readLine()) != null) {
                    System.out.println("Received from client: " + requestLine);

                    if (requestLine.trim().isEmpty()) {
                        continue;  // Skip empty lines
                    }

                    JSONObject request = new JSONObject(requestLine);
                    String type = request.getString("type");

                    JSONObject response;

                    if (type.equals("LIST_SERVICES")) {
                        response = handleListServices();

                    } else if (type.equals("SERVICE_REQUEST")) {
                        response = handleInvokeService(request);

                    } else if (type.equals("DISCONNECT")) {
                        // Client wants to close connection
                        System.out.println("Client disconnecting");
                        break;

                    } else {
                        response = new JSONObject();
                        response.put("type", "ERROR");
                        response.put("success", false);
                        response.put("error", "Unknown request type: " + type);
                    }

                    out.println(response.toString());
                    System.out.println("Sent to client: " + response.toString());
                }

                System.out.println("Client disconnected: " + socket.getInetAddress());
                socket.close();

            } catch (Exception e) {
                System.err.println("Error handling client: " + e.getMessage());
                try {
                    socket.close();
                } catch (Exception ignored) {}
            }
        }

        private JSONObject handleListServices() {
            JSONObject response = new JSONObject();
            response.put("type", "SERVICE_LIST");

            JSONArray services = new JSONArray();
            for (String serviceName : activeServices.keySet()) {
                services.put(serviceName);
            }

            response.put("services", services);
            return response;
        }

        private JSONObject handleInvokeService(JSONObject request) {
            try {
                // Extract request details
                int requestId = request.getInt("requestId");
                String serviceName = request.getString("service");
                String action = request.getString("action");
                String payload = request.getString("payload");

                // Look up service in registry
                ServiceInfo serviceInfo = activeServices.get(serviceName);

                if (serviceInfo == null) {
                    // Service not found
                    JSONObject error = new JSONObject();
                    error.put("type", "SERVICE_RESPONSE");
                    error.put("requestId", requestId);
                    error.put("success", false);
                    error.put("error", "Service not available: " + serviceName);
                    return error;
                }

                // Build request for service node
                JSONObject serviceRequest = new JSONObject();
                serviceRequest.put("type", "SERVICE_REQUEST");
                serviceRequest.put("requestId", requestId);
                serviceRequest.put("service", serviceName);
                serviceRequest.put("action", action);
                serviceRequest.put("payload", payload);

                // Forward to service node
                JSONObject serviceResponse = forwardToServiceNode(
                        serviceInfo.ipAddress,
                        serviceInfo.port,
                        serviceRequest
                );

                return serviceResponse;

            } catch (Exception e) {
                JSONObject error = new JSONObject();
                error.put("type", "SERVICE_RESPONSE");
                error.put("requestId", request.optInt("requestId", -1));
                error.put("success", false);
                error.put("error", "Router error: " + e.getMessage());
                return error;
            }
        }

        private JSONObject forwardToServiceNode(String ip, int port, JSONObject request) {
            Socket socket = null;
            try {
                // Connect to service node
                socket = new Socket(ip, port);
                socket.setSoTimeout(10000); // 10 second timeout

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                );

                // Send request
                out.println(request.toString());
                System.out.println("Forwarded to service node " + ip + ":" + port);

                // Get response
                String responseLine = in.readLine();

                if (responseLine == null) {
                    throw new Exception("Service node closed connection");
                }

                System.out.println("Received from service node: " + responseLine);
                return new JSONObject(responseLine);

            } catch (SocketTimeoutException e) {
                JSONObject error = new JSONObject();
                error.put("type", "SERVICE_RESPONSE");
                error.put("requestId", request.optInt("requestId", -1));
                error.put("success", false);
                error.put("error", "Service timeout - node not responding");
                return error;

            } catch (Exception e) {
                JSONObject error = new JSONObject();
                error.put("type", "SERVICE_RESPONSE");
                error.put("requestId", request.optInt("requestId", -1));
                error.put("success", false);
                error.put("error", "Failed to contact service: " + e.getMessage());
                return error;

            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Router Starting ===");

        // Start heartbeat listener thread
        HeartbeatListener listener = new HeartbeatListener();
        listener.start();

        // Start heartbeat checker thread
        HeartbeatChecker checker = new HeartbeatChecker();
        checker.start();

        // Start TCP server thread  ← ADDED!
        TCPServer tcpServer = new TCPServer();
        tcpServer.start();

        System.out.println("Router is running...");
        System.out.println("Waiting for service nodes to register...\n");

        // Main thread keeps program alive
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}