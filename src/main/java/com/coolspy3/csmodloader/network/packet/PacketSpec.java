package com.coolspy3.csmodloader.network.packet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.coolspy3.csmodloader.network.PacketDirection;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PacketSpec
{

    public Class<?>[] types();

    public PacketDirection direction();

}
