package com.kbsriram.keypan.servlet;

import com.kbsriram.keypan.core.CUtils;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPPublicKeyRing;

@SuppressWarnings("serial")
public final class CKeySaveServlet extends HttpServlet
{
    final static String save(PGPPublicKeyRing pkr)
    {
        String fp = CUtils.byte2hex(pkr.getPublicKey().getFingerprint());
        saveToCache(fp, pkr);
        return "/save?fp="+fp;
    }

    @Override
    public void service
        (HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        String fp = CUtils.nullIfEmpty(req.getParameter("fp"));
        if (fp == null) {
            CLookupServlet.error(resp, "missing fp parameter");
            return;
        }

        PGPPublicKeyRing pkr = findFromCache(fp);
        if (pkr == null) {
            CLookupServlet.error
                (resp, "Sorry I no longer remember this key :-(. Please try searching for it again.");
            return;
        }

        resp.setContentType("application/pgp-keys");
        resp.addHeader
            ("Content-Disposition",
             "attachment; filename="+fp+".asc");

        ArmoredOutputStream out = new ArmoredOutputStream
            (new BufferedOutputStream
             (resp.getOutputStream()));
        pkr.encode(out);
        out.flush();
    }

    private final static synchronized PGPPublicKeyRing findFromCache(String k)
    { return s_cache.get(k); }

    private final static synchronized PGPPublicKeyRing saveToCache
        (String k, PGPPublicKeyRing pkr)
    { return s_cache.put(k, pkr); }

    private final static int MAX_ENTRIES = 20;
    private final static LinkedHashMap<String,PGPPublicKeyRing> s_cache =
        (new LinkedHashMap<String,PGPPublicKeyRing>() {
            @Override protected boolean removeEldestEntry(Map.Entry e) {
                return size() > MAX_ENTRIES;
            }
        });
}
