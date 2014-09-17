/**
 * Based on https://github.com/moxie0/AndroidPinning/
 *
 * Add a very specific set of allowable public keys.
 */

package com.kbsriram.keypan.core;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

class CPinnedTrustManager
    implements X509TrustManager
{
    CPinnedTrustManager(String[] pins, CRootKeys rk)
    {
        m_roots = rk;
        for (String pin : pins) {
            m_pins.add(hexStringToByteArray(pin));
        }
    }

    private boolean isValidPin(X509Certificate certificate)
        throws CertificateException
    {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA1");
            final byte[] spki = certificate.getPublicKey().getEncoded();
            final byte[] pin  = digest.digest(spki);

            for (byte[] validPin : m_pins) {
                if (Arrays.equals(validPin, pin)) {
                    return true;
                }
            }
            // System.out.println("no pin for "+CUtils.byte2hex(pin));
            // System.out.println(certificate);
            return false;
        }
        catch (NoSuchAlgorithmException nsae) {
            throw new CertificateException(nsae);
        }
    }

    private void checkPinTrust(X509Certificate[] chain)
      throws CertificateException
    {
        for (X509Certificate certificate : cleanup(chain, m_roots)) {
            if (isValidPin(certificate)) {
                return;
            }
        }
        throw new CertificateException("No valid pins found in chain!");
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType)
      throws CertificateException
    { throw new CertificateException("Client certificates not supported!"); }

    public void checkServerTrusted(X509Certificate[] chain, String authType)
        throws CertificateException
    {
        if (m_cache.contains(chain[0])) {
            return;
        }
        checkPinTrust(chain);
        m_cache.add(chain[0]);
    }

    public X509Certificate[] getAcceptedIssuers()
    { return null; }

    private final static byte[] hexStringToByteArray(String s)
    {
        final int len = s.length();
        final byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) +
                                  Character.digit(s.charAt(i + 1), 16));
        }

        return data;
    }

    public void clearCache()
    { m_cache.clear(); }

    private final static List<X509Certificate> cleanup
        (X509Certificate[] chain, CRootKeys roots)
      throws CertificateException
    {
        final List<X509Certificate> ret = new ArrayList<X509Certificate>();
        int i;

        boolean foundroot = roots.contains(chain[0]);

        ret.add(chain[0]);

        for (i = 1; i < chain.length; i++) {
            if (isValidLink(chain[i], chain[i - 1])) {
                ret.add(chain[i]);
                if (!foundroot && roots.contains(chain[i])) {
                    foundroot = true;
                }
            }
            else {
                CUtils.logw(CHkpSearch.class, "Invalid link from "+chain[i]);
                break;
            }
        }
        if (!foundroot) {
            throw new CertificateException("Did not find a trusted root.");
        }
        else {
            return ret;
        }
    }

    private final static boolean isValidLink
        (X509Certificate parent, X509Certificate child)
    {
        if (!parent.getSubjectX500Principal()
            .equals(child.getIssuerX500Principal())) {
            return false;
        }

        try { child.verify(parent.getPublicKey()); }
        catch (GeneralSecurityException gse) {
            return false;
        }
        return true;
    }

    private final List<byte[]> m_pins = new ArrayList<byte[]>();
    private final CRootKeys m_roots;
    private final Set<X509Certificate> m_cache =
        Collections.synchronizedSet(new HashSet<X509Certificate>());
}
