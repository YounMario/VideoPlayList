package com.android.player.DataSource;

import android.content.res.AssetManager;
import android.net.Uri;

import com.android.player.util.RotAlgo;
import com.example.videoplaylist.App;
import com.google.android.exoplayer.C;
import com.google.android.exoplayer.upstream.DataSpec;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.Assertions;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Aes128DataSource implements UriDataSource {
    private static final byte[] AES_KEY = hexStringToByteArray(RotAlgo.rotate("_hgueqrscea`sbfbrpstctgbaeafqcue"));
    private static final byte[] AES_IV = hexStringToByteArray(RotAlgo.rotate("_ph`faf`eptecagc_hggdqgqgahrrq_d"));
    private static final String TAG = "Aes128DataSource";

    private String uriString;
    private static AssetManager assetManager = App.getInstance().getAssets();
    private CipherInputStream cipherInputStream;
    private InputStream inputStream;

    private long bytesRemaining;
    private final Uri mUri;

    public Aes128DataSource(Uri uri) {
        mUri = uri;
        initStream();
    }

    private void initStream() {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }

        Key cipherKey = new SecretKeySpec(AES_KEY, "AES");
        AlgorithmParameterSpec cipherIV = new IvParameterSpec(AES_IV);

        try {
            cipher.init(Cipher.DECRYPT_MODE, cipherKey, cipherIV);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }

        try {
            uriString = mUri.toString();
            String path;
            if (uriString.startsWith("/android_asset/")) {
                path = uriString.substring(15);
                inputStream = assetManager.open(path, AssetManager.ACCESS_RANDOM);
            } else {
                inputStream = new FileInputStream(new File(uriString));
            }
            cipherInputStream = new CipherInputStream(inputStream, cipher);
        }   catch (IOException e) {
        }
    }

    @Override
    public String getUri() {
        return uriString;
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        try {
            if (cipherInputStream == null) {
                initStream();
            }

            long cipherInputStreamSkipped = cipherInputStream.skip(dataSpec.position);
            long skipped = inputStream.skip(dataSpec.position);
            if (skipped < dataSpec.position) {
                // assetManager.open() returns an AssetInputStream, whose skip() implementation only skips
                // fewer bytes than requested if the skip is beyond the end of the asset's data.
                throw new EOFException();
            }
            if (dataSpec.length != C.LENGTH_UNBOUNDED) {
                bytesRemaining = dataSpec.length;
            } else {
                bytesRemaining = inputStream.available();
                if (bytesRemaining == Integer.MAX_VALUE) {
                    // assetManager.open() returns an AssetInputStream, whose available() implementation
                    // returns Integer.MAX_VALUE if the remaining length is greater than (or equal to)
                    // Integer.MAX_VALUE. We don't know the true length in this case, so treat as unbounded.
                    bytesRemaining = C.LENGTH_UNBOUNDED;
                }
            }
        } catch (IOException e) {
            throw e;
        }
        return bytesRemaining;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        Assertions.checkState(cipherInputStream != null);
        int bytesRead = cipherInputStream.read(buffer, offset, readLength);
        if (bytesRead < 0) {
            return -1;
        }
        return bytesRead;
    }

    private static byte[] hexStringToByteArray(String s) {
        s = s.toLowerCase();
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
