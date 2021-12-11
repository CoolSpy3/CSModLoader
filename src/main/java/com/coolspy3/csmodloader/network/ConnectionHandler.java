package com.coolspy3.csmodloader.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the connection between a Minecraft client and server. An instance of this class handles
 * only one direction of traffic.
 */
public class ConnectionHandler implements Runnable
{

    private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);

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
    private boolean hasPacketHandler = false;

    /**
     * Creates a new ConnectionHandler
     *
     * @param iSocket The Socket from which to read
     * @param oSocket The Socket to which to write
     * @param serverIp The server's hostname. This will be sent to the server to verify you are
     *        connecting via. a valid endpoint
     * @param accessToken The player's access token
     * @param direction The PacketDirection handled by this ConnectionHandler
     * @param serverKey The KeyPair to use during initial authentication
     *
     * @throws IOException If an I/O error occurs
     */
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

    /**
     * Starts this ConnectionHandler's read loop in a new daemon thread
     */
    public void startInNewThread()
    {
        Thread thread = new Thread(this);

        thread.setDaemon(true);

        thread.start();
    }

    /**
     * Sets the handshaking state of this ConnectionHandler
     *
     * @param state The new state
     */
    private void setState(State state)
    {
        logger.debug("Switching state to {}...", state);
        this.state = state;
    }

    /**
     * Sets the compression threshold of this ConnectionHandler. Can be set to -1 to reset
     *
     * @param threshold The new threshold
     */
    public void setCompression(int threshold)
    {
        logger.trace("Setting compression threshold to {}...", threshold);
        this.compressionThreshhold = threshold;
    }

    /**
     * Preforms initial encryption setup
     *
     * @param serverId The serverId sent from the Minecraft server
     * @param key The public key provided by the Minecraft server
     */
    public void setupEncryption(String serverId, PublicKey key)
    {
        logger.trace("Setting up encryption...");
        this.serverId = serverId;
        this.serverPublicKey = key;
    }

    /**
     * Enables encryption on this ConnectionHandler.
     *
     * @param secretKey The secret key to use when reading from and writing to the packet stream
     */
    public void enableEncryption(SecretKey secretKey)
    {
        logger.trace("Enabling encryption...");
        this.key = secretKey;

        encCipher = Utils.noFail(() -> Cipher.getInstance("AES/CFB8/NoPadding"));
        decCipher = Utils.noFail(() -> Cipher.getInstance("AES/CFB8/NoPadding"));

        Utils.noFail(() -> encCipher.init(1, key, new IvParameterSpec(secretKey.getEncoded())));
        Utils.noFail(() -> decCipher.init(2, key, new IvParameterSpec(secretKey.getEncoded())));

        is = new BufferedInputStream(new CipherInputStream(is, decCipher));
        os = new BufferedOutputStream(new CipherOutputStream(os, encCipher));
    }

    /**
     * Writes the data contained in the provided stream to this ConnectionHandler's OutputStream
     * after prefixing its length.
     *
     * @param baos The stream to copy
     *
     * @throws IOException If an I/O error occurs
     */
    public void safeWrite(ByteArrayOutputStream baos) throws IOException
    {
        safeWrite(baos.toByteArray());
    }

    /**
     * Used to provide a reference to the ConnectionHandler responsible for the opposite direction
     * of traffic. This should be called before this handler's read loop is started.
     *
     * @param other The ConnectionHandler responsible for the opposite direction of traffic
     */
    private void setOther(ConnectionHandler other)
    {
        this.other = other;
    }

    /**
     * Sets the PacketHandler which will be used to process packets for this ConnectionHandler. This
     * should be called by this handler's read loop.
     *
     * @param packetHandler The PacketHandler which will be used to process packets for this
     *        ConnectionHandler
     */
    private void setPacketHandler(PacketHandler packetHandler)
    {
        this.packetHandler = packetHandler;
    }

    /**
     * Writes the specified data to this ConnectionHandler's OutputStream after prefixing its
     * length.
     *
     * @param data The data to write
     *
     * @throws IOException If an I/O error occurs
     */
    public void safeWrite(byte[] data) throws IOException
    {
        safeWrite(() -> {
            Utils.writeVarInt(data.length, os);
            os.write(data);
            os.flush();
        });
    }

    /**
     * Acquires the write lock for this ConnectionHandler's OutputStream and executes the specified
     * command.
     *
     * @param writeCommand The command to execute
     *
     * @throws IOException If an I/O error occurs
     */
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

        logger.debug("{} ConnectionHandler started!", direction);

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
            logger.error("Error Processing Connection!", e);

            Utils.safeCreateAndWaitFor(() -> new TextAreaFrame(direction.toString(), e));
        }
        finally
        {
            Utils.safe(iSocket::close);
            Utils.safe(oSocket::close);
            if (packetHandler != null) packetHandler.shutdown();
        }
    }

    /**
     * Attempts to read and process a packet from the input stream
     *
     * @throws DataFormatException If invalid compressed data is read
     * @throws IOException If an I/O error occurs
     */
    protected void readLoop() throws DataFormatException, IOException
    {
        if (!hasPacketHandler && packetHandler != null)
        {
            hasPacketHandler = true;
            packetHandler.linkToCurrentThread();
        }

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

            if (state != State.PLAY)
            {

                switch (packetId)
                {
                    case 0x00:
                    {
                        if (state != State.HANDSHAKE) break;

                        blockPacket = true;

                        int version = Utils.readVarInt(is);
                        @SuppressWarnings("unused")
                        String name = Utils.readString(is);
                        byte[] serverPort = Utils.readNBytes(is, 2);
                        int nextState = Utils.readVarInt(is);

                        switch (nextState)
                        {
                            case 2:
                                setState(State.LOGIN);
                                break;

                            case 1:
                            default:
                                setState(State.STATUS);
                                break;
                        }

                        other.setState(state);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        baos.write(0x00);
                        Utils.writeVarInt(version, baos);
                        Utils.writeString(serverIp, baos);
                        baos.write(serverPort);
                        Utils.writeVarInt(nextState, baos);

                        safeWrite(baos);

                        break;
                    }

                    case 0x01:
                    {
                        if (state != State.LOGIN) break;

                        switch (direction)
                        {
                            case CLIENTBOUND:
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

                                // Wait until the client enables encryption so that the next call to
                                // is.read()
                                // will be decrypted
                                command = () -> Utils.safe(() -> {
                                    synchronized (handler)
                                    {
                                        handler.wait();
                                    }
                                });

                                break;
                            }

                            case SERVERBOUND:
                            {
                                blockPacket = true;

                                Cipher cipher = Utils.noFail(() -> Cipher
                                        .getInstance(serverKey.getPrivate().getAlgorithm()));

                                Utils.noFail(() -> cipher.init(2, serverKey.getPrivate()));
                                byte[] sharedSecretEncrypted = Utils.readBytes(is);
                                byte[] sharedSecret =
                                        Utils.noFail(() -> cipher.doFinal(sharedSecretEncrypted));

                                Utils.noFail(() -> cipher.init(2, serverKey.getPrivate()));
                                byte[] verifyTokenEncrypted = Utils.readBytes(is);
                                byte[] verifyToken =
                                        Utils.noFail(() -> cipher.doFinal(verifyTokenEncrypted));

                                try
                                {
                                    McUtils.joinServerYggdrasil(accessToken,
                                            GameArgs.get().uuid.toString(), serverId,
                                            serverPublicKey, sharedSecret);
                                }
                                catch (IOException e)
                                {
                                    Utils.safeCreateAndWaitFor(() -> new TextAreaFrame(
                                            "Could not authenticate you with Mojang's servers! (Try restarting the program)",
                                            e));
                                }

                                other.enableEncryption(new SecretKeySpec(
                                        Arrays.copyOf(sharedSecret, sharedSecret.length), "AES"));
                                Cipher recipher = Utils.noFail(
                                        () -> Cipher.getInstance(serverPublicKey.getAlgorithm()));

                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                baos.write(0x01);
                                Utils.noFail(() -> recipher.init(1, serverPublicKey));
                                Utils.writeBytes(
                                        Utils.noFail(() -> recipher.doFinal(
                                                Arrays.copyOf(sharedSecret, sharedSecret.length))),
                                        baos);
                                Utils.noFail(() -> recipher.init(1, serverPublicKey));
                                Utils.writeBytes(Utils.noFail(() -> recipher.doFinal(verifyToken)),
                                        baos);

                                safeWrite(baos);

                                // Send Encryption Response and then enable encryption fo both
                                // listeners
                                command = () -> {
                                    enableEncryption(new SecretKeySpec(
                                            Arrays.copyOf(sharedSecret, sharedSecret.length),
                                            "AES"));
                                    synchronized (other)
                                    {
                                        other.notifyAll();
                                    }
                                };

                                break;
                            }

                            default:
                                break;
                        }

                        break;
                    }

                    case 0x02:
                    {
                        setPacketHandler(new PacketHandler());
                        other.setPacketHandler(packetHandler);

                        command = () -> setState(State.PLAY);
                        other.setState(State.PLAY);

                        break;
                    }

                    default:
                        break;
                }
            }

            if (packetId == 0x03)
            {
                int compressionThreshhold = Utils.readVarInt(is);
                other.setCompression(compressionThreshhold);

                command = () -> {
                    setCompression(compressionThreshhold);
                };
            }
        }
        else
        {
            int uncompressedLength = Utils.readVarInt(is);
            int dataLength = length - Utils.varIntLen(uncompressedLength);

            if (uncompressedLength == 0) packetData = Utils.readNBytes(is, dataLength);
            else
            {
                byte[] compressedPackedData = Utils.readNBytes(is, dataLength);

                decompressor.setInput(compressedPackedData);
                packetData = new byte[uncompressedLength];
                decompressor.inflate(packetData);

                decompressor.reset();
            }

            if (state == State.LOGIN
                    && Utils.readVarInt(new ByteArrayInputStream(packetData)) == 0x02)
            {
                setPacketHandler(new PacketHandler());
                other.setPacketHandler(packetHandler);

                command = () -> setState(State.PLAY);
                other.setState(State.PLAY);
            }
        }

        if (state == State.PLAY) Utils.safeExecuteTimeoutSync(
                () -> Utils.reporting(
                        () -> blockPacket = packetHandler.handleRawPacket(direction, packetData)),
                500, "PacketHandler.handlePacket(%s)",
                Utils.readVarInt(new ByteArrayInputStream(packetData)));

        is.reset();

        byte[] packet = Utils.readNBytes(is, length);

        if (!blockPacket)
        {
            safeWrite(packet);
        }

        command.run();
    }

    /**
     * Attempts to write a packet to this ConnectionHandler's OutputStream
     *
     * @param direction The direction in which to send the packet
     * @param packetId The id of the packet to send
     * @param writeFunc A function which, when called, will write the packet to the provided
     *        OutputStream
     *
     * @throws IOException If an I/O error occurs
     */
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

    /**
     * Attempts to write a packet to this ConnectionHandler's OutputStream
     *
     * @param direction The direction in which to send the packet
     * @param packetId The id of the packet to send
     * @param packetData A byte array containing the data to write to the OutputStream
     *
     * @throws IOException If an I/O error occurs
     */
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

    /**
     * Attempts to write a packet to this ConnectionHandler's OutputStream
     *
     * @param direction The direction in which to send the packet
     * @param packetData A byte array containing the data to write to the OutputStream
     *
     * @throws IOException If an I/O error occurs
     */
    public void write(PacketDirection direction, byte[] packetData) throws IOException
    {
        if (direction != this.direction)
        {
            other.write(direction, packetData);

            return;
        }

        if (compressionThreshhold == -1 || packetData.length < compressionThreshhold)
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            if (compressionThreshhold != -1) Utils.writeVarInt(0, baos);

            baos.write(packetData);

            safeWrite(baos);

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

    /**
     * Creates a pair of ConnectionHandlers, configures them to handle traffic between the supplied
     * sockets, and starts them using {@link #startInNewThread()}.
     *
     * @param client A Socket connected to the Minecraft client
     * @param server A Socket connected to the Minecraft server
     * @param host The server's hostname. This will be sent to the server to verify you are
     *        connecting via. a valid endpoint
     * @param accessToken The player's access token
     * @param direction The PacketDirection handled by this ConnectionHandler
     * @param key The KeyPair to use during initial authentication
     *
     * @return A Connection object which can be used to manage both handlers\
     *
     * @throws IOException If an I/O error occurs
     */
    static Connection start(Socket client, Socket server, String host, String accessToken,
            KeyPair key) throws IOException
    {
        logger.info("Starting ConnectionHandler to server: {}", host);

        ConnectionHandler c2s = new ConnectionHandler(client, server, host, accessToken,
                PacketDirection.SERVERBOUND, key);
        ConnectionHandler s2c = new ConnectionHandler(server, client, host, accessToken,
                PacketDirection.CLIENTBOUND, key);

        c2s.setOther(s2c);
        s2c.setOther(c2s);

        s2c.startInNewThread();
        c2s.startInNewThread();

        return new Connection(() -> {
            Utils.safe(client::close);
            Utils.safe(server::close);
        }, () -> !client.isClosed() || !server.isClosed());
    }

    /**
     * @return The ConnectionHandler responsible for handling traffic on this thread
     */
    public static ConnectionHandler getLocal()
    {
        return localHandler.get();
    }

    public static enum State
    {
        HANDSHAKE, STATUS, LOGIN, PLAY;
    }

}
