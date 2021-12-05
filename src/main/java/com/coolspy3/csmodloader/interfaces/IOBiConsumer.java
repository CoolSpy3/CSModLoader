package com.coolspy3.csmodloader.interfaces;

import java.io.IOException;
import java.util.function.BiConsumer;

/**
 * Represents an implementation of {@link BiConsumer} which handles an I/O operation
 *
 * @param <T> the type of the first argument to the operation
 * @param <U> the type of the second argument to the operation
 */
@FunctionalInterface
public interface IOBiConsumer<T, U>
{

    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     *
     * @throws IOException if an I/O exception occurs.
     */
    public void run(T t, U u) throws IOException;

}
