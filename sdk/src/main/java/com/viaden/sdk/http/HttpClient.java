package com.viaden.sdk.http;

import android.net.SSLSessionCache;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
    // There is no need to keep locks for interceptor lists since they will only be changed before we make network request
    @Nullable
    private List<NetworkInterceptor> interceptors;

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
        return new NetworkInterceptorChain(0, request, factory).proceed(request, factory);
    }

    public void addInterceptor(@NonNull final NetworkInterceptor interceptor) {
        // If we do not have the restriction, we may have read/write conflict on the interceptor list
        // and need to add lock to protect it. If in the future we need to add interceptor after
        // HttpClient start to execute, it is safe to remove this check and add lock.
        if (hasExecuted) {
            throw new IllegalStateException("Can't only be invoked before HttpClient execute any request");
        }
        if (interceptors == null) {
            interceptors = new ArrayList<>();
        }
        interceptors.add(interceptor);
    }

    @NonNull
    @VisibleForTesting
    HttpURLConnection buildConnection(@NonNull final HttpRequest request) throws IOException {
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

    static class StringResponseContentFactory implements ResponseContentFactory<String> {

        @NonNull
        @Override
        public ResponseContent<String> create(final int statusCode, @NonNull final InputStream stream) throws IOException {
            return new StringResponseContent(stream);
        }

        private static final class StringResponseContent implements ResponseContent<String> {
            @Nullable
            private final String content;

            private StringResponseContent(@NonNull final InputStream stream) throws IOException {
                content = new String(IOUtils.toByteArray(stream), Charset.forName("UTF-8"));
            }

            @Nullable
            public String getContent() {
                return content;
            }
        }
    }

    private class NetworkInterceptorChain implements NetworkInterceptor.Chain {
        private final int index;
        @NonNull
        private final HttpRequest request;
        @NonNull
        private final ResponseContentFactory<?> factory;

        NetworkInterceptorChain(final int index, @NonNull final HttpRequest request, @NonNull final ResponseContentFactory<?> factory) {
            this.index = index;
            this.request = request;
            this.factory = factory;
        }

        @NonNull
        @Override
        public HttpRequest getRequest() {
            return request;
        }

        @NonNull
        public ResponseContentFactory<?> getFactory() {
            return factory;
        }

        @NonNull
        @Override
        public HttpResponse proceed(@NonNull final HttpRequest request, @NonNull final ResponseContentFactory<?> factory) throws IOException {
            if (interceptors != null && index < interceptors.size()) {
                // There's another internal interceptor in the chain. Call that.
                final NetworkInterceptor.Chain chain = new NetworkInterceptorChain(index + 1, request, factory);
                return interceptors.get(index).intercept(chain);
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
    }
}
