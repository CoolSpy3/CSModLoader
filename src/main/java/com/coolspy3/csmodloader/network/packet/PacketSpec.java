package com.coolspy3.csmodloader.network.packet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.coolspy3.csmodloader.network.PacketDirection;

/**
 * Defines general information about how a packet should be processed
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PacketSpec
{

    /**
     * @return The object types to use to serialize the data returned by {@link Packet#getValues()}
     *         during default serialization.
     */
    public Class<?>[] types();

    /**
     * @return The direction in which this packet is transmitted
     */
    public PacketDirection direction();

}
