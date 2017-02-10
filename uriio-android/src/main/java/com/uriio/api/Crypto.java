package com.uriio.api;

import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.curve25519.Curve25519KeyPair;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

class Crypto {
    private static final String HMAC_SHA_256 = "hmacSHA256";

    // somehow Curve25519.getInstance() is not exactly a light-weight singleton; we'll make it so
    private static Curve25519 _ec = null;

    private static Curve25519 getEC() {
        if (null == _ec) _ec = Curve25519.getInstance(Curve25519.BEST);
        return _ec;
    }

    static byte[] computeSharedSecret(byte[] servicePublicKey, byte[] beaconPrivateKey) {
        return getEC().calculateAgreement(servicePublicKey, beaconPrivateKey);
    }

    static Curve25519KeyPair generateKeyPair() {
        return getEC().generateKeyPair();
    }

    static byte[] computeIdentityKey(byte[] sharedSecret, byte[] serverPublicKey, byte[] beaconPublicKey)
            throws InvalidKeyException, NoSuchAlgorithmException {
        byte[] salt = new byte[serverPublicKey.length + beaconPublicKey.length];
        System.arraycopy(serverPublicKey, 0, salt, 0, serverPublicKey.length);
        System.arraycopy(beaconPublicKey, 0, salt, serverPublicKey.length, beaconPublicKey.length);

        Mac mac = Mac.getInstance(HMAC_SHA_256);

        // hkdf extract
        mac.init(new SecretKeySpec(salt, HMAC_SHA_256));
        byte[] pseudoRandomKey = mac.doFinal(sharedSecret);

        // hkdf expand
        mac.reset();
        mac.init(new SecretKeySpec(pseudoRandomKey, HMAC_SHA_256));

        return mac.doFinal(new byte[]{1});
    }
}
