package com.coolspy3.csmodloader.network;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.coolspy3.csmodloader.network.packet.Packet;

/**
 * Indicates that the method annotated by this annotation should be subscribed to the packet stream
 * when {@link PacketHandler#register(Object)} is called with an object or class containing it
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeToPacketStream
{

    /**
     * @return The packet types which should be fed to this function. These will only be fed if they
     *         can be accepted by the function.
     */
    public Class<? extends Packet>[] acceptedPacketTypes() default {Packet.class};

}
