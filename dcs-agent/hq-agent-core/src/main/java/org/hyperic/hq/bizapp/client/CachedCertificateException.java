package org.hyperic.hq.bizapp.client;

import java.security.cert.X509Certificate;

@SuppressWarnings("serial")
public class CachedCertificateException extends Exception {
    private X509Certificate[] cachedCertificatesChain;

    public CachedCertificateException(X509Certificate[] cachedCertificatesChain,
                                      Exception e) {
        super(e);
        this.cachedCertificatesChain = cachedCertificatesChain;
    }

    public X509Certificate[] getCachedCertificatesChain() {
        return cachedCertificatesChain;
    }
}
