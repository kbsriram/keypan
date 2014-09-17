package com.kbsriram.keypan.core;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Pattern;

public final class CUtils
{
    public final static String byte2hex(byte[] b)
    {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<b.length; i++) {
            int v = b[i] & 0xff;
            sb.append(s_byte2hex[v >> 4]);
            sb.append(s_byte2hex[v & 0xf]);
        }
        return sb.toString();
    }

    public final static String asString(Reader r)
        throws IOException
    {
        char[] buf = new char[2048];
        int nread;
        StringBuilder sb = new StringBuilder();
        while ((nread = r.read(buf)) > 0) {
            sb.append(buf, 0, nread);
        }
        return sb.toString();
    }

    public final static String groupedFingerprint(String fp)
    {
        if (fp.length() != 40) {
            throw new IllegalArgumentException("bad fp");
        }
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<40; i+=4) {
            if (i > 0) { sb.append(" "); }
            sb.append(fp.substring(i, i+4));
        }
        return sb.toString().toLowerCase();
    }

    public final static Pattern asPattern(String fp)
    {
        if (fp.length() != 40) {
            throw new IllegalArgumentException("bad fp");
        }
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<40; i+=4) {
            if (i > 0) { sb.append("[\\p{Z}\\s]*"); }
            sb.append(fp.substring(i, i+4));
        }
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }

    public final static <T> void lognote(Class<T> cls, String msg)
    {
        if (s_debug) {
            System.out.println("NOTE: "+cls.getSimpleName()+": "+msg);
            return;
        }
        int mlen = msg.length();
        if (mlen > MAX_NOTE_LEN) {
            msg = msg.substring(0, MAX_NOTE_LEN-3)+"...";
            mlen = MAX_NOTE_LEN;
        }
        System.out.print(msg);
        int delta = (MAX_NOTE_LEN - msg.length());
        while (delta > 0) {
            System.out.print(" ");
            delta--;
        }
        if (s_cli) { System.out.print("\r"); }
        else { System.out.println(); }
        System.out.flush();
    }

    public final static void clear()
    {
        if (s_debug) { return; }
        for (int i=0; i<MAX_NOTE_LEN; i++) {
            System.out.print(" ");
        }
        System.out.print("\r");
        System.out.flush();
    }
    public final static String nullIfEmpty(String s)
    {
        if ((s == null) || (s.length() == 0)) {
            return null;
        }
        else {
            return s;
        }
    }

    public final static void setDebug(boolean v)
    { s_debug = v; }
    public final static void setCLI(boolean v)
    { s_cli = v; }
    public final static boolean isDebug()
    { return s_debug; }
    public final static <T> void logw(Class<T> cls, String msg)
    { logw(cls, msg, null); }
    public final static <T> void logw(Class<T> cls, String msg, Throwable th)
    {
        System.err.println("WARN: "+cls.getSimpleName()+": "+msg);
        if (th != null) { th.printStackTrace(); }
    }
    public final static <T> void logd(Class<T> cls, String msg)
    { logd(cls, msg, null); }
    public final static <T> void logd(Class<T> cls, String msg, Throwable th)
    {
        if (s_debug) {
            System.err.println("DEBUG: "+cls.getSimpleName()+": "+msg);
            if (th != null) { th.printStackTrace(); }
        }
    }
    private static boolean s_debug = false;
    private static boolean s_cli = true;
    private final static int MAX_NOTE_LEN = 35;
    private final static char[] s_byte2hex = new char[] {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };
}
