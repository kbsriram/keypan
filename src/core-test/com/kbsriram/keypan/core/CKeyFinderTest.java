package com.kbsriram.keypan.core;

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class CKeyFinderTest
{
    @Test public void testGood()
        throws Exception
    {
        CDefaultGetter getter = new CDefaultGetter();

        List<CKeyFinder.Result> results =
            CKeyFinder.find("github.com/kbsriram", getter);
        assertEquals(1, results.size());
        CKeyFinder.Result result = results.get(0);
        assertEquals(3, result.getConfirmations().size());
    }
}
