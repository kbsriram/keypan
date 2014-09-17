package com.kbsriram.keypan.servlet;

import com.kbsriram.keypan.core.AProfile;
import com.kbsriram.keypan.core.CDefaultGetter;
import com.kbsriram.keypan.core.CKeyFinder;
import com.kbsriram.keypan.core.CUtils;
import com.kbsriram.openpgp.CPGPUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.attr.ImageAttribute;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPUserAttributeSubpacketVector;
import org.bouncycastle.util.encoders.Base64;
import org.tdom.TDom;
import static org.tdom.TDom.*;

@SuppressWarnings("serial")
public final class CLookupServlet extends HttpServlet
{
    @Override
    public void init(ServletConfig config)
        throws ServletException
    {
        super.init(config);
        CUtils.setCLI(false);
    }

    @Override
    public void service
        (HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        String q = CUtils.nullIfEmpty(req.getParameter("q"));
        if (q == null) {
            write(resp, null, "keypan", null);
            return;
        }

        try { doit(q, resp); }
        catch (Exception ex) {
            ex.printStackTrace();
            error(resp, ex.getMessage());
        }
    }

    private final static void doit(String q, HttpServletResponse resp)
        throws Exception
    {
        List<CKeyFinder.Result> results =
            CKeyFinder.find(q, new CDefaultGetter());

        if (results.size() == 0) {
            error(resp, "No linked keys found for '"+q+"'");
            return;
        }

        TNode outer = n("div");
        for (CKeyFinder.Result result: results) {
            outer.append(makeProfile(result));
        }
        write(resp, outer, q+" - keypan", q);
    }

    private final static TNode makeProfile(CKeyFinder.Result result)
    {
        TNode proot = n("div", a("class", "profile_container"));

        TNode pkey =
            n("div", a("class", "profile_key"));
        proot.append(pkey);

        CPGPUtils.PKR pkr = result.getPKR();
        PGPPublicKey pk = pkr.getOriginal().getPublicKey();

        for (CPGPUtils.UserAttribute ua: pkr.getUserAttributes()) {
            ImageAttribute iattr = ua.getUserAttribute().getImageAttribute();
            if (iattr != null) {
                pkey.append
                    (n("img", a("class", "profile_key_image"),
                       a("src", asDataUri(iattr.getImageData()))));
            }
        }

        for (CPGPUtils.UserID uid: pkr.getUserIDs()) {
            pkey.append
                (n("div", a("class", "profile_key_name"),
                   t(uid.getName())));
        }
        pkey.append(n("div", a("class", "spacer")));

        for (AProfile profile: result.getConfirmations()) {
            TNode tnode = formatConfirmation(profile);
            if (tnode != null) { pkey.append(tnode); }
        }

        pkey.append
            (n("div", a("class", "profile_key_fp"),
               t(formatFingerprint(pk.getFingerprint()))));

        proot.append
            (n("div", a("class", "key_save"),
               n("a", a("href", CKeySaveServlet.save(pkr.getOriginal())),
                 t("Save this key"))));
        return proot;
    }

    private final static TNode formatConfirmation(AProfile confirm)
    {
        TNode ret =
            n("div", a("class", "profile_key_confirm"));
        URL icon = confirm.getSiteIcon();
        if (icon != null) {
            ret.append
                (n("img",
                   a("src", icon.toString()),
                   a("class", "site_icon")));
        }

        ret
            .append(t("Linked on "+confirm.getSiteName()+" by "))
            .append(n("a", a("href", confirm.getProfileURL().toString()),
                      t(confirm.getDisplayName())))
            .append(t(" from "))
            .append(n("a", a("href", confirm.getConfirmURL().toString()),
                      t(confirm.getConfirmTitle())))
            .append(t("."));
        return ret;
    }


    private final static String asDataUri(byte data[])
    { return "data:image/jpeg;base64,"+Base64.toBase64String(data); }

    private final static String formatFingerprint(byte[] fp)
    {
        String s = CUtils.byte2hex(fp).toUpperCase();
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<s.length(); i+=4) {
            sb.append(s.substring(i, i+4));
            if (i < 36) { sb.append(" "); }
            if (i == 16) { sb.append(" "); }
        }
        return sb.toString();
    }

    final static void error(HttpServletResponse resp, String msg)
        throws IOException
    { write(resp, n("div", a("class", "error"), t(msg)), "Error", null); }

    final static void write
        (HttpServletResponse resp,
         TNode content, String title, String q)
        throws IOException
    {
        if (content == null) { content = n("div"); }
        TNode qinput =
            n("input",
              a("name", "q"),
              a("type", "text"),
              a("autocomplete", "off"),
              a("spellcheck", "false"));

        if (q == null) {
            qinput.append(a("placeholder", "enter profile url"));
        }
        else {
            if (q.startsWith("http://")) {
                q = q.substring("http://".length());
            }
            else if (q.startsWith("https://")) {
                q = q.substring("https://".length());
            }
            qinput.append(a("value", q));
        }

        resp.getWriter().println("<!DOCTYPE html>");
        TNode html =
            n("html",
              a("lang", "en"),
              a("xmlns", "http://www.w3.org/1999/xhtml"),
              n("head",
                n("title", t(title)),
                n("meta", a("charset", "utf-8")),
                n("meta",
                  a("name", "viewport"),
                  a("content", "device-width,initial-scale=1.0")),
                n("link",
                  a("rel", "stylesheet"),
                  a("href", "media/css/main.css"))),
              n("body",
                n("form",
                  a("action", "/lookup"),
                  a("method", "get"),
                  qinput,
                  n("input",
                    a("type", "submit"),
                    a("value", " Search "))),
                n("div", a("class", "spacer")),
                content));

        html.dump(resp.getWriter());
    }
}
