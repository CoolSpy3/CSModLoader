package com.coolspy3.csmodloader;

import java.io.File;
import java.util.UUID;

/**
 * A data structure for storing the arguments passed through the command line
 */
public class GameArgs
{

    /**
     * The game directory
     */
    public final File gameDir;
    /**
     * The player's username
     */
    public final String username;
    /**
     * The player's UUID
     */
    public final UUID uuid;

    /**
     * The global game arguments
     */
    private static GameArgs args;

    public GameArgs(File gameDir, String username, UUID uuid)
    {
        this.gameDir = gameDir;
        this.username = username;
        this.uuid = uuid;
    }

    /**
     * Sets the global game arguments
     *
     * @param args The new arguments
     */
    static void set(GameArgs args)
    {
        GameArgs.args = args;
    }

    /**
     * Sets these game arguments to be the global game arguments
     */
    void set()
    {
        set(this);
    }

    /**
     * @return The global game arguments
     */
    public static GameArgs get()
    {
        return args;
    }

}
