package com.kbsriram.keypan.core.verifier;

import com.kbsriram.keypan.core.AProfile;
import com.kbsriram.keypan.core.CDefaultGetter;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class CGithubVerifierTest
{
    @Test public void uidRecognition()
    {
        CGithubVerifier v = new CGithubVerifier();
        AProfile p = v.fromUid("https://github.com/Kbsriram", "abc");
        assertNotNull(p);
        assertEquals("kbsriram", p.getUid());
        assertFalse(p.isCritical());

        p = v.fromUid("https://github.com/KBSRIRAM", "github.com/Kbsriram");
        assertNotNull(p);
        assertEquals("kbsriram", p.getUid());
        assertTrue(p.isCritical());

        p = v.fromUid("https://twitter.com/kbsriram", "github.com/kbsriram");
        assertNull(p);
    }

    @Test public void notFound()
        throws IOException
    {
        CGithubVerifier v = new CGithubVerifier();
        AProfile p = v.fromUid("https://github.com/saotnueh29", "abc");
        assertNotNull(p);
        assertNull
            (v.verify(p, "0b1491929806596254700155fd720ad9eba34b1c",
                      new CDefaultGetter()));
    }

    @Test public void search()
        throws IOException
    {
        CGithubVerifier v = new CGithubVerifier();
        AProfile p = v.fromUid("https://github.com/kBsrIram", "kbs");
        assertNotNull(p);
        assertEquals("kbsriram", p.getUid());
        assertFalse(p.isCritical());

        AProfile found =
            v.verify(p, "bf71a5e8e8cd553bde86096962f463c673f6c01f",
                     new CDefaultGetter());
        assertNotNull(found);
        assertEquals
            ("https://gist.github.com/c05dca103a252ac0d6ac",
             found.getConfirmURL().toString());
        assertEquals
            ("https://github.com/kbsriram",
             found.getProfileURL().toString());
    }
}
