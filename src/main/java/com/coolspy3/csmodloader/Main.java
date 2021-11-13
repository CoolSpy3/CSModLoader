package com.coolspy3.csmodloader;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;

import com.coolspy3.csmodloader.gui.MainWindow;
import com.coolspy3.csmodloader.gui.TextAreaFrame;
import com.coolspy3.csmodloader.mod.Entrypoint;
import com.coolspy3.csmodloader.mod.ModLoader;
import com.coolspy3.csmodloader.network.PacketHandler;
import com.coolspy3.csmodloader.network.ServerInstance;
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

        ServerInstance.init(accessToken, rsaKey);

        ArrayList<Entrypoint> mods = ModLoader.loadMods();

        if (mods == null) return;

        PacketHandler.setMods(mods);

        MainWindow.create();
    }
}
