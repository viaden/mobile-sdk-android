package com.viaden.sdk;

import android.support.annotation.NonNull;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class HashUtils {
    @NonNull
    static String asMD5(@NonNull final String string) {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(string.getBytes(Charset.defaultCharset()), 0, string.length());
            return new BigInteger(1, messageDigest.digest()).toString();
        } catch (@NonNull final NoSuchAlgorithmException e) {
            return String.valueOf(string.hashCode());
        }
    }
}
