package com.kbsriram.keypan.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

public class CDefaultGetter
    implements IGetter
{
    public CDefaultGetter() {}

    public void setTarget(URL url)
        throws IOException
    {
        if (CUtils.isDebug()) {
            CUtils.lognote
                (CDefaultGetter.class, url.toString());
        }
        else {
            CUtils.lognote
                (CDefaultGetter.class, "fetch: "+url.getHost()+url.getPath());
        }
        m_conn = url.openConnection();
        if (m_conn instanceof HttpsURLConnection) {
            pin((HttpsURLConnection) m_conn);
        }
    }

    public void setHeader(String key, String value)
    {
        check();
        m_conn.setRequestProperty(key, value);
    }

    public InputStream openStream()
        throws IOException
    {
        check();
        return m_conn.getInputStream();
    }

    private final static void pin(HttpsURLConnection conn)
    { conn.setSSLSocketFactory(s_ctx.getSocketFactory()); }

    private void check()
    {
        if (m_conn == null) {
            throw new IllegalStateException("must first set url");
        }
    }

    private URLConnection m_conn;

    private final static SSLContext s_ctx;
    static
    {
        try {
            final CPinnedTrustManager ptm = new CPinnedTrustManager
                (new String[] {
                    "e16db1168a72d13d7a073a3647387bee290060d9", // sks
                    "43dad630ee53f8a980ca6efd85f46aa37990e0ea", // google
                    "dc9f1c64879d88ca7d0cce64d18d232d1095aa1c", // github
                    "1579f0bcedf49dba5c2608c32ba6c7fab192a84a", // twitter
                }, new CRootKeys());

            s_ctx = SSLContext.getInstance("TLS");
            s_ctx.init(null, new TrustManager[] {ptm}, null);
        }
        catch (Throwable any) {
            throw new ExceptionInInitializerError(any);
        }
    }
}
