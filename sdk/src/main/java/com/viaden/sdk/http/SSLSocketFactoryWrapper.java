package com.viaden.sdk.http;

import android.annotation.SuppressLint;
import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * An {@link SSLSocketFactory} that supports TLS settings.
 * Forward all methods. Enable TLS 1.1 and 1.2 before returning.
 */
class SSLSocketFactoryWrapper extends SSLSocketFactory {
    @NonNull
    private final SSLSocketFactory delegate;

    private SSLSocketFactoryWrapper(@NonNull final SSLSocketFactory delegate) {
        this.delegate = delegate;
    }

    @NonNull
    static SSLSocketFactoryWrapper getDefault(final int socketOperationTimeout, final SSLSessionCache sslSessionCache) {
        return new SSLSocketFactoryWrapper(SSLCertificateSocketFactory.getDefault(socketOperationTimeout, sslSessionCache));
    }

    @NonNull
    @SuppressLint("SSLCertificateSocketFactoryGetInsecure")
    static SSLSocketFactoryWrapper getInsecure(final int socketOperationTimeout, final SSLSessionCache sslSessionCache) {
        return new SSLSocketFactoryWrapper(SSLCertificateSocketFactory.getInsecure(socketOperationTimeout, sslSessionCache));
    }

    private static void enableTlsIfAvailable(final Socket socket) {
        if (socket instanceof SSLSocket) {
            final SSLSocket sslSocket = (SSLSocket) socket;
            final String[] supportedProtocols = sslSocket.getSupportedProtocols();
            // Make sure all supported protocols are enabled. Android does not enable TLSv1.1 or TLSv1.2 by default.
            sslSocket.setEnabledProtocols(supportedProtocols);
        }
    }

    //region SocketFactory
    @Override
    public Socket createSocket() throws IOException {
        final Socket socket = delegate.createSocket();
        enableTlsIfAvailable(socket);
        return socket;
    }

    @Override
    public Socket createSocket(final String host, final int port) throws IOException {
        final Socket socket = delegate.createSocket(host, port);
        enableTlsIfAvailable(socket);
        return socket;
    }

    @Override
    public Socket createSocket(final String host, final int port, final InetAddress localhost, final int localPort) throws IOException {
        final Socket socket = delegate.createSocket(host, port, localhost, localPort);
        enableTlsIfAvailable(socket);
        return socket;
    }

    @Override
    public Socket createSocket(final InetAddress address, final int port) throws IOException {
        final Socket socket = delegate.createSocket(address, port);
        enableTlsIfAvailable(socket);
        return socket;
    }
    //endregion

    //region SSLSocketFactory
    @Override
    public Socket createSocket(final InetAddress address, final int port, final InetAddress localhost, final int localPort) throws IOException {
        final Socket socket = delegate.createSocket(address, port, localhost, localPort);
        enableTlsIfAvailable(socket);
        return socket;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(final Socket socketParam, final String host, final int port, final boolean autoClose) throws IOException {
        final Socket socket = delegate.createSocket(socketParam, host, port, autoClose);
        enableTlsIfAvailable(socket);
        return socket;
    }
    //endregion
}
