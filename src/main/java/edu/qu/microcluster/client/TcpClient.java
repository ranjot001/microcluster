package edu.qu.microcluster.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TcpClient {

    public static void main(String[] args) throws Exception{

        Socket s = new Socket("localhost",9000);

        BufferedReader in =
                new BufferedReader(
                        new InputStreamReader(s.getInputStream()));

        PrintWriter out =
                new PrintWriter(s.getOutputStream(),true);

        Scanner sc = new Scanner(System.in);

        while(true){

            String line = sc.nextLine();

            out.println(line);

            System.out.println(in.readLine());
        }
    }
}
