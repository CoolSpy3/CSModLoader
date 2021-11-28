package com.coolspy3.csmodloader.util;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
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

public final class Utils
{

    public static final Charset UTF_8 = StandardCharsets.UTF_8;
    public static final Runnable DO_NOTHING = () -> {};

    public static Byte[] box(byte[] arr)
    {
        Byte[] out = new Byte[arr.length];

        for (int i = 0; i < arr.length; i++)
            out[i] = arr[i];

        return out;
    }

    public static void createAndWaitFor(Supplier<Frame> createFunc) throws InterruptedException
    {
        Object lock = new Object();

        SwingUtilities.invokeLater(() -> createFunc.get().addWindowListener(new WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent e)
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

    public static void executeTimeoutSync(Runnable func, long timeout, String taskName,
            Object... args) throws InterruptedException
    {
        executeTimeoutSync(() -> {

            func.run();

            return null;

        }, timeout, taskName, null, taskName, args);
    }

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
            System.err.println("Timed out while executing task: " + String.format(taskName, args));

        return (T) arr[0];
    }

    public static <T> T fromBytes(byte[] bytes, Function<ByteBuffer, T> convFunc)
    {
        return convFunc.apply(ByteBuffer.wrap(bytes));
    }

    public static <T> byte[] getBytes(T num, int length,
            BiFunction<ByteBuffer, T, ByteBuffer> convFunc)
    {
        return convFunc.apply(ByteBuffer.allocate(length), num).array();
    }

    public static String getStackTrace(Exception e)
    {
        ByteArrayOutputStream bais = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(bais, true));

        return new String(bais.toByteArray(), UTF_8);
    }

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

    public static void printHex(byte[] arr)
    {
        StringBuilder str = new StringBuilder("[");

        for (int i = 0; i < arr.length; i++)
        {
            str.append(String.format("%02x", arr[i]));
            if (i < arr.length - 1) str.append(", ");
        }

        System.out.println(str.append("]"));
    }

    // Credit: https://www.baeldung.com/java-random-string
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

    public static void reporting(ExceptionRunnable func)
    {
        try
        {
            func.run();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static <T, U> Function<T, U> reporting(ExceptionFunction<T, U> func)
    {
        return t -> Utils.reporting(() -> func.apply(t));
    }

    public static <T, U> Function<T, U> reporting(ExceptionFunction<T, U> func, U defaultValue)
    {
        return t -> Utils.reporting(() -> func.apply(t), defaultValue);
    }

    public static <T> T reporting(ExceptionSupplier<T> func)
    {
        return reporting(func, null);
    }

    public static <T> T reporting(ExceptionSupplier<T> func, T defaultValue)
    {
        try
        {
            return func.get();
        }
        catch (Exception e)
        {
            e.printStackTrace();

            return defaultValue;
        }
    }

    public static void safe(ExceptionRunnable func)
    {
        try
        {
            func.run();
        }
        catch (Exception e)
        {}
    }

    public static <T, U> Function<T, U> safe(ExceptionFunction<T, U> func)
    {
        return t -> Utils.safe(() -> func.apply(t));
    }

    public static <T, U> Function<T, U> safe(ExceptionFunction<T, U> func, U defaultValue)
    {
        return t -> Utils.safe(() -> func.apply(t), defaultValue);
    }

    public static <T> T safe(ExceptionSupplier<T> func)
    {
        return safe(func, null);
    }

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

    public static void safeCreateAndWaitFor(Supplier<Frame> createFunc)
    {
        safe(() -> createAndWaitFor(createFunc));
    }

    public static void safeExecuteTimeoutSync(Runnable func, long timeout, String taskName,
            Object... args)
    {
        safe(() -> executeTimeoutSync(func, timeout, taskName, args));
    }

    public static <T> T safeExecuteTimeoutSync(Supplier<T> func, long timeout, T defaultValue,
            String taskName, Object... args)
    {
        return safe(() -> executeTimeoutSync(func, timeout, defaultValue, taskName, args));
    }

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

            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    public static <T, U> Function<T, U> noFail(ExceptionFunction<T, U> func)
    {
        return t -> Utils.noFail(() -> func.apply(t));
    }

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

            e.printStackTrace(System.err);
            System.exit(1);

            return null;
        }
    }

    public static byte readByte(InputStream is) throws IOException
    {
        int i = is.read();
        if (i == -1) throw new EOFException();

        return (byte) i;
    }

    public static void writeByte(long b, OutputStream os) throws IOException
    {
        os.write((byte) b);
    }

    public static byte[] readBytes(InputStream is) throws IOException
    {
        return readNBytes(is, readVarInt(is));
    }

    public static void writeBytes(byte[] bytes, OutputStream os) throws IOException
    {
        writeVarInt(bytes.length, os);
        os.write(bytes);
    }

    public static String readString(InputStream is) throws IOException
    {
        return new String(readBytes(is), UTF_8);
    }

    public static void writeString(String str, OutputStream os) throws IOException
    {
        writeBytes(str.getBytes(UTF_8), os);
    }

    public static byte[] unbox(Byte[] arr)
    {
        byte[] out = new byte[arr.length];

        for (int i = 0; i < arr.length; i++)
            out[i] = arr[i];

        return out;
    }

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

    public static <T, U> Function<T, U> wrap(ExceptionFunction<T, U> func) throws WrapperException
    {
        return t -> Utils.wrap(() -> func.apply(t));
    }

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

    public static byte[] readNBytes(InputStream is, int len) throws IOException
    {
        byte[] buf = new byte[len];
        int nBytesRead = 0;

        while (nBytesRead < len)
            nBytesRead += is.read(buf, nBytesRead, len - nBytesRead);

        return buf;
    }

    // Credit: https://wiki.vg/index.php?title=Protocol&oldid=7368#With_compression
    public static int readVarInt(InputStream is) throws IOException
    {
        int value = 0;
        int bitOffset = 0;
        byte currentByte;

        do
        {
            if (bitOffset == 35) throw new RuntimeException("VarInt is too big");

            currentByte = readByte(is);
            // System.out.println(String.format("%02x", currentByte));
            value |= (currentByte & 0b01111111) << bitOffset;

            bitOffset += 7;
        }
        while ((currentByte & 0b10000000) != 0);

        return value;
    }

    public static long readVarLong(InputStream is) throws IOException
    {
        long value = 0;
        int bitOffset = 0;
        byte currentByte;

        do
        {
            if (bitOffset == 70) throw new RuntimeException("VarLong is too big");

            currentByte = readByte(is);
            value |= (currentByte & 0b01111111) << bitOffset;

            bitOffset += 7;
        }
        while ((currentByte & 0b10000000) != 0);

        return value;
    }

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

    public static void writeVarLong(long value, OutputStream os) throws IOException
    {
        while (true)
        {
            if ((value & 0xFFFFFFFFFFFFFF80L) == 0)
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
