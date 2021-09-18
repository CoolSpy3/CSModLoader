package com.coolspy3.csmodloader;

import java.io.IOException;

public interface IOConsumer<T> {

    public void run(T t) throws IOException;

}
