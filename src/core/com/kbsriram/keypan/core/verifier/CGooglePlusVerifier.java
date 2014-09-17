package com.kbsriram.keypan.core.verifier;

import com.kbsriram.keypan.core.AProfile;
import com.kbsriram.keypan.core.CUtils;
import com.kbsriram.keypan.core.IGetter;
import com.kbsriram.keypan.core.IVerifier;
import java.io.IOException;
import java.io.FileNotFoundException;
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

public class CGooglePlusVerifier
    implements IVerifier
{
    public AProfile fromUid(String uid, String query)
    {
        Matcher m;
        if (!(m = s_urlre.matcher(uid)).find()) {
            return null;
        }
        String plus_uid = m.group(1).toLowerCase();

        String query_uid;
        if ((m = s_urlre.matcher(query)).find()) {
            query_uid = m.group(1).toLowerCase();
        }
        else {
            query_uid = null;
        }

        return
            new Profile(plus_uid)
            .setIsCritical(plus_uid.equals(query_uid));
    }

    /**
     * Return the profile (with any additional info) if you were able
     * to find a confirming fingerprint. Otherwise, return null.
     */
    public AProfile verify(AProfile in, String fp, IGetter getter)
        throws IOException
    {
        // Search the profile/aboutme content.
        if (checkAndUpdate(getter, in, CUtils.asPattern(fp))) {
            return in;
        }
        return null;
    }

    private final static boolean checkAndUpdate
        (IGetter getter, AProfile in, Pattern check)
        throws IOException
    {
        Reader rdr = null;
        JSONObject js;
        String target = "https://www.googleapis.com/plus/v1/people/"+
            in.getUid()+"?fields=name,displayName,tagline,aboutMe,image,url&key="+API_KEY;
        try {
            getter.setTarget(new URL(target));
            getter.setHeader("User-Agent", "github.com/kbsriram/keypan");
            getter.setHeader("Referer", "https://plus.google.com");
            rdr = new InputStreamReader(getter.openStream());
            js = new JSONObject(CUtils.asString(rdr));
        }
        catch (JSONException jse) {
            throw new IOException(jse);
        }
        catch (FileNotFoundException fne) {
            return false;
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
        if (!foundPattern(js.optString("aboutMe"), check) &&
            !foundPattern(js.optString("tagline"), check)) {
            return false;
        }
        // Great! Update interesting content in the profile and return it.
        in.setConfirmURL(new URL(js.getString("url")+"/about"));
        in.setProfileURL(new URL(js.getString("url")));
        String v = js.optString("displayName");
        if (v != null) { in.setDisplayName(v); }
        JSONObject image = js.optJSONObject("image");
        if (image != null) {
            v = image.optString("url");
            if (v != null) { in.setIconURL(new URL(v)); }
        }
        return true;
    }

    private final static boolean foundPattern(String content, Pattern check)
    {
        if (content == null) { return false; }
        // Bug with json - not correctly reading utf-8 encoding,
        // specifically non-breaking space. So, just remove anything
        // that's not ascii-ish. sigh.
        content = content.replaceAll("[^\\u0000-\\u007F]", "");
        return check.matcher(content).find();
    }

    public final static class Profile extends AProfile
    {
        Profile(String uid)
        { super(uid); }
        @Override
        public String getSiteName()
        { return "Google+"; }
        @Override
        public URL getSiteIcon()
        { return s_siteicon; }
        @Override
        public String getConfirmTitle()
        { return "their profile"; }
    }
    private final static Pattern s_urlre =
        Pattern.compile
        ("\\bplus\\.google\\.com/(\\+?[a-zA-Z0-9'._-]{3,})", Pattern.CASE_INSENSITIVE);
    // Yes, it's here in plain sight and anyone can abuse it by
    // exhausting quotas etc. Please dont :-)
    private final static String API_KEY =
        "AIzaSyAzvQwEhJyx4olnEfJLXaqtjUrsu8lnoiA";
    private static URL s_siteicon = null;
    static
    {
        try {
            s_siteicon = new URL
                ("https://ssl.gstatic.com/s2/oz/images/faviconr3.ico");
        }
        catch (MalformedURLException mee) {}
    }
}
