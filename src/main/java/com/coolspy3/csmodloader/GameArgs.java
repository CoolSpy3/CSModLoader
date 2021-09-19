package com.coolspy3.csmodloader;

import java.io.File;
import java.util.UUID;

public class GameArgs {

    public final File gameDir;
    public final String username;
    public final UUID uuid;

    private static GameArgs args;

    public GameArgs(File gameDir, String username, UUID uuid) {
        this.gameDir = gameDir;
        this.username = username;
        this.uuid = uuid;
    }

    static void set(GameArgs args) {
        GameArgs.args = args;
    }

    void set() {
        set(this);
    }

    public static GameArgs get() {
        return args;
    }

}
