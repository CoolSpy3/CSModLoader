package com.coolspy3.csmodloader.interfaces;

import java.io.IOException;

public interface IOBiConsumer<T, U> {

    public void run(T t, U u) throws IOException;

}
