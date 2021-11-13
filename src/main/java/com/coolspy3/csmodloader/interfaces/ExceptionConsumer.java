package com.coolspy3.csmodloader.interfaces;

@FunctionalInterface
public interface ExceptionConsumer<T>
{
    public void accept(T t) throws Exception;
}
