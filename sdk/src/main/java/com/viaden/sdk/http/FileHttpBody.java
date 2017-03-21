package com.viaden.sdk.http;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileHttpBody extends HttpBody {
    @NonNull
    private final File file;

    public FileHttpBody(@NonNull final File file, @NonNull final String contentType) {
        super(contentType, file.length());
        this.file = file;
    }

    @NonNull
    @Override
    public InputStream getContent() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public void writeTo(@NonNull final OutputStream out) throws IOException {
        final FileInputStream fileInput = new FileInputStream(file);
        try {
            IOUtils.copy(fileInput, out);
        } finally {
            IOUtils.closeQuietly(fileInput);
        }
    }
}
