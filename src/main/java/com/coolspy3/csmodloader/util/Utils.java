package com.coolspy3.csmodloader.util;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import com.coolspy3.csmodloader.gui.TextAreaFrame;
import com.coolspy3.csmodloader.interfaces.ExceptionFunction;
import com.coolspy3.csmodloader.interfaces.ExceptionRunnable;
import com.coolspy3.csmodloader.interfaces.ExceptionSupplier;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility functions
 */
public final class Utils
{

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    /**
     * A reference to {@link StandardCharsets#UTF_8}
     */
    public static final Charset UTF_8 = StandardCharsets.UTF_8;
    /**
     * A runnable which has no functionality ({@code () -> {}})
     */
    public static final Runnable DO_NOTHING = () -> {};

    /**
     * Boxes the given byte array
     *
     * @param arr The array to box
     *
     * @return The boxed array
     */
    public static Byte[] box(byte[] arr)
    {
        Byte[] out = new Byte[arr.length];

        for (int i = 0; i < arr.length; i++)
            out[i] = arr[i];

        return out;
    }

    /**
     * Creates a Frame and waits for the user to close it
     *
     * @param createFunc A function which will be invoked to create the frame.
     *
     * @throws InterruptedException if any thread interrupted the current thread before or while the
     *         current thread was waiting for a notification. The interrupted status of the current
     *         thread is cleared when this exception is thrown.
     */
    public static void createAndWaitFor(Supplier<Frame> createFunc) throws InterruptedException
    {
        Object lock = new Object();

        SwingUtilities.invokeLater(() -> createFunc.get().addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                synchronized (lock)
                {
                    lock.notifyAll();
                }
            }
        }));

        synchronized (lock)
        {
            lock.wait();
        }
    }

    /**
     * Executes the supplied function synchronously until it completes or a set duration passes,
     * after which, it switches to asynchronous execution.
     *
     * @param func The function to run
     * @param timeout The max amount of time to wait before switching to asynchronous execution in
     *        milliseconds
     * @param taskName A format string which can be used to construct the name of the task being
     *        executed (for logging purposes)
     * @param args Arguments which will be passed to the {@link String#format(String, Object...)}
     *        function when formatting the {@code taskName}
     *
     * @throws InterruptedException if any thread interrupted the current thread before or while the
     *         current thread was waiting for a notification. The interrupted status of the current
     *         thread is cleared when this exception is thrown.
     */
    public static void executeTimeoutSync(Runnable func, long timeout, String taskName,
            Object... args) throws InterruptedException
    {
        executeTimeoutSync(() -> {

            func.run();

            return null;

        }, timeout, taskName, null, taskName, args);
    }

    /**
     * Executes the supplied function synchronously until it completes or a set duration passes,
     * after which, it switches to asynchronous execution.
     *
     * @param <T> The return type of the function
     * @param func The function to run
     * @param timeout The max amount of time to wait before switching to asynchronous execution in
     *        milliseconds
     * @param defaultValue The value to return if the function times out while executing
     * @param taskName A format string which can be used to construct the name of the task being
     *        executed (for logging purposes)
     * @param args Arguments which will be passed to the {@link String#format(String, Object...)}
     *        function when formatting the {@code taskName}
     *
     * @return The result of the function or {@code defaultValue} if it switched to asynchronous
     *         execution
     *
     * @throws InterruptedException if any thread interrupted the current thread before or while the
     *         current thread was waiting for a notification. The interrupted status of the current
     *         thread is cleared when this exception is thrown.
     */
    @SuppressWarnings("unchecked")
    public static <T> T executeTimeoutSync(Supplier<T> func, long timeout, T defaultValue,
            String taskName, Object... args) throws InterruptedException
    {
        Object lock = new Object();
        AtomicBoolean isRunning = new AtomicBoolean(true);
        Object[] arr = new Object[] {defaultValue};

        Thread thread = new Thread(() -> {
            try
            {
                arr[0] = func.get();
            }
            finally
            {
                isRunning.set(false);
                synchronized (lock)
                {
                    lock.notifyAll();
                }
            }
        });

        thread.setDaemon(true);
        thread.start();

        synchronized (lock)
        {
            if (isRunning.get()) lock.wait(timeout);
        }

        if (isRunning.get())
            logger.warn("Timed out while executing task: " + String.format(taskName, args));

        return (T) arr[0];
    }

    /**
     * Deserializes an object from a byte array
     *
     * @param <T> The object type to deserialize
     * @param bytes The byte array
     * @param convFunc A function which parses the object from a ByteBuffer
     *
     * @return The parsed object
     */
    public static <T> T fromBytes(byte[] bytes, Function<ByteBuffer, T> convFunc)
    {
        return convFunc.apply(ByteBuffer.wrap(bytes));
    }

    /**
     * Serializes an object to a byte array
     *
     * @param <T> The object type to serialize
     * @param t The object
     * @param length The serialized length of the object
     * @param convFunc A function which serializes the object to the given ByteBuffer
     *
     * @return The serialized object as a byte array
     */
    public static <T> byte[] getBytes(T t, int length,
            BiFunction<ByteBuffer, T, ByteBuffer> convFunc)
    {
        return convFunc.apply(ByteBuffer.allocate(length), t).array();
    }

    /**
     * Retrieves the full stack trace of an exception as a string
     *
     * @param e The exception from which to retrieve the stack trace
     *
     * @return The stack trace
     */
    public static String getStackTrace(Exception e)
    {
        ByteArrayOutputStream bais = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(bais, true));

        return new String(bais.toByteArray(), UTF_8);
    }

    /**
     * Converts the specified byte array int hex
     *
     * @param arr The array to convert
     *
     * @return The hex string
     */
    public static String hexString(byte[] arr)
    {
        StringBuilder str = new StringBuilder("[");

        for (int i = 0; i < arr.length; i++)
        {
            str.append(String.format("%02x", arr[i]));
            if (i < arr.length - 1) str.append(", ");
        }

        return str.append("]").toString();
    }

    /**
     * Sends a HTTP POST request to the specified URL
     *
     * @param url The URL to which to send the request
     * @param payload The payload of the request. This will be converted to a string using
     *        {@link Gson#toJson(Object)}
     *
     * @return An Object array containing the response code from the server and the string
     *         representation of any additional data received from the server
     *
     * @throws IOException if an I/O error occurs
     */
    public static Object[] post(String url, Object payload) throws IOException
    {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setUseCaches(false);

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);

        byte[] requestBytes =
                (payload instanceof String ? (String) payload : new Gson().toJson(payload))
                        .getBytes(Utils.UTF_8);
        conn.setRequestProperty("Content-Length", Integer.toString(requestBytes.length));

        try (OutputStream connos = conn.getOutputStream())
        {
            connos.write(requestBytes);
            connos.flush();
        }

        int responseCode = conn.getResponseCode();

        try (InputStream is = conn.getInputStream())
        {
            return new Object[] {responseCode, IOUtils.toString(is, Utils.UTF_8)};
        }
    }

    // Credit: https://www.baeldung.com/java-random-string
    /**
     * Generates a random alpha-numeric String
     *
     * @param length The length of the resulting String
     *
     * @return The random String
     */
    public static String randomString(int length)
    {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;
        Random random = new Random();

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97)).limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return generatedString;
    }

    /**
     * Executes the provided function and logs if an error occurs
     *
     * @param func The function to execute
     */
    public static void reporting(ExceptionRunnable func)
    {
        try
        {
            func.run();
        }
        catch (Exception e)
        {
            logger.warn("Utils.reporting", e);
        }
    }

    /**
     * Creates a function which wraps all calls to the provided function in
     * {@link #reporting(ExceptionSupplier)}
     *
     * @param <T> The input type of the function
     * @param <U> The return type of the function
     * @param func The function to wrap
     *
     * @return The new function
     */
    public static <T, U> Function<T, U> reporting(ExceptionFunction<T, U> func)
    {
        return t -> Utils.reporting(() -> func.apply(t));
    }

    /**
     * Creates a function which wraps all calls to the provided function in
     * {@link #reporting(ExceptionSupplier, Object)}
     *
     * @param <T> The input type of the function
     * @param <U> The return type of the function
     * @param func The function to wrap
     * @param defaultValue The value to return in case of an exception
     *
     * @return The new function
     */
    public static <T, U> Function<T, U> reporting(ExceptionFunction<T, U> func, U defaultValue)
    {
        return t -> Utils.reporting(() -> func.apply(t), defaultValue);
    }

    /**
     * Executes the provided function and logs if an exception occurs
     *
     * @param <T> The return type of the function
     * @param func The function to execute
     *
     * @return The value returned by the function or {@code null} if an exception ocurred
     */
    public static <T> T reporting(ExceptionSupplier<T> func)
    {
        return reporting(func, null);
    }

    /**
     * Executes the provided function and logs if an exception occurs
     *
     * @param <T> The return type of the function
     * @param func The function to execute
     * @param defaultValue The value to return in case of an exception
     *
     * @return The value returned by the function or {@code defaultValue} if an exception ocurred
     */
    public static <T> T reporting(ExceptionSupplier<T> func, T defaultValue)
    {
        try
        {
            return func.get();
        }
        catch (Exception e)
        {
            logger.warn("Utils.reporting", e);

            return defaultValue;
        }
    }

    /**
     * Executes a function, ignoring any exceptions
     *
     * @param func The function to execute
     */
    public static void safe(ExceptionRunnable func)
    {
        try
        {
            func.run();
        }
        catch (Exception e)
        {}
    }

    /**
     * Creates a function which wraps all calls to the provided function in
     * {@link #safe(ExceptionSupplier)}
     *
     * @param <T> The input type of the function
     * @param <U> The return type of the function
     * @param func The function to wrap
     *
     * @return The new function
     */
    public static <T, U> Function<T, U> safe(ExceptionFunction<T, U> func)
    {
        return t -> Utils.safe(() -> func.apply(t));
    }

    /**
     * Creates a function which wraps all calls to the provided function in
     * {@link #safe(ExceptionSupplier, Object)}
     *
     * @param <T> The input type of the function
     * @param <U> The return type of the function
     * @param func The function to wrap
     * @param defaultValue The value to return in case of an exception
     *
     * @return The new function
     */
    public static <T, U> Function<T, U> safe(ExceptionFunction<T, U> func, U defaultValue)
    {
        return t -> Utils.safe(() -> func.apply(t), defaultValue);
    }

    /**
     * Executes a function, ignoring any exceptions
     *
     * @param <T> The return type of the function
     * @param func The function to execute
     *
     * @return The value returned by the function or {@code null} if an exception ocurred
     */
    public static <T> T safe(ExceptionSupplier<T> func)
    {
        return safe(func, null);
    }

    /**
     * Executes a function, ignoring any exceptions
     *
     * @param <T> The return type of the function
     * @param func The function to execute
     * @param defaultValue The value to return in case of an exception
     *
     * @return The value returned by the function or {@code defaultValue} if an exception ocurred
     */
    public static <T> T safe(ExceptionSupplier<T> func, T defaultValue)
    {
        try
        {
            return func.get();
        }
        catch (Exception e)
        {
            return defaultValue;
        }
    }

    /**
     * A convenience function for wrapping {@link #createAndWaitFor(Supplier)} in a call to
     * {@link #safe(ExceptionRunnable)}
     *
     * @param createFunc The function to use to create the window
     */
    public static void safeCreateAndWaitFor(Supplier<Frame> createFunc)
    {
        safe(() -> createAndWaitFor(createFunc));
    }

    /**
     * A convenience function for wrapping
     * {@link #executeTimeoutSync(Runnable, long, String, Object...)} in a call to
     * {@link #safe(ExceptionRunnable)}
     *
     * @param func The function to run
     * @param timeout The max amount of time to wait before switching to asynchronous execution in
     *        milliseconds
     * @param taskName A format string which can be used to construct the name of the task being
     *        executed (for logging purposes)
     * @param args Arguments which will be passed to the {@link String#format(String, Object...)}
     *        function when formatting the {@code taskName}
     */
    public static void safeExecuteTimeoutSync(Runnable func, long timeout, String taskName,
            Object... args)
    {
        safe(() -> executeTimeoutSync(func, timeout, taskName, args));
    }

    /**
     * A convenience function for wrapping
     * {@link #executeTimeoutSync(Supplier, long, Object, String, Object...)} in a call to
     * {@link #safe(ExceptionRunnable)}
     *
     * @param <T> The return type of the function
     * @param func The function to run
     * @param timeout The max amount of time to wait before switching to asynchronous execution in
     *        milliseconds
     * @param defaultValue The value to return if the function times out while executing
     * @param taskName A format string which can be used to construct the name of the task being
     *        executed (for logging purposes)
     * @param args Arguments which will be passed to the {@link String#format(String, Object...)}
     *        function when formatting the {@code taskName}
     * 
     * @return The result of the function or {@code defaultValue} if it switched to asynchronous
     *         execution
     */
    public static <T> T safeExecuteTimeoutSync(Supplier<T> func, long timeout, T defaultValue,
            String taskName, Object... args)
    {
        return safe(() -> executeTimeoutSync(func, timeout, defaultValue, taskName, args));
    }

    /**
     * Executes a function, assuming any exceptions to be fatal errors and terminating the program.
     * This should only be called when the occurrence of an exception should be impossible with the
     * assumed system configuration.
     *
     * @param func The function to execute
     */
    public static void noFail(ExceptionRunnable func)
    {
        try
        {
            func.run();
        }
        catch (Exception e)
        {
            safeCreateAndWaitFor(() -> new TextAreaFrame(
                    "If you're seeing this, you've got a problem: This error shouldn't be able to occur :/\nMost likely your system is incompatible with this program :(",
                    e));

            logger.error("Utils.noFail", e);
            System.exit(1);
        }
    }

    /**
     * Creates a function which wraps all calls to the provided function in
     * {@link #noFail(ExceptionSupplier)}
     *
     * @param <T> The input type of the function
     * @param <U> The return type of the function
     * @param func The function to wrap
     *
     * @return The new function
     */
    public static <T, U> Function<T, U> noFail(ExceptionFunction<T, U> func)
    {
        return t -> Utils.noFail(() -> func.apply(t));
    }

    /**
     * Executes a function, assuming any exceptions to be fatal errors and terminating the program.
     * This should only be called when the occurrence of an exception should be impossible with the
     * assumed system configuration.
     *
     * @param <T> The return type of the function
     * @param func The function to execute
     *
     * @return The value returned by the function
     */
    public static <T> T noFail(ExceptionSupplier<T> func)
    {
        try
        {
            return func.get();
        }
        catch (Exception e)
        {
            safeCreateAndWaitFor(() -> new TextAreaFrame(
                    "If you're seeing this, you've got a problem: This error shouldn't be able to occur :/\nMost likely your system is incompatible with this program :(",
                    e));

            logger.error("Utils.noFail", e);
            System.exit(1);

            return null;
        }
    }

    /**
     * Reads a single byte of data from an InputStream, throwing an {@link EOFException} of the end
     * of the stream is reached.
     *
     * @param is The stream from which to read
     *
     * @return The read byte
     *
     * @throws IOException If an I/O error occurs
     */
    public static byte readByte(InputStream is) throws IOException
    {
        int i = is.read();
        if (i == -1) throw new EOFException();

        return (byte) i;
    }

    /**
     * Writes a single byte of data to an OutputStream
     *
     * @param b THe byte to write
     * @param os The stream to which to write
     *
     * @throws IOException If an I/O error occurs
     */
    public static void writeByte(int b, OutputStream os) throws IOException
    {
        os.write(b & 0xFF);
    }

    /**
     * Reads a byte array prefixed with its length encoded as a VarInt from an InputStream.
     *
     * @param is The InputStream from which to read
     *
     * @return The read bytes
     *
     * @throws IOException If an I/O error occurs
     */
    public static byte[] readBytes(InputStream is) throws IOException
    {
        return readNBytes(is, readVarInt(is));
    }

    /**
     * Writes an array of bytes to an OutputStream after prefixing its length encoded as a VarInt
     *
     * @param bytes The bytes to write
     * @param os The OutputStream to which to write
     *
     * @throws IOException If an I/O error occurs
     */
    public static void writeBytes(byte[] bytes, OutputStream os) throws IOException
    {
        writeVarInt(bytes.length, os);
        os.write(bytes);
    }

    /**
     * Reads a UTF-8 encoded String prefixed with its length in bytes encoded as a VarInt from an
     * InputStream.
     *
     * @param is The InputStream from which to read
     *
     * @return The read String
     *
     * @throws IOException If an I/O error occurs
     */
    public static String readString(InputStream is) throws IOException
    {
        return new String(readBytes(is), UTF_8);
    }

    /**
     * Writes a String encoded in UTF-8 to an OutputStream after prefixing its length in bytes
     * encoded as a VarInt
     *
     * @param str The String to write
     * @param os The OutputStream to which to write
     *
     * @throws IOException If an I/O error occurs
     */
    public static void writeString(String str, OutputStream os) throws IOException
    {
        writeBytes(str.getBytes(UTF_8), os);
    }

    /**
     * Unboxes the given byte array
     *
     * @param arr The array to inbox
     *
     * @return The unboxed array
     */
    public static byte[] unbox(Byte[] arr)
    {
        byte[] out = new byte[arr.length];

        for (int i = 0; i < arr.length; i++)
            out[i] = arr[i];

        return out;
    }

    /**
     * Executes a function wrapping any thrown exceptions in a WrapperException
     *
     * @param func The function to execute
     *
     * @throws WrapperException If the function throws an exception
     */
    public static void wrap(ExceptionRunnable func) throws WrapperException
    {
        try
        {
            func.run();
        }
        catch (Exception e)
        {
            throw new WrapperException(e);
        }
    }

    /**
     * Creates a function which wraps all calls to the provided function in
     * {@link #wrap(ExceptionSupplier)}
     *
     * @param <T> The input type of the function
     * @param <U> The return type of the function
     * @param func The function to wrap
     *
     * @return The new function
     */
    public static <T, U> Function<T, U> wrap(ExceptionFunction<T, U> func) throws WrapperException
    {
        return t -> Utils.wrap(() -> func.apply(t));
    }

    /**
     * Executes a function wrapping any thrown exceptions in a WrapperException
     *
     * @param <T> The return type of the function
     * @param func The function to execute
     *
     * @return The value returned by the function
     *
     * @throws WrapperException If the function throws an exception
     */
    public static <T> T wrap(ExceptionSupplier<T> func) throws WrapperException
    {
        try
        {
            return func.get();
        }
        catch (Exception e)
        {
            throw new WrapperException(e);
        }
    }

    /**
     * Reads a specified number of bytes from an InputStream
     *
     * @param is The InputStream from which to read
     * @param len The number of bytes to read
     *
     * @return The read bytes
     *
     * @throws IOException If an I/O error occurs
     */
    public static byte[] readNBytes(InputStream is, int len) throws IOException
    {
        byte[] buf = new byte[len];
        int nBytesRead = 0;

        while (nBytesRead < len)
            nBytesRead += is.read(buf, nBytesRead, len - nBytesRead);

        return buf;
    }

    // Credit: https://wiki.vg/index.php?title=Protocol&oldid=7368#With_compression
    /**
     * Reads a variable length integer from an InputStream
     *
     * @param is The InputStream from which to read
     *
     * @return The read integer value
     *
     * @throws IOException If an I/O error occurs
     */
    public static int readVarInt(InputStream is) throws IOException
    {
        int value = 0;
        int bitOffset = 0;
        byte currentByte;

        do
        {
            if (bitOffset == 35) throw new IOException("VarInt is too big");

            currentByte = readByte(is);
            value |= (currentByte & 0b01111111) << bitOffset;

            bitOffset += 7;
        }
        while ((currentByte & 0b10000000) != 0);

        return value;
    }

    /**
     * Reads a variable length long from an InputStream
     *
     * @param is The InputStream from which to read
     *
     * @return The read long value
     *
     * @throws IOException If an I/O error occurs
     */
    public static long readVarLong(InputStream is) throws IOException
    {
        long value = 0;
        int bitOffset = 0;
        byte currentByte;

        do
        {
            if (bitOffset == 70) throw new IOException("VarLong is too big");

            currentByte = readByte(is);
            value |= (currentByte & 0b01111111) << bitOffset;

            bitOffset += 7;
        }
        while ((currentByte & 0b10000000) != 0);

        return value;
    }

    /**
     * Writes a variable length integer to an OutputStream
     *
     * @param os The OutputStream to which to write
     * @param value The value to write
     *
     * @throws IOException If an I/O error occurs
     */
    public static void writeVarInt(int value, OutputStream os) throws IOException
    {
        while (true)
        {
            if ((value & 0xFFFFFF80) == 0)
            {
                writeByte(value, os);

                return;
            }

            writeByte(value & 0x7F | 0x80, os);
            // Note: >>> means that the sign bit is shifted with the rest of the number
            // rather than being
            // left alone
            value >>>= 7;
        }
    }

    /**
     * Writes a variable length long to an OutputStream
     *
     * @param os The OutputStream to which to write
     * @param value The value to write
     *
     * @throws IOException If an I/O error occurs
     */
    public static void writeVarLong(long value, OutputStream os) throws IOException
    {
        while (true)
        {
            if ((value & 0xFFFFFFFFFFFFFF80L) == 0)
            {
                writeByte((int) (value & 0xFF), os);

                return;
            }

            writeByte((int) (value & 0x7F) | 0x80, os);
            // Note: >>> means that the sign bit is shifted with the rest of the number
            // rather than being
            // left alone
            value >>>= 7;
        }
    }

    /**
     * Calculates the encoded length of a variable length integer
     *
     * @param value The value to check
     *
     * @return The length of the value encoded as a variable length integer in bytes
     */
    public static int varIntLen(int value)
    {
        int len = 0;
        while (true)
        {
            if ((value & 0xFFFFFF80) == 0)
            {
                len++;

                return len;
            }

            len++;
            // Note: >>> means that the sign bit is shifted with the rest of the number
            // rather than being
            // left alone
            value >>>= 7;
        }
    }

    /**
     * Calculates the encoded length of a variable length long
     *
     * @param value The value to check
     *
     * @return The length of the value encoded as a variable length long in bytes
     */
    public static int varLongLen(long value)
    {
        int len = 0;
        while (true)
        {
            if ((value & 0xFFFFFFFFFFFFFF80L) == 0)
            {
                len++;

                return len;
            }

            len++;
            // Note: >>> means that the sign bit is shifted with the rest of the number
            // rather than being
            // left alone
            value >>>= 7;
        }
    }

    private Utils()
    {}
}
