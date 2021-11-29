package com.coolspy3.csmodloader.network.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;

import com.coolspy3.csmodloader.interfaces.IOBiConsumer;
import com.coolspy3.csmodloader.interfaces.IOFunction;
import com.coolspy3.csmodloader.util.Utils;

/**
 * A class containing code to serialize and deserialize an object
 *
 * @param <T> The type of object which can be serialized
 */
public interface ObjectParser<T>
{

    /**
     * Creates a new ObjectParser which reinterprets an existing parser's type to form a new type.
     *
     * @param <T> The type of the original parser
     * @param <U> The new type to be parsed
     * @param parser The original parser
     * @param encMapper A function converting from the new type to the type of the original parser
     * @param decMapper A function converting from the type of the original parser to the new type
     * @param type The new type
     *
     * @return The new parser
     */
    public static <T, U> ObjectParser<U> mapping(ObjectParser<T> parser, Function<U, T> encMapper,
            Function<T, U> decMapper, Class<U> type)
    {
        return new ObjectParser<U>()
        {

            @Override
            public U decode(InputStream is) throws IOException
            {
                return decMapper.apply(parser.decode(is));
            }

            @Override
            public void encode(U obj, OutputStream os) throws IOException
            {
                parser.encode(encMapper.apply(obj), os);
            }

            @Override
            public Class<U> getType()
            {
                return type;
            }
        };
    }

    /**
     * Creates a new object parser which deserializes from an InputStream but serializes to a byte
     * array
     *
     * @param <T> The type of object serialized by this ObjectParser
     * @param encFunc The function used to serialize an object
     * @param decFunc The function used to deserialize an object
     * @param type The class type serialized by this ObjectParser
     *
     * @return The new parser
     */
    public static <T> ObjectParser<T> of(Function<T, Byte[]> encFunc,
            IOFunction<InputStream, T> decFunc, Class<T> type)
    {
        return new ObjectParser<T>()
        {
            @Override
            public T decode(InputStream is) throws IOException
            {
                return decFunc.run(is);
            }

            @Override
            public void encode(T obj, OutputStream os) throws IOException
            {
                os.write(Utils.unbox(encFunc.apply(obj)));
            }

            @Override
            public Class<?> getType()
            {
                return type;
            }
        };
    }

    /**
     * Creates a new object parser which serializes and deserializes a known number of bytes for
     * each object.
     *
     * @param <T> The type of object serialized by this ObjectParser
     * @param encFunc The function used to serialize an object
     * @param decFunc The function used to deserialize an object
     * @param length The fixed number of bytes used to encode each object
     * @param type The class type serialized by this ObjectParser
     *
     * @return The new parser
     */
    public static <T> ObjectParser<T> of(Function<T, Byte[]> encFunc, Function<Byte[], T> decFunc,
            int length, Class<T> type)
    {
        return new ObjectParser<T>()
        {
            @Override
            public T decode(InputStream is) throws IOException
            {
                return decFunc.apply(Utils.box(Utils.readNBytes(is, length)));
            }

            @Override
            public void encode(T obj, OutputStream os) throws IOException
            {
                os.write(Utils.unbox(encFunc.apply(obj)));
            }

            @Override
            public Class<?> getType()
            {
                return type;
            }
        };
    }

    /**
     * Creates a new object parser which serializes and deserializes using I/O streams
     *
     * @param <T> The type of object serialized by this ObjectParser
     * @param encFunc The function used to serialize an object
     * @param decFunc The function used to deserialize an object
     * @param type The class type serialized by this ObjectParser
     *
     * @return The new parser
     */
    public static <T> ObjectParser<T> of(IOBiConsumer<T, OutputStream> encFunc,
            IOFunction<InputStream, T> decFunc, Class<T> type)
    {
        return new ObjectParser<T>()
        {
            @Override
            public T decode(InputStream is) throws IOException
            {
                return decFunc.run(is);
            }

            @Override
            public void encode(T obj, OutputStream os) throws IOException
            {
                encFunc.run(obj, os);
            }

            @Override
            public Class<?> getType()
            {
                return type;
            }
        };
    }

    /**
     * Creates a new object parser which serializes a functionally equivalent type to an existing
     * parser.
     *
     * @param <T> The type of the original parser
     * @param <U> The new type of object which will be serialized by this parser
     * @param parser The original parser
     * @param type The class type serialized by this ObjectParser
     *
     * @return The new parser
     *
     * @see WrapperType
     */
    public static <T, U extends WrapperType<T>> ObjectParser<T> wrapping(ObjectParser<T> parser,
            Class<U> type)
    {
        return new ObjectParser<T>()
        {

            @Override
            public T decode(InputStream is) throws IOException
            {
                return parser.decode(is);
            }

            @Override
            public void encode(T obj, OutputStream os) throws IOException
            {
                parser.encode(obj, os);
            }

            @Override
            public Class<?> getType()
            {
                return type;
            }
        };
    }

    /**
     * Decodes an object from the provided InputStream
     *
     * @param is The stream from which to decode
     * @return The resulting object
     *
     * @throws IOException If an I/O error occurs
     */
    public T decode(InputStream is) throws IOException;

    /**
     * Encodes an object to the provided OutputStream
     *
     * @param obj The object to encode
     * @param os The OutputStream onto which to write the encoded object
     *
     * @throws IOException If an I/O error occurs
     */
    public void encode(T obj, OutputStream os) throws IOException;

    /**
     * @return The class type serialized by this ObjectParser
     */
    public Class<?> getType();

    /**
     * Attempts to encode the given object onto the provided output stream
     *
     * @param obj The object to encode
     * @param os The OutputStream onto which to write the encoded object
     *
     * @throws ClassCastException If the provided object cannot be encoded by this ObjectParser
     * @throws IOException If an I/O error occurs
     */
    @SuppressWarnings("unchecked")
    public default void encodeObject(Object obj, OutputStream os)
            throws ClassCastException, IOException
    {
        encode((T) obj, os);
    }

}
