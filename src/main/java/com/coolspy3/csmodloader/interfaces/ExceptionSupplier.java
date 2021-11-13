package com.coolspy3.csmodloader.interfaces;

@FunctionalInterface
public interface ExceptionSupplier<T>
{
    public T get() throws Exception;
}
