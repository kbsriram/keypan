package com.kbsriram.keypan.core.verifier;

import com.kbsriram.keypan.core.AProfile;
import com.kbsriram.keypan.core.CUtils;
import com.kbsriram.keypan.core.IGetter;
import com.kbsriram.keypan.core.IVerifier;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class CGithubVerifier
    implements IVerifier
{
    public AProfile fromUid(String uid, String query)
    {
        Matcher m;
        if (!(m = s_urlre.matcher(uid)).find()) {
            return null;
        }
        String github_uid = m.group(1).toLowerCase();

        String query_uid;
        if ((m = s_urlre.matcher(query)).find()) {
            query_uid = m.group(1).toLowerCase();
        }
        else {
            query_uid = null;
        }

        return
            new Profile(github_uid)
            .setIsCritical(github_uid.equals(query_uid));
    }

    /**
     * Return the profile (with any additional info) if you were able
     * to find a confirming fingerprint. Otherwise, return null.
     */
    public AProfile verify(AProfile in, String fp, IGetter getter)
        throws IOException
    {
        // 1a. try searching with grouped fingerprint.
        String gistid = maybeFind
            (getter, in.getUid(), URLEncoder.encode
             ("user:"+in.getUid()+" \""+CUtils.groupedFingerprint(fp)+"\"",
              "utf-8"));

        if (gistid == null) {
            // 1b. Try without grouping.
            gistid = maybeFind
                (getter, in.getUid(), URLEncoder.encode
                 ("user:"+in.getUid()+" "+fp, "utf-8"));
        }

        if (gistid == null) { return null; }

        // 2. Confirm we find the fingerprint in the title of the tweet.
        if (checkAndUpdate(getter, in, gistid, CUtils.asPattern(fp))) {
            return in;
        }
        return null;
    }

    private final static String maybeFind(IGetter getter, String uid, String q)
        throws IOException
    {
        BufferedInputStream bin = null;
        Document doc;
        try {
            getter.setTarget(new URL("https://gist.github.com/search?q="+q));
            getter.setHeader
                ("User-Agent",
                 "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:31.0) Gecko/20100101 Firefox/31.0");
            getter.setHeader
                ("Referer", "https://gist.github.com/search");
            bin = new BufferedInputStream(getter.openStream());
            doc = Jsoup.parse
                (bin, "utf-8", "https://gist.github.com/search");
        }
        finally {
            if (bin != null) {
                try { bin.close(); }
                catch (Throwable ign) {}
            }
        }

        String selector = "div.gist-item span.creator a[href^=/"+uid+"/";
        Pattern p = Pattern.compile("/"+uid+"/([a-f0-9]+)");

        for (Element e: doc.select(selector)) {
            Matcher m = p.matcher(e.attr("href"));
            if (m.matches()) {
                return m.group(1);
            }
        }
        return null;
    }

    private final static boolean checkAndUpdate
        (IGetter getter, AProfile in, String gid, Pattern check)
        throws IOException
    {
        Reader rdr = null;
        JSONObject js;
        String target = "https://api.github.com/gists/"+gid;
        try {
            getter.setTarget(new URL(target));
            getter.setHeader("User-Agent", "github.com/kbsriram/keypan");
            rdr = new InputStreamReader(getter.openStream());
            js = new JSONObject(CUtils.asString(rdr));
        }
        catch (JSONException jse) {
            throw new IOException(jse);
        }
        finally {
            if (rdr != null) {
                try { rdr.close(); }
                catch (Throwable ign) {}
            }
        }

        try { return checkAndUpdateJSON(js, in, check); }
        catch (JSONException jse) {
            throw new IOException(jse);
        }
    }

    @SuppressWarnings("unchecked")
    private final static boolean checkAndUpdateJSON
        (JSONObject js, AProfile in, Pattern check)
        throws MalformedURLException, JSONException
    {
        // Confirm we're the correct user.
        JSONObject owner = js.optJSONObject("owner");
        if (owner == null) { return false; }
        if (!in.getUid().equalsIgnoreCase
            (owner.optString("login"))) {
            return false;
        }

        // I expect to see a single file here.
        JSONObject files = js.optJSONObject("files");
        if (files == null) { return false; }
        if (files.length() != 1) { return false; }
        Iterator<String> it = files.keys();
        JSONObject file = files.optJSONObject(it.next());
        if (file == null) { return false; }

        // I expect the content to be "small", for some arbitrary
        // definition of small.
        int size = file.optInt("size", -1);
        if ((size <= 0) || (size > LIMIT_GIST_CONTENT_SIZE)) {
            return false;
        }

        String content = file.optString("content");
        if (content == null) { return false; }
        // Did we find the fingerprint in the pattern?
        if (!check.matcher(content).find()) { return false; }

        // Great! Update interesting content in the profile and return it.
        in.setConfirmURL(new URL(js.getString("html_url")));
        in.setProfileURL(new URL(owner.getString("html_url")));
        in.setIconURL(new URL(owner.getString("avatar_url")));
        return true;
    }

    public final static class Profile extends AProfile
    {
        Profile(String uid)
        { super(uid); }
        @Override
        public String getSiteName()
        { return "Github"; }
        @Override
        public URL getSiteIcon()
        { return s_siteicon; }
        @Override
        public String getConfirmTitle()
        { return "this gist"; }
    }
    private final static Pattern s_urlre =
        Pattern.compile
        ("\\bgithub\\.com/([a-zA-Z0-9_]{2,15})", Pattern.CASE_INSENSITIVE);
    private final static int LIMIT_GIST_CONTENT_SIZE = 8192;
    private static URL s_siteicon = null;
    static
    {
        try {
            s_siteicon = new URL("https://github.com/apple-touch-icon-144.png");
        }
        catch (MalformedURLException mee) {}
    }
}
