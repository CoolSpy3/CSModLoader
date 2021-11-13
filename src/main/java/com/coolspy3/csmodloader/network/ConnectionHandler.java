package com.coolspy3.csmodloader.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.coolspy3.csmodloader.GameArgs;
import com.coolspy3.csmodloader.gui.TextAreaFrame;
import com.coolspy3.csmodloader.interfaces.IOCommand;
import com.coolspy3.csmodloader.interfaces.IOConsumer;
import com.coolspy3.csmodloader.util.McUtils;
import com.coolspy3.csmodloader.util.Utils;

public class ConnectionHandler implements Runnable
{

    private static final KeyFactory keyFactory = Utils.noFail(() -> KeyFactory.getInstance("RSA"));

    private static final InheritableThreadLocal<ConnectionHandler> localHandler =
            new InheritableThreadLocal<>();

    // Config Info
    private final Socket iSocket, oSocket;

    private InputStream is;
    private OutputStream os;

    private final String serverIp;
    private final PacketDirection direction;

    private final KeyPair serverKey;
    private final String accessToken;

    private final Deflater compressor;
    private final Inflater decompressor;

    private final ReentrantLock socketLock;

    // State Variables
    private int compressionThreshhold;
    private boolean blockPacket;
    private State state;

    // Variables which will be assigned as needed
    private String serverId;

    private PublicKey serverPublicKey;
    private SecretKey key;

    private Cipher encCipher;
    private Cipher decCipher;

    private ConnectionHandler other;
    private PacketHandler packetHandler;

    public ConnectionHandler(Socket iSocket, Socket oSocket, String serverIp, String accessToken,
            PacketDirection direction, KeyPair serverKey) throws IOException
    {
        this.iSocket = iSocket;
        this.oSocket = oSocket;
        this.is = new BufferedInputStream(iSocket.getInputStream());
        this.os = new BufferedOutputStream(oSocket.getOutputStream());
        this.serverIp = serverIp;
        this.direction = direction;
        this.serverKey = serverKey;
        this.accessToken = accessToken;

        this.compressionThreshhold = -1;
        this.blockPacket = false;
        this.state = State.HANDSHAKE;

        this.compressor = new Deflater();
        this.decompressor = new Inflater();
        this.socketLock = new ReentrantLock();
    }

    public void startInNewThread()
    {
        new Thread(this).start();
    }

    public void setCompression(int threshold)
    {
        this.compressionThreshhold = threshold;
    }

    public void setupEncryption(String serverId, PublicKey key)
    {
        this.serverId = serverId;
        this.serverPublicKey = key;
    }

    public void enableEncryption(SecretKey secretKey)
    {
        this.key = secretKey;

        encCipher = Utils.noFail(() -> Cipher.getInstance("AES/CFB8/NoPadding"));
        decCipher = Utils.noFail(() -> Cipher.getInstance("AES/CFB8/NoPadding"));

        Utils.noFail(() -> encCipher.init(1, key, new IvParameterSpec(secretKey.getEncoded())));
        Utils.noFail(() -> decCipher.init(2, key, new IvParameterSpec(secretKey.getEncoded())));

        is = new BufferedInputStream(new CipherInputStream(is, decCipher));
        os = new BufferedOutputStream(new CipherOutputStream(os, encCipher));
    }

    void blockPacket()
    {
        blockPacket = true;
    }

    public void safeWrite(ByteArrayOutputStream baos) throws IOException
    {
        safeWrite(baos.toByteArray());
    }

    private void setOther(ConnectionHandler other)
    {
        this.other = other;
    }

    private void setPacketHandler(PacketHandler packetHandler)
    {
        this.packetHandler = packetHandler;
    }

    private void safeWrite(byte[] data) throws IOException
    {
        safeWrite(() -> {
            Utils.writeVarInt(data.length, os);
            os.write(data);
            os.flush();
        });
    }

    private void safeWrite(IOCommand writeCommand) throws IOException
    {
        socketLock.lock();
        try
        {
            writeCommand.run();
        }
        finally
        {
            socketLock.unlock();
        }
    }

