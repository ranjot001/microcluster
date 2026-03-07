package edu.qu.microcluster.archive;

public class RequestParser {

    public static Request parse(String line) {
        if (line == null) throw new IllegalArgumentException("Empty request");
        line = line.trim();
        if (line.isEmpty()) throw new IllegalArgumentException("Empty request");

        String[] parts = line.split("\\|", 3);

        if (parts.length == 1) {
            throw new IllegalArgumentException("Invalid format. Use SERVICE|ACTION|PAYLOAD");
        }

        String service = parts[0].trim();
        String action;
        String payload;

        if (parts.length == 2) {
            action = "DEFAULT";
            payload = parts[2];
            payload = payload.replace("\\n", "\n");
        } else {

            action = parts[1].trim();
            payload = parts[2];
            payload = payload.replace("\\n", "\n");
        }

        if (service.isEmpty()) throw new IllegalArgumentException("Missing service");
        if (action.isEmpty()) action = "DEFAULT";

        return new Request(service, action, payload);
    }
}