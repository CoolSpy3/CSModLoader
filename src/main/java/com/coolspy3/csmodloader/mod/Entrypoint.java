package com.coolspy3.csmodloader.mod;

import com.coolspy3.csmodloader.network.PacketHandler;

public interface Entrypoint
{

    public default Entrypoint create()
    {
        return this;
    }

    public default void init(PacketHandler handler)
    {}

}
