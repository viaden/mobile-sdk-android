package com.viaden.sdk.http;

import android.net.SSLSessionCache;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class HttpClient {
    private static final String MAX_CONNECTIONS_PROPERTY_NAME = "http.maxConnections";
    private static final String KEEP_ALIVE_PROPERTY_NAME = "http.keepAlive";

    private static final String CONTENT_LENGTH_HEADER = "Content-Length";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";

    private static final int CONNECTION_TIMEOUT = 15000; // 15 sec

    private final int socketOperationTimeout;
    @NonNull
    private final SSLSocketFactoryWrapper sslSocketFactory;
    private boolean hasExecuted;

    public HttpClient() {
        this(true);
    }

    public HttpClient(final boolean secure) {
        this(CONNECTION_TIMEOUT, null, secure);
    }

    public HttpClient(final int connectionTimeout) {
        this(connectionTimeout, null, true);
    }

    public HttpClient(final int socketOperationTimeout, @Nullable final SSLSessionCache sslSessionCache, final boolean secure) {
        this.socketOperationTimeout = socketOperationTimeout;
        if (secure) {
            sslSocketFactory = SSLSocketFactoryWrapper.getDefault(socketOperationTimeout, sslSessionCache);
        } else {
            sslSocketFactory = SSLSocketFactoryWrapper.getInsecure(socketOperationTimeout, sslSessionCache);
        }
    }

    public static void setMaxConnections(final int maxConnections) {
        if (maxConnections <= 0) {
            throw new IllegalArgumentException("Max connections should be large than 0");
        }
        System.setProperty(MAX_CONNECTIONS_PROPERTY_NAME, String.valueOf(maxConnections));
    }

    public static void setKeepAlive(final boolean isKeepAlive) {
        System.setProperty(KEEP_ALIVE_PROPERTY_NAME, String.valueOf(isKeepAlive));
    }

    @NonNull
    public HttpResponse execute(@NonNull final HttpRequest request) throws IOException {
        return execute(request, new StringResponseContentFactory());
    }

    @NonNull
    public HttpResponse execute(@NonNull final HttpRequest request, @NonNull final ResponseContentFactory<?> factory) throws IOException {
        if (!hasExecuted) {
            hasExecuted = true;
        }
        // No more interceptors. Do HTTP.
        HttpURLConnection connection = null;
        InputStream stream = null;
        try {
            connection = buildConnection(request);

            // Start network connection and write data to server if possible
            final HttpBody body = request.getBody();
            if (body != null) {
                final OutputStream outputStream = connection.getOutputStream();
                body.writeTo(outputStream);
                outputStream.flush();
                outputStream.close();
            }
            final int statusCode = connection.getResponseCode();

            // Headers
            final Map<String, String> headers = new HashMap<>();
            for (final Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
                // The status code's key from header entry is always null(like null=HTTP/1.1 200 OK), since we
                // have already had statusCode in HttpResponse, we just ignore this header entry.
                if (entry.getKey() != null && !entry.getValue().isEmpty()) {
                    headers.put(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().get(0));
                }
            }

            // Content
            if (statusCode < 400) {
                stream = connection.getInputStream();
            } else {
                stream = connection.getErrorStream();
            }

            return new HttpResponse.Builder()
                    .setResponseContent(factory.create(statusCode, stream))
                    .setStatusCode(statusCode)
                    .setTotalSize(connection.getContentLength())
                    .setReasonPhrase(connection.getResponseMessage())
                    .setHeaders(headers)
                    .setContentType(connection.getContentType())
                    .build();
        } finally {
            IOUtils.closeQuietly(stream);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @NonNull
    private HttpURLConnection buildConnection(@NonNull final HttpRequest request) throws IOException {
        final URL url = new URL(request.getUrl());

        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(request.getHttpMethod().toString());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            connection.setRequestProperty("Connection", "Close");
        }
        connection.setConnectTimeout(socketOperationTimeout);
        connection.setReadTimeout(socketOperationTimeout);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        if ("https".equals(url.getProtocol())) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
        }

        // Don't handle redirects. We copy the setting from AndroidHttpClient.
        // For detail, check https://quip.com/Px8jAxnaun2r
        connection.setInstanceFollowRedirects(false);
        // Set header
        for (final Map.Entry<String, String> entry : request.getAllHeaders().entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        // Set body
        final HttpBody body = request.getBody();
        if (body != null) {
            // Content type and content length
            connection.setRequestProperty(CONTENT_LENGTH_HEADER, String.valueOf(body.getContentLength()));
            connection.setRequestProperty(CONTENT_TYPE_HEADER, body.getContentType());
            connection.setDoOutput(true);
        }
        return connection;
    }
}
