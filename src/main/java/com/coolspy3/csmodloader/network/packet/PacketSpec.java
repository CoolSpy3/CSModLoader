package com.coolspy3.csmodloader.network.packet;

import com.coolspy3.csmodloader.network.PacketDirection;

public @interface PacketSpec
{

    public Class<?>[] argTypes();

    public PacketDirection getDirection();

}
