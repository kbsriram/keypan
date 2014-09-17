package com.kbsriram.keypan.core;

import java.io.IOException;

public interface IVerifier
{
    /**
     * Return a profile from a key uid, and use the original query to
     * set the is-critical flag on the profile if necessary.
     * Return null if you cannot handle this uid.
     */
    public AProfile fromUid(String uid, String query);

    /**
     * Return the profile (with any additional info) if you were able
     * to find a confirming fingerprint. Otherwise, return null.
     */
    public AProfile verify(AProfile in, String fingerprint, IGetter getter)
        throws IOException;
}
