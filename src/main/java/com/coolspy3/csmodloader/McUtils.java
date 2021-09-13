package com.coolspy3.csmodloader;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;

public final class McUtils {

    public static File getMcDirFile() {
        return getMcDirPath().toFile();
    }

    public static Path getMcDirPath() {
        String os = System.getProperty("os.name").toLowerCase();

        if(os.contains("win")) {
            return Paths.get(System.getenv("APPDATA"), ".minecraft");
        } else if(os.contains("mac")) {
            return Paths.get(System.getProperty("user.home"), "Library", "Application Support", "minecraft");
        } else if(os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return Paths.get(System.getProperty("user.home"), ".minecraft");
        } else {
            System.err.println("Unsupported OS!");
            System.exit(1);
            return null;
        }
    }

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

    public static boolean validateAccessToken(String accessToken) {
        try {
            ValidateTokenRequest request = new ValidateTokenRequest();
            request.accessToken = accessToken;

            return (int)Utils.post("https://authserver.mojang.com/validate", request)[0] < 300;
        } catch(IOException e) {
            return false;
        }
    }

    private McUtils() {}

}
