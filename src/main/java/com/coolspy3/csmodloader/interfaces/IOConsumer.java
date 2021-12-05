package com.coolspy3.csmodloader.interfaces;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Represents an implementation of {@link Consumer} which throws an optional Exception
 *
 * @param <T> the type of the input to the operation
 */
@FunctionalInterface
public interface IOConsumer<T>
{

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     *
     * @throws IOException if an I/O exception occurs.
     */
    public void run(T t) throws IOException;

}
