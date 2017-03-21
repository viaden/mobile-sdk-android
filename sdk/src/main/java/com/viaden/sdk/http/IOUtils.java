package com.viaden.sdk.http;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * General IO stream manipulation utilities.
 */
class IOUtils {
    private static final int EOF = -1;
    /**
     * The default buffer size ({@value}) to use for {@link #copyLarge(InputStream, OutputStream)}.
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    /**
     * Get the contents of an <code>InputStream</code> as a <code>byte[]</code>.
     * <p/>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     *
     * @param input the <code>InputStream</code> to read from
     * @return the requested byte array
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O error occurs
     */
    @NonNull
    static byte[] toByteArray(@NonNull final InputStream input) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output);
        return output.toByteArray();
    }

    /**
     * Copy bytes from an <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p/>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * <p/>
     * Large streams (over 2GB) will return a bytes copied value of
     * <code>-1</code> after the copy has completed since the correct
     * number of bytes cannot be returned as an int. For large streams
     * use the <code>copyLarge(InputStream, OutputStream)</code> method.
     *
     * @param input  the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @return the number of bytes copied, or -1 if &gt; Integer.MAX_VALUE
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     */
    static int copy(@NonNull final InputStream input, @NonNull final OutputStream output) throws IOException {
        final long count = copyLarge(input, output);
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) count;
    }

    /**
     * Copy bytes from a large (over 2GB) <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p/>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * <p/>
     * The buffer size is given by {@link #DEFAULT_BUFFER_SIZE}.
     *
     * @param input  the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     */
    private static long copyLarge(@NonNull final InputStream input, @NonNull final OutputStream output) throws IOException {
        return copyLarge(input, output, new byte[DEFAULT_BUFFER_SIZE]);
    }

    /**
     * Copy bytes from a large (over 2GB) <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p/>
     * This method uses the provided buffer, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * <p/>
     *
     * @param input  the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @param buffer the buffer to use for the copy
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     */
    private static long copyLarge(@NonNull final InputStream input, @NonNull final OutputStream output, @NonNull final byte[] buffer) throws IOException {
        long count = 0;
        int n;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
     * Unconditionally close an <code>InputStream</code>.
     * <p/>
     * Equivalent to {@link InputStream#close()}, except any exceptions will be ignored.
     * This is typically used in finally blocks.
     *
     * @param input the InputStream to close, may be null or already closed
     */
    static void closeQuietly(@Nullable final InputStream input) {
        try {
            if (input != null) {
                input.close();
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Unconditionally close an <code>OutputStream</code>.
     * <p/>
     * Equivalent to {@link OutputStream#close()}, except any exceptions will be ignored.
     * This is typically used in finally blocks.
     *
     * @param output the OutputStream to close, may be null or already closed
     */
    private static void closeQuietly(@Nullable final OutputStream output) {
        try {
            if (output != null) {
                output.close();
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Closes a <code>Closeable</code> unconditionally.
     * <p/>
     * Equivalent to {@link Closeable#close()}, except any exceptions will be ignored.
     * This is typically used in finally blocks.
     * <p/>
     * Example code:
     * <pre>
     *   Closeable closeable = null;
     *   try {
     *       closeable = new FileReader("foo.txt");
     *       // process closeable
     *       closeable.close();
     *   } catch (Exception e) {
     *       // error handling
     *   } finally {
     *       IOUtils.closeQuietly(closeable);
     *   }
     * </pre>
     *
     * @param closeable the object to close, may be null or already closed
     */
    public static void closeQuietly(@Nullable final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (final IOException ignored) {
        }
    }

    public static void writeTo(@NonNull final InputStream inputStream, @NonNull final File file) throws IOException {
        FileOutputStream fos = null;
        InputStream bis = null;
        try {
            fos = new FileOutputStream(file);
            bis = new BufferedInputStream(inputStream);
            byte[] buffer = new byte[4096];
            int total;
            while ((total = bis.read(buffer)) != -1) {
                fos.write(buffer, 0, total);
            }
            fos.flush();
        } finally {
            IOUtils.closeQuietly(fos);
            IOUtils.closeQuietly(bis);
        }
    }
}
