package edu.qu.microcluster.archive;


public class Main {
    public static void main(String[] args) throws Exception {
        int port = 9001;
        if (args.length >= 1) port = Integer.parseInt(args[0]);
        new TcpServiceNode(port).start();
    }
}