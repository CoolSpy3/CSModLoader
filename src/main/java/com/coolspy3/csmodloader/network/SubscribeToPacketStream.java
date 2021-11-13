package com.coolspy3.csmodloader.network;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.coolspy3.csmodloader.network.packet.Packet;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeToPacketStream
{

    public Class<? extends Packet>[] acceptedPacketTypes() default {Packet.class};

}
