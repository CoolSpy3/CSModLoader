package com.coolspy3.csmodloader.interfaces;

import java.io.IOException;

public interface IOConsumer<T> {

    public void run(T t) throws IOException;

}
