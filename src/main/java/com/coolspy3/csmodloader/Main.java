package com.coolspy3.csmodloader;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;
import javax.swing.SwingUtilities;
import com.coolspy3.csmodloader.gui.MainWindow;
import com.coolspy3.csmodloader.gui.TextAreaFrame;
import com.coolspy3.csmodloader.network.ConnectionHandler;
import com.coolspy3.csmodloader.util.Utils;

public class Main
{

    public static void main(String[] args)
    {
        MainWindow.create();

        if (true) return;

        String accessToken = null;
        String gameDir = null;
        String username = null;
        UUID uuid = null;

        {
            Iterator<String> argIttr = Arrays.asList(args).iterator();
            if (args.length > 0) for (;;)
            {
                String arg = argIttr.next().toLowerCase();

                if (!argIttr.hasNext()) break;

                switch (arg)
                {
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

        new GameArgs(new File(gameDir), username, uuid).set();

        Config.init();
        try
        {
            Config.load();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Utils.safeCreateAndWaitFor(() -> new TextAreaFrame("Failed to load config file", e));
            System.exit(1);
        }

        KeyPair rsaKey;
        {
            KeyPairGenerator generator = Utils.noFail(() -> KeyPairGenerator.getInstance("RSA"));
            generator.initialize(1024, Utils.noFail(SecureRandom::getInstanceStrong));
            rsaKey = generator.genKeyPair();
        }

        try (ServerSocket sc = new ServerSocket(25565))
        {
            while (true)
            {
                try
                {
                    Socket client = sc.accept();
                    // Socket server = new Socket("mc.hypixel.net", 25565);
                    Socket server = new Socket("localhost", 11223);
                    ConnectionHandler.start(client, server, "mc.hypixel.net", accessToken, rsaKey);
                }
                catch (Exception e)
                {
                    e.printStackTrace(System.err);
                    Utils.safe(() -> new TextAreaFrame(e));
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Utils.safeCreateAndWaitFor(() -> new TextAreaFrame(e));
            System.exit(1);
        }
    }
}
