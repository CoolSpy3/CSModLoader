package com.coolspy3.csmodloader.mod;

import com.coolspy3.csmodloader.network.ConnectionHandler;
import com.coolspy3.csmodloader.network.PacketHandler;

/**
 * Represents the entrypoint to a mod. This will be initialized when the mod is loaded. All
 * entrypoints must implement a constructor which takes no arguments.
 */
public interface Entrypoint
{

    /**
     * Creates a new Entrypoint which will be used to call {@link #init(PacketHandler)}. This
     * default implementation returns <code>this</code>.
     *
     * @return The entrypoint on which to call {@link #init(PacketHandler)}.
     */
    public default Entrypoint create()
    {
        return this;
    }

    /**
     * Preforms optional initialization for this mod. Implementing mods should use this to make one
     * or more calls to <code>PacketHandler.register</code>.
     *
     * NOTE: This is called before the connection to the server is setup, so
     * {@link ConnectionHandler#getLocal()} will return <code>null</code>
     *
     * @param handler The PacketHandler which will be assigned to this instance of the mod.
     */
    public default void init(PacketHandler handler)
    {}

}
