package com.coolspy3.csmodloader;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Main {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        String accessToken = null;
        for(int i = 0; i < args.length-1; i++) {
            if(args[i].toLowerCase().equals("--accesstoken")) {
                accessToken = args[i+1];
                break;
            }
        }
        if(accessToken == null || !McUtils.validateAccessToken(accessToken)) {
            System.err.println("Invalid Access Token: " + accessToken);
            System.exit(1);
        }
        Config.load();
        KeyPair rsaKey;
        {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024, SecureRandom.getInstanceStrong());
            rsaKey = generator.genKeyPair();
        }
        try(ServerSocket sc = new ServerSocket(25565)) {
            while (true) {
                try {
                    Socket client = sc.accept();
                    // Socket server = new Socket("mc.hypixel.net", 25565);
                    Socket server = new Socket("localhost", 11223);
                    ConnectionHandler c2s = new ConnectionHandler(client, server, "mc.hypixel.net", accessToken, PacketDirection.SERVERBOUND, rsaKey);
                    ConnectionHandler s2c = new ConnectionHandler(server, client, "mc.hypixel.net", accessToken, PacketDirection.CLIENTBOUND, rsaKey);
                    c2s.setOther(s2c);
                    s2c.setOther(c2s);
                    s2c.startInNewThread();
                    c2s.startInNewThread();
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
        }
    }
}
