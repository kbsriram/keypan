package com.kbsriram.keypan.core;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class CHkpSearchTest
{
    @Test public void testGood()
        throws IOException, PGPException
    {
        CDefaultGetter getter = new CDefaultGetter();

        List<CHkpSearch.Info> results =
            CHkpSearch.lookup("github.com/kbsriram", getter);

        assertEquals(1, results.size());
        CHkpSearch.Info info = results.get(0);
        assertTrue(info.getKeyId().length() >= 8);
        assertTrue
            ("bf71a5e8e8cd553bde86096962f463c673f6c01f"
             .endsWith(info.getKeyId()));
        checkPkrc(info.getKeyId(), getter);
    }

    @SuppressWarnings("unchecked")
    private void checkPkrc(String kid, IGetter getter)
        throws PGPException, IOException
    {
        PGPPublicKeyRingCollection pkrc = CHkpSearch.get(kid, getter);
        assertNotNull(pkrc);
        assertEquals(1, pkrc.size());
        Iterator<PGPPublicKeyRing> it = pkrc.getKeyRings();
        PGPPublicKeyRing pkr = it.next();
        assertTrue
            (CUtils.byte2hex(pkr.getPublicKey().getFingerprint())
             .endsWith(kid));
    }
}
