package edu.qu.microcluster.core;

import org.json.JSONObject;
import edu.qu.microcluster.server.ServiceFactory;
import edu.qu.microcluster.services.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Random;

public class ServiceNode {
    //TCP Service Handler THread
    static class ServiceRequestHandler extends Thread {
        private int port;
        private String serviceName;

        public ServiceRequestHandler(int port, String serviceName){
            this.port = port;
            this.serviceName = serviceName;
        }
        @Override
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                System.out.println(serviceName + " TCP server listening on port " + port);

                //accepting incoming connections from the router
                //spawning a new thread for every request that comes in
                while(true){
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleRequest(clientSocket)).start();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void handleRequest(Socket socket) {
            try {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                );
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                String requestLine = in.readLine();
                System.out.println("Received: " + requestLine);
                if (requestLine != null) {
                    JSONObject req = new JSONObject(requestLine);
                    JSONObject resp = processServiceRequest(req);
                    out.println(resp.toString());
                    System.out.println("Sent: " + resp.toString());

                }
                socket.close();


            } catch (Exception e) {
                System.err.println("Error handling request: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private JSONObject processServiceRequest(JSONObject request){
            JSONObject response = new JSONObject();
            response.put("type", "SERVICE_RESPONSE");
            try {
                int requestId = request.getInt("requestId");
                String serviceName = request.getString("service");
                String action = request.getString("action");
                String payload = request.getString("payload");

                // Get the service instance (teammate's code)
                Service service = ServiceFactory.get(serviceName);

                if (service == null) {
                    // Service not found
                    response.put("requestId", requestId);
                    response.put("success", false);
                    response.put("error", "Unknown service: " + serviceName);
                    return response;
                }
                String result = service.execute(action, payload);
                response.put("requestId", requestId);
                response.put("success", true);
                response.put("result", result);

            } catch (Exception e) {
                response.put("requestId", request.optInt("requestId", -1));
                response.put("success", false);
                response.put("error", e.getMessage());
            }
            return response;
        }
    }

    //UDP sender class
    static class HeartbeatSender extends Thread {
        private String routerIP;
        private int routerPort;
        private String serviceName;
        private int servicePort;

        public HeartbeatSender(String routerIP, int routerPort, String serviceName, int servicePort){
            this.routerPort = routerPort;
            this.routerIP = routerIP;
            this.serviceName = serviceName;
            this.servicePort = servicePort;
        }
        @Override
        public void run(){
            try {
                // Create UDP socket
                DatagramSocket socket = new DatagramSocket();
                Random random = new Random();

                while (true) {
                    // Build JSON heartbeat
                    JSONObject json = new JSONObject();
                    json.put("type", "HEARTBEAT");
                    json.put("service", serviceName);
                    json.put("port", servicePort);

                    // Convert to bytes
                    byte[] buffer = json.toString().getBytes();

                    // Create and send packet using command-line args
                    InetAddress address = InetAddress.getByName(routerIP);
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, routerPort);
                    socket.send(packet);

                    System.out.println("[" + System.currentTimeMillis() + "] Heartbeat sent: " + json.toString());


                    int sleepTime = 15000 + random.nextInt(15001);
                    Thread.sleep(sleepTime);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) throws Exception {
        // Validate arguments
        if (args.length != 4) {
            System.out.println("Usage: java ServiceNode <routerIP> <routerUDPPort> <serviceName> <myTCPPort>");
            System.out.println("Example: java ServiceNode 3.87.45.123 9000 BASE64_ENCODE 8080");
            System.exit(1);
        }

        // Parse command-line arguments
        String routerIP = args[0];
        int routerUDPPort = Integer.parseInt(args[1]);
        String serviceName = args[2];
        int myServicePort = Integer.parseInt(args[3]);

//        System.out.println("=== Service Node Starting ===");
//        System.out.println("Service: " + serviceName);
//        System.out.println("My TCP Port: " + myServicePort);
//        System.out.println("Router: " + routerIP + ":" + routerUDPPort);
//        System.out.println("Sending heartbeats every 15 seconds...");
//        System.out.println("============================");

        // Start heartbeat thread
        HeartbeatSender heartbeat = new HeartbeatSender(routerIP, routerUDPPort, serviceName, myServicePort);
        heartbeat.start();

        // Start TCP service handler
        ServiceRequestHandler handler = new ServiceRequestHandler(myServicePort, serviceName);
        handler.start();

        System.out.println(serviceName + " service is running!");

        // Keep main thread alive
        Thread.sleep(Long.MAX_VALUE);


    }
}