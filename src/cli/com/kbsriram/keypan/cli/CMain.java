package com.kbsriram.keypan.cli;

import com.kbsriram.keypan.core.AProfile;
import com.kbsriram.keypan.core.CDefaultGetter;
import com.kbsriram.keypan.core.CKeyFinder;
import com.kbsriram.keypan.core.CUtils;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import org.bouncycastle.bcpg.ArmoredOutputStream;

public class CMain
{
    public static void main(String args[])
        throws Exception
    {
        if (args.length == 0) {
            help(null);
            return;
        }

        String q = null;
        File out = null;

        for (int i=0; i<args.length; i++) {
            String arg = args[i];
            if ("--help".equals(arg)) {
                help(null);
                return;
            }
            if ("--debug".equals(arg)) {
                CUtils.setDebug(true);
            }
            else if (arg.startsWith("--")) {
                help("Unknown option "+arg);
                return;
            }
            else {
                if (q != null) {
                    help("Specify exactly one query");
                    return;
                }
                q = arg;
            }
        }
        if (q == null) {
            help("Missing query argument");
            return;
        }
        doit(q);
    }

    private final static String readString(String prompt, String dflt)
    {
        String ret = null;
        while (true) {
            if (dflt != null) {
                ret = readLine("%s [%s]: ", prompt, dflt);
            }
            else {
                ret = readLine("%s: ", prompt);
            }

            if (ret == null) { return null; } // eof

            ret = ret.trim();
            if (ret.length() > 0) {
                return ret;
            }
            else {
                // hit return for default
                if (dflt != null) { return dflt; }
            }
        }
    }

    private final static boolean isYes(String prompt, boolean dflt)
    {
        String resp;
        while (true) {
            if (dflt) {
                resp = readLine("%s? (Y/n) ", prompt);
            }
            else {
                resp = readLine("%s? (y/N) ", prompt);
            }
            if (resp == null) { return false; }
            resp = resp.trim();
            if (resp.length() == 0) { return dflt; }
            if ("y".equalsIgnoreCase(resp)) { return true; }
            if ("n".equalsIgnoreCase(resp)) { return false; }
            System.out.println("Please type in y or n");
        }
    }

    private final static String readLine(String prompt, Object... args)
    {
        String m = String.format(prompt, args);
        System.out.print(m);
        System.out.flush();
        try { return s_reader.readLine(); }
        catch (IOException ioe) { return null; }
    }

    private final static void doit(String q)
        throws Exception
    {
        List<CKeyFinder.Result> results =
            CKeyFinder.find(q, new CDefaultGetter());
        CUtils.clear();
        if (results.size() == 0) {
            System.out.println
                ("No confirmed keys found for '"+q+"'");
            System.exit(1);
            return;
        }
        System.out.print("Found ");
        if (results.size() > 1) {
            System.out.print(results.size()+ "keys");
        }
        else {
            System.out.print("key");
        }
        System.out.println(" for '"+q+"'");
        boolean first = true;
        for (CKeyFinder.Result result: results) {
            if (first) { first = false; }
            else { System.out.println(); }
            byte[] fp =
                result.getPKR().getOriginal().getPublicKey().getFingerprint();
            System.out.print("PGP fingerprint: ");
            printFingerprint(fp);
            for (AProfile confirmation: result.getConfirmations()) {
                printConfirmation(confirmation);
            }
            if (isYes("Save this key", false)) {
                saveKey(result);
            }
        }
    }

    private final static void saveKey(CKeyFinder.Result result)
    {
        String path;
        do {
            path = readString("Save as", suggestName(result));
            if (path == null) { return; }
        } while (!saveKeyToFile(result, new File(path)));
    }

    private final static String suggestName(CKeyFinder.Result result)
    {
        // Try the first uid on the list?
        for (AProfile profile: result.getConfirmations()) {
            String name = profile.getDisplayName();
            if (name == null) { name = profile.getUid(); }
            return safeName(name)+".asc";
        }
        return "public.asc";
    }

    private final static String safeName(String s)
    {
        if ((s == null) || (s.length() == 0)) { return "public"; }
        StringBuilder sb = new StringBuilder();
        for (char c: s.toCharArray()) {
            if (((c >= 'a') && (c <= 'z')) ||
                ((c >= 'A') && (c <= 'Z')) ||
                ((c >= '0') && (c <= '9')) ||
                (c == '-') || (c == '_') || (c == '.')) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private final static boolean saveKeyToFile
        (CKeyFinder.Result result, File path)
    {
        if (path.canRead()) {
            System.out.println(path+" already exists, choose another path.");
            return false;
        }

        ArmoredOutputStream out = null;
        boolean ok = false;
        try {
            out = new ArmoredOutputStream
                (new BufferedOutputStream
                 (new FileOutputStream(path)));
            result.getPKR().getOriginal().encode(out);
            out.flush();
            ok = true;
        }
        catch (Throwable any) {
            System.out.println
                ("Could not save : "+any.toString());
        }
        finally {
            if (out != null) {
                try { out.close(); }
                catch (Throwable ign) {}
            }
            if (!ok) { path.delete(); }
        }
        return ok;
    }

    private final static void printFingerprint(byte[] fp)
    {
        String s = CUtils.byte2hex(fp).toUpperCase();
        for (int i=0; i<s.length(); i+=4) {
            System.out.print(s.substring(i, i+4));
            if (i < 36) { System.out.print(" "); }
            if (i == 16) { System.out.print(" "); }
        }
        System.out.println();
    }

    private final static void printConfirmation(AProfile p)
    {
        System.out.println
            ("linked on "+p.getSiteName()+" by "+p.getDisplayName()+
             " <"+p.getProfileURL()+">");
        System.out.println
            ("    from "+p.getConfirmTitle()+" <"+p.getConfirmURL()+">");
    }

    private final static void help(String msg)
    {
        if (msg != null) { System.out.println(msg); }
        System.out.println
            ("Usage: java -jar keypan-cli.jar <query>");
        System.out.println
            ("    [--debug]");
        System.out.println
            ("Examples:");
        System.out.println
            ("Find keys associated with a github profile.");
        System.out.println
            ("    java -jar keypan-cli.jar github.com/kbsriram");
        System.out.println
            ("Find keys associated with a google plus profile.");
        System.out.println
            ("    java -jar keypan-cli.jar plus.google.com/+kbsriram");
        System.out.println
            ("Find keys associated with a twitter profile.");
        System.out.println
            ("    java -jar keypan-cli.jar twitter.com/kbsriram");
    }
    private final static BufferedReader s_reader =
        new BufferedReader(new InputStreamReader(System.in));
}
