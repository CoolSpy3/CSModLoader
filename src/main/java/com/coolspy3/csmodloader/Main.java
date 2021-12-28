package com.coolspy3.csmodloader;

import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
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
import com.coolspy3.csmodloader.mod.Mod;
import com.coolspy3.csmodloader.mod.ModLoader;
import com.coolspy3.csmodloader.network.PacketHandler;
import com.coolspy3.csmodloader.network.ServerInstance;
import com.coolspy3.csmodloader.util.Utils;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main class
 */
@Mod(id = "csmodloader", name = "CSModLoader", version = "1.3.0", description = "The mod loader")
public class Main
{

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static boolean init = false;

    /**
     * The entrypoint to the program
     *
     * @param args The command-line arguments
     */
    public static void main(String[] args)
    {
        synchronized (Main.class)
        {
            if (init) return;

            init = true;
        }

        StatusPrinter.print((LoggerContext) LoggerFactory.getILoggerFactory());

        logger.info("Program Started!");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> logger.info("Program Exiting...")));

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException(Thread t, Throwable e)
            {
                logger.error("Uncaught Exception in Thread: {}", t.getName(), e);
            }
        });

        String accessToken = null;
        String gameDir = null;
        String username = null;
        UUID uuid = null;

        {
            Iterator<String> argIttr = Arrays.asList(args).iterator();
            while (argIttr.hasNext())
            {
                String arg = argIttr.next().toLowerCase();

                if (!argIttr.hasNext()) break;

                switch (arg)
                {
                    case "--accesstoken":
                        accessToken = argIttr.next();
                        logger.info("Read Argument \"Access Token\": {}",
                                accessToken.replaceAll(".", "*"));
                        break;

                    case "--gamedir":
                        gameDir = argIttr.next();
                        logger.info("Read Argument \"Game Directory\": {}", gameDir);
                        break;

                    case "--username":
                        username = argIttr.next();
                        logger.info("Read Argument \"Username\": {}", username);
                        break;

                    case "--uuid":
                        String uuidString = argIttr.next();

                        logger.info("Read Argument \"UUID\": {}", uuidString);

                        uuid = UUID.fromString(uuidString.substring(0, 8) + "-"
                                + uuidString.substring(8, 12) + "-" + uuidString.substring(12, 16)
                                + "-" + uuidString.substring(16, 20) + "-"
                                + uuidString.substring(20));
                        break;

                    default:
                        logger.warn("Unknown Argument: {}", arg);
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
            logger.error("Failed to load config file!", e);

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

        logger.debug("Initialization Complete!");

        ArrayList<Entrypoint> mods = ModLoader.loadMods();

        if (mods == null) System.exit(1);

        PacketHandler.setMods(mods);

        logger.debug("Starting GUI...");

        ServerInstance.restartServers();

        MainWindow.create();
    }
}
