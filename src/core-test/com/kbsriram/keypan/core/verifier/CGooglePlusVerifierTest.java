package com.kbsriram.keypan.core.verifier;

import com.kbsriram.keypan.core.AProfile;
import com.kbsriram.keypan.core.CDefaultGetter;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class CGooglePlusVerifierTest
{
    @Test public void uidRecognition()
    {
        CGooglePlusVerifier v = new CGooglePlusVerifier();
        AProfile p = v.fromUid("https://plus.google.com/+Kbsriram", "abc");
        assertNotNull(p);
        assertEquals("+kbsriram", p.getUid());
        assertFalse(p.isCritical());

        p = v.fromUid("https://plus.google.com/12345", "abc");
        assertNotNull(p);
        assertEquals("12345", p.getUid());
        assertFalse(p.isCritical());

        p = v.fromUid("https://plus.google.com/+KBSRIRAM", "plus.google.com/+Kbsriram");
        assertNotNull(p);
        assertEquals("+kbsriram", p.getUid());
        assertTrue(p.isCritical());

        p = v.fromUid("https://twitter.com/kbsriram", "plus.google.com/+kbsriram");
        assertNull(p);
    }

    @Test public void notFound()
        throws IOException
    {
        CGooglePlusVerifier v = new CGooglePlusVerifier();
        AProfile p = v.fromUid("https://plus.google.com/+saotnueh29", "abc");
        assertNotNull(p);
        assertNull
            (v.verify(p, "0b1491929806596254700155fd720ad9eba34b1c",
                      new CDefaultGetter()));
    }

    @Test public void search()
        throws IOException
    {
        CGooglePlusVerifier v = new CGooglePlusVerifier();
        AProfile p = v.fromUid("https://plus.google.com/+kBsrIram", "kbs");
        assertNotNull(p);
        assertEquals("+kbsriram", p.getUid());
        assertFalse(p.isCritical());

        AProfile found =
            v.verify(p, "bf71a5e8e8cd553bde86096962f463c673f6c01f",
                     new CDefaultGetter());
        assertNotNull(found);
        assertEquals
            ("KB Sriram",
             found.getDisplayName());
        assertEquals
            ("https://plus.google.com/+KBSriram/about",
             found.getConfirmURL().toString());
        assertEquals
            ("https://plus.google.com/+KBSriram",
             found.getProfileURL().toString());
        assertNotNull(found.getIconURL());
    }
}
