/**
 * Based on https://github.com/moxie0/AndroidPinning/
 */
package com.kbsriram.keypan.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

final class CRootKeys
{
    CRootKeys()
        throws IOException, GeneralSecurityException
    {
        final KeyStore ks = KeyStore.getInstance("BKS");
        BufferedInputStream bin = null;
        try {
            bin = new BufferedInputStream
                (CRootKeys.class.getResourceAsStream("trusted_roots"));
            ks.load(bin, "trusted".toCharArray());
        }
        finally {
            if (bin != null) {
                try { bin.close(); }
                catch (Throwable any) {}
            }
        }

        for (Enumeration<String> it = ks.aliases(); it.hasMoreElements();) {
            final String alias = it.nextElement();
            final X509Certificate cert = (X509Certificate)
                ks.getCertificate(alias);
            if (cert != null) {
                m_roots.put(cert.getSubjectX500Principal(), cert);
            }
        }
    }

    final boolean contains(X509Certificate cert)
    {
        final X509Certificate root = m_roots.get
            (cert.getSubjectX500Principal());
        return
            (root != null) &&
            root.getPublicKey().equals(cert.getPublicKey());
    }
    private final Map<Principal, X509Certificate> m_roots =
        new HashMap<Principal, X509Certificate>();
    static
    {
        Security.addProvider(new BouncyCastleProvider());
    }
}
