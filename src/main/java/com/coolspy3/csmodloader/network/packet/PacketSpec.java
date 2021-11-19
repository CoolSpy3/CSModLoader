package com.coolspy3.csmodloader.network.packet;

import com.coolspy3.csmodloader.network.PacketDirection;

public @interface PacketSpec
{

    public Class<?>[] types();

    public PacketDirection direction();

}