    @Override
    @SuppressWarnings({"UseSpecificCatch", "SynchronizeOnNonFinalField"})
    public void run()
    {
        localHandler.set(this);
        packetHandler.linkToCurrentThread();

        try
        {
            boolean running = true;
            while (running)
            {
                try
                {
                    if (state == State.STATUS)
                    {
                        byte[] buf = new byte[1024];
                        int nBytesRead = is.read(buf);

                        if (nBytesRead < 0)
                        {
                            throw new EOFException();
                        }

                        safeWrite(() -> {
                            os.write(buf, 0, nBytesRead);
                            os.flush();
                        });
                    }
                    else
                        readLoop();
                }
                catch (IOException e)
                {
                    try
                    {
                        if (iSocket.isClosed() || oSocket.isClosed()) throw new EOFException();

                        throw e;
                    }
                    catch (EOFException exc)
                    {
                        running = false;
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            Utils.safeCreateAndWaitFor(() -> new TextAreaFrame(e));
        }
        finally
        {
            Utils.safe(iSocket::close);
            Utils.safe(oSocket::close);
        }
    }

    protected void readLoop() throws DataFormatException, IOException
    {
        int length = Utils.readVarInt(is);
        is.mark(length);

        blockPacket = false;
        Runnable command = Utils.DO_NOTHING;

        byte[] packetData;
        if (compressionThreshhold == -1)
        {
            packetData = Utils.readNBytes(is, length);
            is.reset();
            is.mark(length);

            int packetId = Utils.readVarInt(is);

            if (packetId == 0x03 || packetId == 0x46)
            {
                int compressionThreshhold = Utils.readVarInt(is);
                other.setCompression(compressionThreshhold);

                command = () -> {
                    setCompression(compressionThreshhold);
                };
            }

            if (packetId == 0x00)
            {
                switch (state)
                {
                    case HANDSHAKE:
                        blockPacket = true;

                        int version = Utils.readVarInt(is);
                        @SuppressWarnings("unused")
                        String name = Utils.readString(is);
                        byte[] serverPort = Utils.readNBytes(is, 2);
                        int nextState = Utils.readVarInt(is);

                        switch (nextState)
                        {
                            case 2:
                                state = State.PLAY;
                                break;

                            case 1:
                            default:
                                state = State.STATUS;
                                break;
                        }

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        baos.write(0x00);
                        Utils.writeVarInt(version, baos);
                        Utils.writeString(serverIp, baos);
                        baos.write(serverPort);
                        Utils.writeVarInt(nextState, baos);

                        safeWrite(baos);
                        break;

                    case PLAY:
                    case STATUS:
                    default:
                        break;
                }
            }

            if (packetId == 0x01)
            {
                if (direction == PacketDirection.CLIENTBOUND)
                {
                    blockPacket = true;

                    String serverId = Utils.readString(is);
                    byte[] publicKeyEncoded = Utils.readBytes(is);
                    byte[] verifyToken = Utils.readBytes(is);

                    PublicKey publicKey = Utils.noFail(() -> keyFactory
                            .generatePublic(new X509EncodedKeySpec(publicKeyEncoded)));
                    setupEncryption(serverId, publicKey);
                    other.setupEncryption(serverId, publicKey);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    baos.write(0x01);
                    Utils.writeString("", baos);
                    Utils.writeBytes(serverKey.getPublic().getEncoded(), baos);
                    Utils.writeBytes(verifyToken, baos);

                    safeWrite(baos);

                    ConnectionHandler handler = this;

                    // Wait until the client enables encryption so that the next call to is.() will
                    // be decrypted
                    command = () -> Utils.safe(() -> {
                        synchronized (handler)
                        {
                            handler.wait();
                        }
                    });
                }
                else if (direction == PacketDirection.SERVERBOUND)
                {
                    blockPacket = true;

                    Cipher cipher = Utils.noFail(
                            () -> Cipher.getInstance(serverKey.getPrivate().getAlgorithm()));

                    Utils.noFail(() -> cipher.init(2, serverKey.getPrivate()));
                    byte[] sharedSecretEncrypted = Utils.readBytes(is);
                    byte[] sharedSecret = Utils.noFail(() -> cipher.doFinal(sharedSecretEncrypted));

                    Utils.noFail(() -> cipher.init(2, serverKey.getPrivate()));
                    byte[] verifyTokenEncrypted = Utils.readBytes(is);
                    byte[] verifyToken = Utils.noFail(() -> cipher.doFinal(verifyTokenEncrypted));

                    try
                    {
                        McUtils.joinServerYggdrasil(accessToken, GameArgs.get().uuid.toString(),
                                serverId, serverPublicKey, sharedSecret);
                    }
                    catch (IOException e)
                    {
                        Utils.safeCreateAndWaitFor(() -> new TextAreaFrame(
                                "Could not authenticate you with Mojang's servers! (Try restarting the program)",
                                e));
                    }

                    other.enableEncryption(new SecretKeySpec(
                            Arrays.copyOf(sharedSecret, sharedSecret.length), "AES"));
                    Cipher recipher =
                            Utils.noFail(() -> Cipher.getInstance(serverPublicKey.getAlgorithm()));

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    baos.write(0x01);
                    Utils.noFail(() -> recipher.init(1, serverPublicKey));
                    Utils.writeBytes(
                            Utils.noFail(() -> recipher
                                    .doFinal(Arrays.copyOf(sharedSecret, sharedSecret.length))),
                            baos);
                    Utils.noFail(() -> recipher.init(1, serverPublicKey));
                    Utils.writeBytes(Utils.noFail(() -> recipher.doFinal(verifyToken)), baos);

                    safeWrite(baos);

                    // Send Encryption Response and then enable encryption fo both listeners
                    command = () -> {
                        enableEncryption(new SecretKeySpec(
                                Arrays.copyOf(sharedSecret, sharedSecret.length), "AES"));
                        synchronized (other)
                        {
                            other.notifyAll();
                        }
                    };
                }
            }
        }
        else
        {
            int uncompressedLength = Utils.readVarInt(is);

            if (uncompressedLength == 0) packetData = Utils.readNBytes(is, uncompressedLength);
            else
            {
                int dataLength = length - Utils.varIntLen(uncompressedLength);
                byte[] compressedPackedData = Utils.readNBytes(is, dataLength);

                decompressor.setInput(compressedPackedData);
                packetData = new byte[uncompressedLength];
                decompressor.inflate(packetData);

                decompressor.reset();
            }
        }

        if (key != null && command == Utils.DO_NOTHING)
            packetHandler.handleRawPacket(this, packetData);

        is.reset();

        byte[] packet = Utils.readNBytes(is, length);

        if (!blockPacket)
        {
            safeWrite(packet);
        }

        command.run();
    }

    public void write(PacketDirection direction, int packetId, IOConsumer<OutputStream> writeFunc)
            throws IOException
    {
        if (direction != this.direction)
        {
            other.write(direction, packetId, writeFunc);

            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        writeFunc.run(baos);

        write(direction, packetId, baos.toByteArray());
    }

    public void write(PacketDirection direction, int packetId, byte[] packetData) throws IOException
    {
        if (direction != this.direction)
        {
            other.write(direction, packetId, packetData);

            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Utils.writeVarInt(packetId, baos);
        baos.write(packetData);

        write(direction, baos.toByteArray());
    }

    public void write(PacketDirection direction, byte[] packetData) throws IOException
    {
        if (direction != this.direction)
        {
            other.write(direction, packetData);

            return;
        }

        if (compressionThreshhold == -1 || packetData.length < compressionThreshhold)
        {
            safeWrite(() -> {
                Utils.writeVarInt(packetData.length, os);

                if (compressionThreshhold != -1) Utils.writeVarInt(0, os);

                os.write(packetData);

                os.flush();
            });

            return;
        }

        int uncompressedLength = packetData.length;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Utils.writeVarInt(uncompressedLength, baos);

        compressor.setInput(packetData);
        compressor.finish();

        byte[] buf = new byte[1024];

        while (!compressor.finished())
        {
            int numCompressedBytes = compressor.deflate(buf);

            baos.write(buf, 0, numCompressedBytes);
        }

        safeWrite(baos);
    }

    static Connection start(Socket client, Socket server, String host, String accessToken,
            KeyPair key) throws IOException
    {
        ConnectionHandler c2s = new ConnectionHandler(client, server, host, accessToken,
                PacketDirection.SERVERBOUND, key);
        ConnectionHandler s2c = new ConnectionHandler(server, client, host, accessToken,
                PacketDirection.CLIENTBOUND, key);

        c2s.setOther(s2c);
        s2c.setOther(c2s);

        PacketHandler packetHandler = new PacketHandler();
        c2s.setPacketHandler(packetHandler);
        s2c.setPacketHandler(packetHandler);

        s2c.startInNewThread();
        c2s.startInNewThread();

        return new Connection(() -> {
            Utils.safe(client::close);
            Utils.safe(server::close);
        }, () -> !client.isClosed() || !server.isClosed());
    }

    public static ConnectionHandler getLocal()
    {
        return localHandler.get();
    }

    public static enum State
    {
        HANDSHAKE, STATUS, PLAY;
    }

}
