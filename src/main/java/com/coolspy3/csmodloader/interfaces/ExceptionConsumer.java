package com.coolspy3.csmodloader.interfaces;

import java.util.function.Consumer;

/**
 * Represents an implementation of {@link Consumer} which throws an optional Exception
 *
 * @param <T> the type of the input to the operation
 */
@FunctionalInterface
public interface ExceptionConsumer<T>
{
    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     *
     * @throws Exception if an Exception occurs
     */
    public void accept(T t) throws Exception;
}
