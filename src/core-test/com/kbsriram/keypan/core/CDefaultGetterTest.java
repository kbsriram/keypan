package com.kbsriram.keypan.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Pattern;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class CDefaultGetterTest
{
    @Test public void quickChecks()
        throws IOException
    {
        Pattern p = asPattern("bf71a5e8e8cd553bde86096962f463c673f6c01f");

        checkFP("https://gist.github.com/kbsriram/c05dca103a252ac0d6ac", p);
        checkFP("https://plus.google.com/+KBSriram/about", p);
        p = asPattern("0b1491929806596254700155fd720ad9eba34b1c");
        checkFP
            ("https://twitter.com/micahflee/status/464558531983994880", p);
    }

    private final static Pattern asPattern(String fp)
    {
        if (fp.length() != 40) {
            throw new IllegalArgumentException("bad fp");
        }
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<40; i+=4) {
            if (i > 0) { sb.append("[\\s\u00a0]*"); }
            sb.append(fp.substring(i, i+4));
        }
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }

    private void checkFP(String target, Pattern p)
        throws IOException
    {
        CDefaultGetter getter = new CDefaultGetter();
        getter.setTarget(new URL(target));
        getter.setHeader("User-Agent", "keypan");

        BufferedReader br = null;
        boolean ok = false;
        try {
            br =
                new BufferedReader
                (new InputStreamReader
                 (getter.openStream()));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.replaceAll("[^ -~]", "");
                if (p.matcher(line).find()) {
                    ok = true;
                }
                else {
                    // System.out.println(line);
                }
            }
        }
        finally {
            if (br != null) {
                try { br.close(); }
                catch (Throwable th) {}
            }
        }
        assertTrue(ok);
    }
}
