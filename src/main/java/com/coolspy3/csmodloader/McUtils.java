package com.coolspy3.csmodloader;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;

public final class McUtils {

    public static Object[] joinServerYggdrasil(String accessToken, String selectedProfile, String serverId, PublicKey serverPublicKey, byte[] sharedSecret) throws IOException {
        MessageDigest digest = Utils.noFail(() -> MessageDigest.getInstance("SHA-1"));
        digest.update(serverId.getBytes("ISO_8859_1"));
        digest.update(new SecretKeySpec(sharedSecret, "AES").getEncoded());
        digest.update(serverPublicKey.getEncoded());
        String nServerId = new BigInteger(digest.digest()).toString(16);

        JoinMinecraftServerRequest request = new JoinMinecraftServerRequest();
        request.accessToken = accessToken;
        request.selectedProfile = UUID.fromString(selectedProfile);
        request.serverId = nServerId;

        return Utils.post("https://sessionserver.mojang.com/session/minecraft/join", request);
    }

    private McUtils() {}

}
