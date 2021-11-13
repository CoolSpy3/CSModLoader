package com.coolspy3.csmodloader.interfaces;

@FunctionalInterface
public interface ExceptionFunction<T, U>
{
    public U apply(T t) throws Exception;
}
