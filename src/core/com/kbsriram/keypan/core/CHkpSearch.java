package com.kbsriram.keypan.core;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;

/**
 * Simple lib to search/grab keys from hkp servers.
 */
public class CHkpSearch
{
    public final static List<Info> lookup(String q, IGetter getter)
        throws IOException
    { return lookup(q, getter, DEFAULT_HKP); }

    public final static PGPPublicKeyRingCollection get
        (String kid, IGetter getter)
        throws IOException
    { return get(kid, getter, DEFAULT_HKP); }

    public final static PGPPublicKeyRingCollection get
        (String kid, IGetter getter, String base)
        throws IOException
    {
        getter.setTarget
            (new URL
             (base+"?op=get&options=mr&search=0x"+kid));
        getter.setHeader("User-Agent", "keypan");
        InputStream in = null;
        try {
            in = PGPUtil.getDecoderStream
                (new BufferedInputStream(getter.openStream()));
            return new PGPPublicKeyRingCollection(in);
        }
        catch (PGPException pge) {
            // skip
            CUtils.logw(CHkpSearch.class, "Skip key, unable to parse", pge);
            return null;
        }
        catch (IOException ioe) {
            // skip
            CUtils.logw(CHkpSearch.class, "Unable to search server", ioe);
            return null;
        }
        finally {
            if (in != null) {
                try { in.close(); }
                catch (Throwable th) {}
            }
        }
    }

    public final static List<Info> lookup(String q, IGetter getter, String base)
        throws IOException
    {
        List<Info> ret = new ArrayList<Info>();

        getter.setTarget
            (new URL
             (base+"?op=index&options=mr&search="+
              URLEncoder.encode(q, "utf-8")));
        getter.setHeader("User-Agent", "keypan");
        BufferedReader br = null;
        try {
            br = new BufferedReader
                (new InputStreamReader(getter.openStream()));
            String line;
            Info cur = null;
            while ((line = br.readLine()) != null) {
                Matcher m;
                if ((m = s_pubre.matcher(line)).matches()) {
                    if (cur != null) {
                        ret.add(cur);
                    }
                    cur = checkPub(m);
                }
                else if ((m = s_uidre.matcher(line)).matches()) {
                    maybeAddUid(cur, m);
                }
                else {
                    // quietly skip.
                }
            }
            // grab anything in-progress.
            if (cur != null) { ret.add(cur); }
        }
        catch (FileNotFoundException fne) {
            // 404 - treat as no results.
        }
        finally {
            if (br != null) {
                try { br.close(); }
                catch (Throwable th) {}
            }
        }
        return ret;
    }

    private final static boolean notEmpty(String s)
    { return (s != null) && (s.length() > 0); }

    private final static Info checkPub(Matcher m)
    {
        String fp = m.group(1).toLowerCase();
        String flags = m.group(6);
        if (notEmpty(flags)) {
            CUtils.logd(CHkpSearch.class,
                        "reject 0x"+fp+" because flags="+flags);
            return null;
        }
        String exp = m.group(5);
        if (notEmpty(exp)) {
            try {
                long ts = Long.parseLong(exp);
                if (ts < (System.currentTimeMillis()/1000l)) {
                    CUtils.logd(CHkpSearch.class,
                                "reject 0x"+fp+" because key has expired");
                    return null;
                }
            }
            catch (NumberFormatException nfe) {
                CUtils.logd(CHkpSearch.class,
                            "reject 0x"+fp+" because bad parse: "+exp);
                return null;
            }
        }
        return new Info(fp);
    }

    private final static void maybeAddUid(Info info, Matcher m)
    {
        if (info == null) { return; }
        String flags = m.group(4);
        if (notEmpty(flags)) {
            CUtils.logd(CHkpSearch.class,
                        "uid-reject because flags="+flags);
            return;
        }
        String exp = m.group(3);
        if (notEmpty(exp)) {
            try {
                long ts = Long.parseLong(exp);
                if (ts < (System.currentTimeMillis()/1000l)) {
                    CUtils.logd(CHkpSearch.class,
                                "uid-reject because uid has expired");
                    return;
                }
            }
            catch (NumberFormatException nfe) {
                CUtils.logd(CHkpSearch.class,
                            "uid-reject because bad parse: "+exp);
                return;
            }
        }
        String uid;
        try {
            uid = URLDecoder.decode(m.group(1), "utf-8");
        }
        catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }
        info.addUid(uid);
    }

    public final static class Info
    {
        private Info(String kid)
        { m_kid = kid; }
        private Info addUid(String uid)
        {
            m_uids.add(uid);
            return this;
        }

        public String getKeyId()
        { return m_kid; }
        public List<String> getUids()
        { return m_uids; }
        private final String m_kid;
        private final List<String> m_uids = new ArrayList<String>();
    }
    private final static String DEFAULT_HKP =
        "http://hkps.pool.sks-keyservers.net/pks/lookup";
    // unfortunately, unable to consistently obtain a ciphersuite in
    // java-land that's able to handle all the servers in the ssl
    // pool.
    // "https://hkps.pool.sks-keyservers.net/pks/lookup";
    private final static Pattern s_pubre =
        Pattern.compile
        ("^pub:([a-fA-F0-9]+):(\\d+)?:(\\d+)?:(\\d+)?:(\\d+)?:([rde]+)?$");
    private final static Pattern s_uidre =
        Pattern.compile
        ("^uid:(.+):(\\d+)?:(\\d+)?:([rde]+)?$");
}
