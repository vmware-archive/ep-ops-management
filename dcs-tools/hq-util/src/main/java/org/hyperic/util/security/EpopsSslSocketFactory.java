package org.hyperic.util.security;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.params.HttpParams;

public class EpopsSslSocketFactory extends SSLSocketFactory {

    private String[] enabledCiphers = { "TLS_RSA_WITH_AES_256_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA" };

    public EpopsSslSocketFactory(SSLContext sslContext,
                                 X509HostnameVerifier hostnameVerifier) {
        super(sslContext, hostnameVerifier);
    }

    @Override
    public Socket createSocket()
        throws IOException {
        SSLSocket sslSocket = (SSLSocket) super.createSocket();
        sslSocket.setEnabledCipherSuites(enabledCiphers);
        return sslSocket;
    }

    @Override
    public Socket createSocket(Socket s,
                               String host,
                               int port,
                               boolean autoClose)
        throws UnknownHostException, IOException {
        SSLSocket sslSocket = (SSLSocket) super.createSocket(s, host, port, autoClose);
        sslSocket.setEnabledCipherSuites(enabledCiphers);
        return sslSocket;
    }

    @Override
    public Socket createSocket(HttpParams params)
        throws IOException {
        SSLSocket sslSocket = (SSLSocket) super.createSocket(params);
        sslSocket.setEnabledCipherSuites(enabledCiphers);
        return sslSocket;
    }
}
