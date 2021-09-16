package com.coolspy3.csmodloader;

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
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ConnectionHandler implements Runnable {

    private static final KeyFactory keyFactory = Utils.noFail(() -> KeyFactory.getInstance("RSA"));

    // Config Info
    private final Socket iSocket, oSocket;
    private InputStream is;
    private OutputStream os;
    private final String serverIp;
    private final PacketDirection direction;
    private final KeyPair serverKey;
    private final String accessToken;
    private Deflater compressor;
    private Inflater decompressor;

    // State Variables
    private int compressionThreshhold;
    private boolean blockPacket;
    private State state;

    // Variables which will be assigned as needed
    private Player player;
    private String serverId;
    private PublicKey serverPublicKey;
    private SecretKey key;
    private Cipher encCipher;
    private Cipher decCipher;
    private ConnectionHandler other;

    public ConnectionHandler(Socket iSocket, Socket oSocket, String serverIp, String accessToken, PacketDirection direction, KeyPair serverKey) throws IOException {
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
    }

    public void startInNewThread() {
        new Thread(this).start();
    }

    public void setCompression(int threshold) {
        this.compressionThreshhold = threshold;
    }

    void setOther(ConnectionHandler other) {
        this.other = other;
    }

    void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public void setupEncryption(String serverId, PublicKey key) {
        this.serverId = serverId;
        this.serverPublicKey = key;
    }

    public void enableEncryption(SecretKey secretKey) {
        this.key = secretKey;

        encCipher = Utils.noFail(() -> Cipher.getInstance("AES/CFB8/NoPadding"));
        decCipher = Utils.noFail(() -> Cipher.getInstance("AES/CFB8/NoPadding"));

        Utils.noFail(() -> encCipher.init(1, key, new IvParameterSpec(secretKey.getEncoded())));
        Utils.noFail(() -> decCipher.init(2, key, new IvParameterSpec(secretKey.getEncoded())));

        is = new BufferedInputStream(new CipherInputStream(is, decCipher));
        os = new BufferedOutputStream(new CipherOutputStream(os, encCipher));
    }

    public void blockPacket() {
        blockPacket = true;
    }

    @Override
    @SuppressWarnings({"UseSpecificCatch", "SynchronizeOnNonFinalField"})
    public void run() {
        try {
            boolean running = true;
            while(running) {
                try {
                    if(state == State.STATUS) {
                        byte[] buf = new byte[1024];
                        int nBytesRead = is.read(buf);
                        if(nBytesRead < 0) {
                            throw new EOFException();
                        }
                        os.write(buf, 0, nBytesRead);
                        os.flush();
                    } else {
                        readLoop();
                    }
                } catch(IOException e) {
                    try {
                        if(iSocket.isClosed() || oSocket.isClosed()) {
                            throw new EOFException();
                        }
                        throw e;
                    } catch(EOFException exc) {
                        running = false;
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace(System.err);
        } finally {
            Utils.safe(iSocket::close);
            Utils.safe(oSocket::close);
        }
    }

    protected void readLoop() throws DataFormatException, IOException {
        int length = Utils.readVarInt(is);
        is.mark(length);

        blockPacket = false;
        Runnable command = () -> {};

        byte[] packetData;
        if(compressionThreshhold == -1) {
            packetData = Utils.readNBytes(is, length);
            is.reset();
            is.mark(length);
            int packetId = Utils.readVarInt(is);
            if(packetId == 0x03 || packetId == 0x46) {
                setCompression(-1);
                other.setCompression(-1);
            }
            if(packetId == 0x00) {
                switch(state) {
                case HANDSHAKE:
                    blockPacket = true;

                    int version = Utils.readVarInt(is);
                    @SuppressWarnings("unused")
                    String name = Utils.readString(is);
                    byte[] serverPort = Utils.readNBytes(is, 2);
                    int nextState = Utils.readVarInt(is);

                    switch(nextState) {
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
                    Utils.writeVarInt(baos, version);
                    Utils.writeString(baos, serverIp);
                    baos.write(serverPort);
                    Utils.writeVarInt(baos, nextState);

                    Utils.writeVarInt(os, baos.size());
                    os.write(baos.toByteArray());
                    os.flush();
                    break;
                case PLAY:
                    // String username = Utils.readString(is);
                    // try {
                    //     player = new Player(username, MojangAPI.getUUID(username));
                    // } catch(APIException e) {
                    //     throw new IOException(e);
                    // }
                    break;
                case STATUS:
                default:
                    break;
                }
            }
            if(packetId == 0x01) {
                if(direction == PacketDirection.CLIENTBOUND) {
                    blockPacket = true;

                    String serverId = Utils.readString(is);
                    byte[] publicKeyEncoded = Utils.readBytes(is);
                    byte[] verifyToken = Utils.readBytes(is);

                    PublicKey publicKey = Utils.noFail(() -> keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyEncoded)));
                    setupEncryption(serverId, publicKey);
                    other.setupEncryption(serverId, publicKey);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    baos.write(0x01);
                    Utils.writeString(baos, "");
                    Utils.writeBytes(baos, serverKey.getPublic().getEncoded());
                    Utils.writeBytes(baos, verifyToken);

                    Utils.writeVarInt(os, baos.size());
                    os.write(baos.toByteArray());
                    os.flush();

                    ConnectionHandler handler = this;
                    // Wait until the client enables encryption so that the next call to is.wait() will be decrypted
                    command = () -> Utils.safe(() -> {synchronized(handler) { handler.wait(); }});
                } else if(direction == PacketDirection.SERVERBOUND) {
                    blockPacket = true;

                    byte[] sharedSecretEncrypted = Utils.readBytes(is);
                    Cipher cipher = Utils.noFail(() -> Cipher.getInstance(serverKey.getPrivate().getAlgorithm()));
                    Utils.noFail(() -> cipher.init(2, serverKey.getPrivate()));
                    byte[] sharedSecret = Utils.noFail(() -> cipher.doFinal(sharedSecretEncrypted));
                    Utils.noFail(() -> cipher.init(2, serverKey.getPrivate()));
                    byte[] verifyTokenEncrypted = Utils.readBytes(is);
                    byte[] verifyToken = Utils.noFail(() -> cipher.doFinal(verifyTokenEncrypted));

                    // String accessToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIwNjcyMTZkMjkwYzc1OTkxYWU1NDE0ZmMwM2JhMDA3NSIsInlnZ3QiOiJhYzUyNjEwZjk4Njc0M2FkOTQyMmFjN2JjNzEzMjAzOSIsInNwciI6Ijg2MmI3N2YyNjc0ZjQyYjQ5NmFmMWY5ODY0MjY2NjYyIiwiaXNzIjoiWWdnZHJhc2lsLUF1dGgiLCJleHAiOjE2MzE1MTc1NzIsImlhdCI6MTYzMTM0NDc3Mn0.b3SWtte4PfhRAZO-V8XRjq4WZkz7YAexI35DPpoNFS0";
                    // String accessToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIwNjcyMTZkMjkwYzc1OTkxYWU1NDE0ZmMwM2JhMDA3NSIsInlnZ3QiOiI0YmNlODg3ZDYyZmM0NjNiYTlmYjVmZTJiYWQyNmU5YiIsInNwciI6Ijg2MmI3N2YyNjc0ZjQyYjQ5NmFmMWY5ODY0MjY2NjYyIiwiaXNzIjoiWWdnZHJhc2lsLUF1dGgiLCJleHAiOjE2MzE1NzM4NjAsImlhdCI6MTYzMTQwMTA2MH0.o1Rzjr7LT_mVvSIaK5sUkqnRvhyA_7X8GyU0njQM5so";
                    // String accessToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIwNjcyMTZkMjkwYzc1OTkxYWU1NDE0ZmMwM2JhMDA3NSIsInlnZ3QiOiJhMjhiMzIxYjVlMGU0OTFkYTU2ZTUzMmVkYzYyMjliZSIsInNwciI6Ijg2MmI3N2YyNjc0ZjQyYjQ5NmFmMWY5ODY0MjY2NjYyIiwiaXNzIjoiWWdnZHJhc2lsLUF1dGgiLCJleHAiOjE2MzE2NTY1OTUsImlhdCI6MTYzMTQ4Mzc5NX0.NbE9JN5XwpQl7N8EvLppfrZimlFKAnRT9ElM084pE1A";
                    String selectedProfile = "862b77f2-674f-42b4-96af-1f9864266662";
                    McUtils.joinServerYggdrasil(accessToken, selectedProfile, serverId, serverPublicKey, sharedSecret);

                    other.enableEncryption(new SecretKeySpec(Arrays.copyOf(sharedSecret, sharedSecret.length), "AES"));
                    Cipher recipher = Utils.noFail(() -> Cipher.getInstance(serverPublicKey.getAlgorithm()));

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    baos.write(0x01);
                    Utils.noFail(() -> recipher.init(1, serverPublicKey));
                    Utils.writeBytes(baos, Utils.noFail(() -> recipher.doFinal(Arrays.copyOf(sharedSecret, sharedSecret.length))));
                    Utils.noFail(() -> recipher.init(1, serverPublicKey));
                    Utils.writeBytes(baos, Utils.noFail(() -> recipher.doFinal(verifyToken)));

                    Utils.writeVarInt(os, baos.size());
                    os.write(baos.toByteArray());
                    os.flush();

                    // Send Encryption Response and then enable encryption fo both listeners
                    command = () -> {
                        enableEncryption(new SecretKeySpec(Arrays.copyOf(sharedSecret, sharedSecret.length), "AES"));
                        synchronized(other) {
                            other.notifyAll();
                        }
                    };
                }
            }
        } else {
            int uncompressedLength = Utils.readVarInt(is);
            if(uncompressedLength == 0) {
                packetData = Utils.readNBytes(is, uncompressedLength);
            } else {
                int dataLength = length - Utils.varIntLen(uncompressedLength);
                byte[] compressedPackedData = Utils.readNBytes(is, dataLength);
                decompressor.setInput(compressedPackedData);
                packetData = new byte[uncompressedLength];
                decompressor.inflate(packetData);
                decompressor.reset();
            }
        }
        PacketHandler.handleRawPacket(this, packetData);
        is.reset();
        byte[] packet = Utils.readNBytes(is, length);
        if(blockPacket) {
            assert packet[0] == 0x00 || packet[0] == 0x01 : "" + packet[0];
        }
        if(!blockPacket) {
            Utils.writeVarInt(os, length);
            os.write(packet);
        }
        os.flush();
        command.run();
    }

    public static enum State {

        HANDSHAKE, STATUS, PLAY;

    }

}
