package com.coolspy3.csmodloader.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;

import com.coolspy3.csmodloader.Utils;
import com.coolspy3.csmodloader.interfaces.IOBiConsumer;
import com.coolspy3.csmodloader.interfaces.IOFunction;

public interface ObjectParser<T> {

    public static <T, U> ObjectParser<U> mapping(ObjectParser<T> parser, Function<U, T> encMapper, Function<T, U> decMapper, Class<U> type) {
        return new ObjectParser<U>() {

            @Override
            public U decode(InputStream is) throws IOException {
                return decMapper.apply(parser.decode(is));
            }

            @Override
            public void encode(U obj, OutputStream os) throws IOException {
                parser.encode(encMapper.apply(obj), os);
            }

            @Override
            public Class<U> getType() {
                return type;
            }
        };
    }

    public static <T> ObjectParser<T> of(Function<T, Byte[]> encFunc, IOFunction<InputStream, T> decFunc, Class<T> type) {
        return new ObjectParser<T>() {
            @Override
            public T decode(InputStream is) throws IOException {
                return decFunc.run(is);
            }

            @Override
            public void encode(T obj, OutputStream os) throws IOException {
                os.write(Utils.unbox(encFunc.apply(obj)));
            }

            @Override
            public Class<?> getType() {
                return type;
            }
        };
    }

    public static <T> ObjectParser<T> of(Function<T, Byte[]> encFunc, Function<Byte[], T> decFunc, int length, Class<T> type) {
        return new ObjectParser<T>() {
            @Override
            public T decode(InputStream is) throws IOException {
                return decFunc.apply(Utils.box(Utils.readNBytes(is, length)));
            }

            @Override
            public void encode(T obj, OutputStream os) throws IOException {
                os.write(Utils.unbox(encFunc.apply(obj)));
            }

            @Override
            public Class<?> getType() {
                return type;
            }
        };
    }

    public static <T> ObjectParser<T> of(IOBiConsumer<T, OutputStream> encFunc, IOFunction<InputStream, T> decFunc, Class<T> type) {
        return new ObjectParser<T>() {
            @Override
            public T decode(InputStream is) throws IOException {
                return decFunc.run(is);
            }

            @Override
            public void encode(T obj, OutputStream os) throws IOException {
                encFunc.run(obj, os);
            }

            @Override
            public Class<?> getType() {
                return type;
            }
        };
    }

    public static <T, U extends WrapperType<T>> ObjectParser<T> wrapping(ObjectParser<T> parser, Class<U> type) {
        return new ObjectParser<T>() {

            @Override
            public T decode(InputStream is) throws IOException {
                return parser.decode(is);
            }

            @Override
            public void encode(T obj, OutputStream os) throws IOException {
                parser.encode(obj, os);
            }

            @Override
            public Class<?> getType() {
                return type;
            }
        };
    }

    public T decode(InputStream is) throws IOException;
    public void encode(T obj, OutputStream os) throws IOException;
    public Class<?> getType();

    @SuppressWarnings("unchecked")
    public default void encodeObject(Object obj, OutputStream os) throws IOException {
        encode((T)obj, os);
    }

}
