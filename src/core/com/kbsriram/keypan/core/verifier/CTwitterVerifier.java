package com.kbsriram.keypan.core.verifier;

import com.kbsriram.keypan.core.AProfile;
import com.kbsriram.keypan.core.CUtils;
import com.kbsriram.keypan.core.IGetter;
import com.kbsriram.keypan.core.IVerifier;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class CTwitterVerifier
    implements IVerifier
{
    /**
     * Return a profile from a key uid, and use the original query to
     * set the is-critical flag on the profile if necessary.
     * Return null if you cannot handle this uid.
     */
    public AProfile fromUid(String uid, String query)
    {
        Matcher m;
        if (!(m = s_urlre.matcher(uid)).find()) {
            return null;
        }
        String twitter_uid = m.group(1).toLowerCase();

        String query_uid;
        if ((m = s_urlre.matcher(query)).find()) {
            query_uid = m.group(1).toLowerCase();
        }
        else {
            query_uid = null;
        }

        return
            new Profile(twitter_uid)
            .setIsCritical(twitter_uid.equals(query_uid));
    }

    public AProfile verify(AProfile in, String fp, IGetter getter)
        throws IOException
    { return verify(in, fp, getter, false); }

    /**
     * Return the profile (with any additional info) if you were able
     * to find a confirming fingerprint. Optionally, look for fingerprints
     * in tweets as well. If none found, return null.
     */
    public AProfile verify
        (AProfile in, String fp, IGetter getter, boolean search_tweets)
        throws IOException
    {
        // 1. Try in the content/description of the profile.
        try {
            if (checkMainProfile(getter, in, CUtils.asPattern(fp))) {
                return in;
            }
        }
        catch (FileNotFoundException fne) {
            // No such profile, terminate search.
            return null;
        }

        if (!search_tweets) { return null; }

        // 2. Try searching for a tweet.
        String tweetid = maybeFind
            (getter, URLEncoder.encode
             ("from:"+in.getUid()+" \""+CUtils.groupedFingerprint(fp)+"\"",
              "utf-8"));

        if (tweetid == null) { return null; }

        // 3. Confirm we found the fingerprint in the title of the tweet.
        if (checkAndUpdate(getter, in, tweetid, CUtils.asPattern(fp))) {
            return in;
        }
        return null;
    }

    private final static boolean checkMainProfile
        (IGetter getter, AProfile in, Pattern p)
        throws IOException
    {
        BufferedInputStream bin = null;
        Document doc;
        String purlS = "https://twitter.com/"+in.getUid();
        URL purl;
        try {
            purl = new URL(purlS);
            getter.setTarget(purl);
            getter.setHeader
                ("User-Agent",
                 "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:31.0) Gecko/20100101 Firefox/31.0");
            getter.setHeader
                ("Referer", "https://twitter.com/search-home");
            bin = new BufferedInputStream(getter.openStream());
            doc = Jsoup.parse
                (bin, "utf-8", "https://twitter.com/"+in.getUid());
        }
        finally {
            if (bin != null) {
                try { bin.close(); }
                catch (Throwable ign) {}
            }
        }

        for (Element e: doc.select("head>meta[name=description]")) {
            String s = e.attr("content");
            if ((s != null) && p.matcher(s).find()) {
                in.setProfileURL(purl);
                in.setConfirmURL(purl);
                ((Profile)in).setConfirmTitle("their profile");
                return true;
            }
        }
        return false;
    }

    private final static String maybeFind(IGetter getter, String q)
        throws IOException
    {
        BufferedInputStream bin = null;
        Document doc;
        try {
            getter.setTarget(new URL("https://twitter.com/search?q="+q));
            getter.setHeader
                ("User-Agent",
                 "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:31.0) Gecko/20100101 Firefox/31.0");
            getter.setHeader
                ("Referer", "https://twitter.com/search-home");
            bin = new BufferedInputStream(getter.openStream());
            doc = Jsoup.parse
                (bin, "utf-8", "https://twitter.com/search");
        }
        finally {
            if (bin != null) {
                try { bin.close(); }
                catch (Throwable ign) {}
            }
        }

        for (Element e: doc.select("ol.stream-items>li[data-item-id]")) {
            return e.attr("data-item-id");
        }
        return null;
    }

    private final static boolean checkAndUpdate
        (IGetter getter, AProfile in, String tid, Pattern check)
        throws IOException
    {
        BufferedInputStream bin = null;
        Document doc;
        String target = "https://twitter.com/"+in.getUid()+"/status/"+tid;
        try {
            getter.setTarget(new URL(target));
            getter.setHeader
                ("User-Agent",
                 "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:31.0) Gecko/20100101 Firefox/31.0");
            getter.setHeader
                ("Referer", "https://twitter.com/"+in.getUid());
            bin = new BufferedInputStream(getter.openStream());
            doc = Jsoup.parse(bin, "utf-8", target);
        }
        finally {
            if (bin != null) {
                try { bin.close(); }
                catch (Throwable ign) {}
            }
        }

        boolean found = false;
        for (Element e: doc.select("html>head>title")) {
            if (check.matcher(e.text()).find()) {
                found = true;
                break;
            }
        }
        if (!found) { return false; }
        // Update content.
        in
            .setProfileURL(new URL("https://twitter.com/"+in.getUid()))
            .setConfirmURL(new URL(target));
        ((Profile)in).setConfirmTitle("this tweet");
        return true;
    }

    public final static class Profile extends AProfile
    {
        Profile(String uid)
        { super(uid); }
        @Override
        public String getSiteName()
        { return "Twitter"; }
        @Override
        public URL getSiteIcon()
        { return s_siteicon; }
        @Override
        public String getConfirmTitle()
        { return m_ctitle; }
        private Profile setConfirmTitle(String t)
        { m_ctitle = t; return this; }
        private String m_ctitle;
    }
    private final static Pattern s_urlre =
        Pattern.compile
        ("\\btwitter\\.com/([a-zA-Z0-9_]{1,15})", Pattern.CASE_INSENSITIVE);
    private static URL s_siteicon = null;
    static
    {
        try {
            s_siteicon = new URL("https://abs.twimg.com/favicons/favicon.ico");
        }
        catch (MalformedURLException mee) {}
    }
}
