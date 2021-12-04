package com.coolspy3.csmodloader.util;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;

/**
 * Utility functions which deal directly with Minecraft behaviors
 */
public final class McUtils
{

    /**
     * Uses the Yggdrasil protocol to simulate a Minecraft client joining the specified server
     *
     * @param accessToken The player's access token
     * @param selectedProfile The player's UUID
     * @param serverId The hostname of the server
     * @param serverPublicKey The server's public key
     * @param sharedSecret The shared secret negotiated by the client and server
     *
     * @return An Object array containing the response code from the server and the string
     *         representation of any additional data received from the server
     *
     * @throws IOException If an I/O error occurs
     *
     * @see Utils#post(String, Object)
     */
    public static Object[] joinServerYggdrasil(String accessToken, String selectedProfile,
            String serverId, PublicKey serverPublicKey, byte[] sharedSecret) throws IOException
    {
        MessageDigest digest = Utils.noFail(() -> MessageDigest.getInstance("SHA-1"));
        digest.update(serverId.getBytes(StandardCharsets.ISO_8859_1));
        digest.update(new SecretKeySpec(sharedSecret, "AES").getEncoded());
        digest.update(serverPublicKey.getEncoded());

        String nServerId = new BigInteger(digest.digest()).toString(16);

        JoinMinecraftServerRequest request = new JoinMinecraftServerRequest();
        request.accessToken = accessToken;
        request.selectedProfile = UUID.fromString(selectedProfile);
        request.serverId = nServerId;

        return Utils.post("https://sessionserver.mojang.com/session/minecraft/join", request);
    }

    private McUtils()
    {}

}
