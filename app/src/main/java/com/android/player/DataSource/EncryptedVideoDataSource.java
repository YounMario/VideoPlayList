package com.android.player.DataSource;

/**
 * Created by wangye on 16-8-10.
 */

import android.content.res.AssetManager;
import android.net.Uri;

import com.android.player.util.IOUtils;
import com.android.player.util.RotAlgo;
import com.cleanmaster.util.CMLog;
import com.google.android.exoplayer.C;
import com.google.android.exoplayer.upstream.DataSpec;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.keniu.security.MoSecurityApplication;

import java.io.ByteArrayInputStream;
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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class EncryptedVideoDataSource implements UriDataSource {

    public static final String TAG = "EncryptedDataSource";

    private static final byte[] AES_KEY = RotAlgo.hexStringToByteArray(RotAlgo.rotate("_hgueqrscea`sbfbrpstctgbaeafqcue"));
    private static final byte[] AES_IV = RotAlgo.hexStringToByteArray(RotAlgo.rotate("_ph`faf`eptecagc_hggdqgqgahrrq_d"));

    private final Uri mUri;
    private byte[] decByteArray;
    public EncryptedVideoDataSource(Uri uri) {
        mUri = uri;
        initData();
    }

    private void initData() {
        try {
            InputStream encryptedInputStream;

            uriString = mUri.toString();
            String path;
            if (uriString.startsWith("/android_asset/")) {
                path = uriString.substring(15);
                encryptedInputStream = assetManager.open(path, AssetManager.ACCESS_RANDOM);
            } else {
                encryptedInputStream = new FileInputStream(new File(uriString));
            }

            long time = System.currentTimeMillis();
            byte[] encryptedInputByteArray = IOUtils.toByteArray(encryptedInputStream);
            decByteArray = decrypt(encryptedInputByteArray);
            CMLog.i(TAG, "decryption costs time: " + (System.currentTimeMillis() - time)
                    + ", size: " + (decByteArray == null ? 0 : decByteArray.length));
            // byte[] reEcryptedByteArray = encrypt(encryptedInputByteArray);
        }  catch (IOException e) {
        }
    }

    public static class EncryptedVideoDataSourceException extends IOException {
        public EncryptedVideoDataSourceException(IOException cause) {
            super(cause);
        }
        public EncryptedVideoDataSourceException(String detailMessage) {
            super(detailMessage);
        }
    }
    private InputStream inputStream;
    private String uriString;
    private long bytesRemaining;
    private boolean opened;

    private static AssetManager assetManager = MoSecurityApplication.getInstance().getAssets();
    @Override
    public long open(DataSpec dataSpec) throws EncryptedVideoDataSourceException {
        try {
            if (decByteArray == null) {
                initData();
            }
            if (decByteArray == null) {
                throw new EncryptedVideoDataSourceException("decoded failed, path: " + mUri.getPath());
            }
            inputStream = new ByteArrayInputStream(decByteArray);
            long skipped = inputStream.skip(dataSpec.position);
            if (skipped < dataSpec.position) {
                throw new EOFException();
            }
            if (dataSpec.length != C.LENGTH_UNBOUNDED) {
                bytesRemaining = dataSpec.length;
            } else {
                bytesRemaining = inputStream.available();
                if (bytesRemaining == 0) {
                    bytesRemaining = C.LENGTH_UNBOUNDED;
                }
            }
        } catch (IOException e) {
            throw new EncryptedVideoDataSourceException(e);
        }
        opened = true;
        CMLog.i(TAG, "open dataSpec: " + dataSpec + ", return bytesRemaining: " + bytesRemaining);
        return bytesRemaining;
    }
    @Override
    public int read(byte[] buffer, int offset, int readLength) throws EncryptedVideoDataSourceException {
        if (bytesRemaining == 0) {
            return -1;
        } else {
            int bytesRead = 0;
            try {
                int bytesToRead = bytesRemaining == C.LENGTH_UNBOUNDED ? readLength
                        : (int) Math.min(bytesRemaining, readLength);
                bytesRead = inputStream.read(buffer, offset, bytesToRead);
            } catch (IOException e) {
                throw new EncryptedVideoDataSourceException(e);
            }
            if (bytesRead > 0) {
                if (bytesRemaining != C.LENGTH_UNBOUNDED) {
                    bytesRemaining -= bytesRead;
                }
            }
            return bytesRead;
        }
    }
    @Override
    public String getUri() {
        return uriString;
    }
    @Override
    public void close() throws EncryptedVideoDataSourceException {
        uriString = null;
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                throw new EncryptedVideoDataSourceException(e);
            } finally {
                inputStream = null;
                if (opened) {
                    opened = false;
                }
            }
        }
    }

    private static byte[] decrypt(byte[] encryptedInputByteArray) {
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
            return cipher.doFinal(encryptedInputByteArray);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] encrypt(byte[] encryptedInputByteArray) {
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
            cipher.init(Cipher.ENCRYPT_MODE, cipherKey, cipherIV);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }

        try {
            return cipher.doFinal(encryptedInputByteArray);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }
}