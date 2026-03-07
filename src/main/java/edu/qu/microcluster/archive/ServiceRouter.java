package edu.qu.microcluster.archive;

import edu.qu.microcluster.server.ServiceFactory;
import edu.qu.microcluster.services.Service;

public class ServiceRouter {

    public static String handle(String requestLine) {
        try {
            Request req = RequestParser.parse(requestLine);
            Service svc = ServiceFactory.get(req.service);

            if (svc == null) {
                return "ERR|Unknown service: " + req.service;
            }

            String result = svc.execute(req.action, req.payload);
            return "OK|" + result;

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) msg = e.getClass().getSimpleName();
            return "ERR|" + msg;
        }
    }
}
