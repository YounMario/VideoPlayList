package com.android.player.DataSource;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;

import com.android.player.util.AesHelper;
import com.example.videoplaylist.App;
import com.google.android.exoplayer.C;
import com.google.android.exoplayer.upstream.DataSpec;
import com.google.android.exoplayer.upstream.TransferListener;
import com.google.android.exoplayer.upstream.UriDataSource;

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
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesDataSource implements UriDataSource {

    private static AssetManager assetManager = App.getInstance().getAssets();

    private static final String TAG = AesDataSource.class.getSimpleName();
    private TransferListener listener;

    private String uriString;
    private InputStream inputStream;
    private CipherInputStream cipherInputStream;
    private long bytesRemaining;
    private boolean opened;

    private Uri mUri;

    public static final class AesDataSourceException extends IOException {

        public AesDataSourceException(IOException cause) {
            super(cause);
        }
    }

    public AesDataSource(Uri uri) {
        mUri = uri;
    }

    public AesDataSource(Context context) {
        this(context, null);
    }

    public AesDataSource(Context context, TransferListener listener) {
        this.listener = listener;
    }

    @Override
    public String getUri() {
        return uriString;
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        try {
            uriString = mUri.toString();
            String path;
            if (uriString.startsWith("/android_asset/")) {
                path = uriString.substring(15);
                inputStream = assetManager.open(path, AssetManager.ACCESS_RANDOM);
            } else {
                inputStream = new FileInputStream(new File(uriString));
            }

            //Decrypt Logic
            cipher = Cipher.getInstance("AES/CTR/NoPadding");
            Key cipherKey = new SecretKeySpec(AesHelper.AES_KEY, "AES");
            AlgorithmParameterSpec cipherIV = new IvParameterSpec(AesHelper.AES_IV);
            cipher.init(Cipher.DECRYPT_MODE, cipherKey, new IvParameterSpec(new byte[16]));
            cipherInputStream = new CipherInputStream(inputStream, cipher);

            AesHelper.jumpToOffset(cipher, new SecretKeySpec(AesHelper.AES_KEY, "AES"), new IvParameterSpec(new byte[16]), dataSpec.position);
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

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
        opened = true;
        if (listener != null) {
            listener.onTransferStart();
        }
        return bytesRemaining;
    }

    @Override
    public void close() throws IOException {
        uriString = null;
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                throw new AesDataSourceException(e);
            } finally {
                inputStream = null;
                if (opened) {
                    opened = false;
                    if (listener != null) {
                        listener.onTransferEnd();
                    }
                }
            }
        }
    }

    SecretKeySpec key;
    Cipher cipher;
    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        if (bytesRemaining == 0) {
            return -1;
        } else {
            int bytesRead = 0;
            try {
                int bytesToRead = bytesRemaining == C.LENGTH_UNBOUNDED ? readLength
                        : (int) Math.min(bytesRemaining, readLength);

//                seek(offset);
//                jumpToOffset(cipher, new SecretKeySpec(AES_KEY, "AES"), new IvParameterSpec(new byte[16]), offset);

                bytesRead = cipherInputStream.read(buffer, offset, bytesToRead);
                //bytesRead = inputStream.read(buffer, offset, bytesToRead);

            } catch (IOException e) {
                throw new AesDataSourceException(e);
            }

            if (bytesRead > 0) {
                if (bytesRemaining != C.LENGTH_UNBOUNDED) {
                    bytesRemaining -= bytesRead;
                }
                if (listener != null) {
                    listener.onBytesTransferred(bytesRead);
                }
            }
            return bytesRead;
        }
    }
}