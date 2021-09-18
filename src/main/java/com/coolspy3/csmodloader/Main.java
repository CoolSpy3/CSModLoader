package com.coolspy3.csmodloader;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;

public class Main {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        String accessToken = null;
        String gameDir = null;
        String username = null;
        UUID uuid = null;
        {
            Iterator<String> argIttr = Arrays.asList(args).iterator();
            for(;;) {
                String arg = argIttr.next().toLowerCase();
                if(!argIttr.hasNext()) {
                    break;
                }
                switch(arg) {
                    case "--accesstoken":
                        accessToken = argIttr.next();
                        continue;
                    case "--gamedir":
                        gameDir = argIttr.next();
                        continue;
                    case "--username":
                        username = argIttr.next();
                        continue;
                    case "--uuid":
                        uuid = UUID.fromString(argIttr.next());
                        continue;
                    default:
                        break;
                }
            }
        }
        new GameArgs(gameDir, username, uuid).set();
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
