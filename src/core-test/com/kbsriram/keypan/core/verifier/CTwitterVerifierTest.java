package com.kbsriram.keypan.core.verifier;

import com.kbsriram.keypan.core.AProfile;
import com.kbsriram.keypan.core.CDefaultGetter;
import com.kbsriram.keypan.core.CUtils;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class CTwitterVerifierTest
{
    @Test public void uidRecognition()
    {
        CUtils.setDebug(true);
        CTwitterVerifier v = new CTwitterVerifier();
        AProfile p = v.fromUid("https://twitter.com/kbsriram", "abc");
        assertNotNull(p);
        assertEquals("kbsriram", p.getUid());
        assertFalse(p.isCritical());

        p = v.fromUid("https://twitter.com/kbsriram", "twitter.com/kbsriram");
        assertNotNull(p);
        assertEquals("kbsriram", p.getUid());
        assertTrue(p.isCritical());

        p = v.fromUid("https://github.com/kbsriram", "twitter.com/kbsriram");
        assertNull(p);
    }

    @Test public void notFound()
        throws IOException
    {
        CUtils.setDebug(true);
        CTwitterVerifier v = new CTwitterVerifier();
        AProfile p = v.fromUid("https://twitter.com/saotnueh", "abc");
        assertNotNull(p);
        assertNull
            (v.verify(p, "0b1491929806596254700155fd720ad9eba34b1c",
                      new CDefaultGetter()));
    }

    @Test public void confirmProfile()
        throws IOException
    {
        CUtils.setDebug(true);
        CTwitterVerifier v = new CTwitterVerifier();
        AProfile p = v.fromUid("https://twitter.com/galanglawoffice", "abc");
        assertNotNull(p);
        assertEquals("galanglawoffice", p.getUid());
        assertFalse(p.isCritical());

        AProfile found =
            v.verify(p, "07aa3d6eb623798c2ef16997547a89aa9b3fa75d",
                     new CDefaultGetter());
        assertNotNull(found);
        assertNotNull(found.getConfirmURL());
        assertNotNull(found.getProfileURL());
        assertEquals("https://twitter.com/galanglawoffice",
                     found.getConfirmURL().toString());
    }

    @Test public void confirmTweet()
        throws IOException
    {
        CUtils.setDebug(true);
        CTwitterVerifier v = new CTwitterVerifier();
        AProfile p = v.fromUid("https://twitter.com/kbsriram", "abc");
        assertNotNull(p);
        assertEquals("kbsriram", p.getUid());
        assertFalse(p.isCritical());

        AProfile found =
            v.verify(p, "bf71a5e8e8cd553bde86096962f463c673f6c01f",
                     new CDefaultGetter());
        assertNotNull(found);
        assertNotNull(found.getConfirmURL());
        assertNotNull(found.getProfileURL());
        assertEquals("https://twitter.com/kbsriram",
                     found.getConfirmURL().toString());
    }
}
