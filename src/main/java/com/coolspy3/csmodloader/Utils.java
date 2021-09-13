package com.coolspy3.csmodloader;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;

public final class Utils {

    public static final Charset UTF_8 = StandardCharsets.UTF_8;

    public static Object[] post(String url, Object payload) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setUseCaches(false);

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);

        byte[] requestBytes = (payload instanceof String ? (String)payload : new Gson().toJson(payload)).getBytes(Utils.UTF_8);
        conn.setRequestProperty("Content-Length", "" + requestBytes.length);

        try (OutputStream connos = conn.getOutputStream()) {
            connos.write(requestBytes);
            connos.flush();
        }

        int responseCode = conn.getResponseCode();
        try (InputStream is = conn.getInputStream()) {
            return new Object[] { responseCode, IOUtils.toString(is, Utils.UTF_8) };
        }
    }

    public static void printHex(byte[] arr) {
        String str = "[";
        for (int i = 0; i < arr.length; i++) {
            str += String.format("%02x", arr[i]);
            if (i < arr.length - 1) {
                str += ", ";
            }
        }
        System.out.println(str + "]");
    }

    public static void safe(ExceptionRunnable func) {
        try {
            func.run();
        } catch (Exception e) {
        }
    }

    public static <T> T safe(ExceptionSupplier<T> func) {
        return safe(func, null);
    }

    public static <T> T safe(ExceptionSupplier<T> func, T defaultValue) {
        try {
            return func.get();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static void noFail(ExceptionRunnable func) {
        try {
            func.run();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static <T> T noFail(ExceptionSupplier<T> func) {
        try {
            return func.get();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
            return null;
        }
    }

    public static byte readByte(InputStream is) throws IOException {
        int i = is.read();
        if (i == -1) {
            throw new EOFException();
        }
        return (byte) i;
    }

    public static void writeByte(OutputStream os, long b) throws IOException {
        os.write((byte) b);
    }

    public static byte[] readBytes(InputStream is) throws IOException {
        return readNBytes(is, readVarInt(is));
    }

    public static void writeBytes(OutputStream os, byte[] bytes) throws IOException {
        writeVarInt(os, bytes.length);
        os.write(bytes);
    }

    public static String readString(InputStream is) throws IOException {
        return new String(readBytes(is), UTF_8);
    }

    public static void writeString(OutputStream os, String str) throws IOException {
        writeBytes(os, str.getBytes(UTF_8));
    }

    @FunctionalInterface
    public static interface ExceptionRunnable {
        public void run() throws Exception;
    }

    @FunctionalInterface
    public static interface ExceptionSupplier<T> {
        public T get() throws Exception;
    }

    public static byte[] readNBytes(InputStream is, int len) throws IOException {
        byte[] buf = new byte[len];
        int nBytesRead = 0;
        while(nBytesRead < len) {
            nBytesRead += is.read(buf, nBytesRead, len - nBytesRead);
        }
        return buf;
    }

    // Credit: https://wiki.vg/index.php?title=Protocol&oldid=7368#With_compression
    public static int readVarInt(InputStream is) throws IOException {
        int value = 0;
        int bitOffset = 0;
        byte currentByte;
        do {
            if (bitOffset == 35)
                throw new RuntimeException("VarInt is too big");

            currentByte = readByte(is);
            // System.out.println(String.format("%02x", currentByte));
            value |= (currentByte & 0b01111111) << bitOffset;

            bitOffset += 7;
        } while ((currentByte & 0b10000000) != 0);

        return value;
    }

    public static long readVarLong(InputStream is) throws IOException {
        long value = 0;
        int bitOffset = 0;
        byte currentByte;
        do {
            if (bitOffset == 70)
                throw new RuntimeException("VarLong is too big");

            currentByte = readByte(is);
            value |= (currentByte & 0b01111111) << bitOffset;

            bitOffset += 7;
        } while ((currentByte & 0b10000000) != 0);

        return value;
    }

    public static void writeVarInt(OutputStream os, int value) throws IOException {
        while (true) {
            if ((value & 0xFFFFFF80) == 0) {
                writeByte(os, value);
                return;
            }

            writeByte(os, value & 0x7F | 0x80);
            // Note: >>> means that the sign bit is shifted with the rest of the number
            // rather than being
            // left alone
            value >>>= 7;
        }
    }

    public static void writeVarLong(OutputStream os, long value) throws IOException {
        while (true) {
            if ((value & 0xFFFFFFFFFFFFFF80L) == 0) {
                writeByte(os, value);
                return;
            }

            writeByte(os, value & 0x7F | 0x80);
            // Note: >>> means that the sign bit is shifted with the rest of the number
            // rather than being
            // left alone
            value >>>= 7;
        }
    }

    private Utils() {
    }
}
