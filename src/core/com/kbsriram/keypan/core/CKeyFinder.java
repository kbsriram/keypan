package com.kbsriram.keypan.core;

// Pulls all the bits of code together to do the actual work of
// finding, and then validating keys.
import com.kbsriram.openpgp.CPGPUtils;
import com.kbsriram.keypan.core.verifier.CGithubVerifier;
import com.kbsriram.keypan.core.verifier.CGooglePlusVerifier;
import com.kbsriram.keypan.core.verifier.CTwitterVerifier;
import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;

public class CKeyFinder
{
    public final static List<Result> find(String q, IGetter getter)
        throws IOException
    {
        // Find candidates at the keyserver.
        List<Result> results = new ArrayList<Result>();
        for (CHkpSearch.Info candidate: CHkpSearch.lookup(q, getter)) {
            results.addAll(validate(candidate, q, getter));
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private final static List<Result> validate
        (CHkpSearch.Info candidate, String q, IGetter getter)
        throws IOException
    {
        List<Result> ret = new ArrayList<Result>();

        if (shouldSkip(candidate, q)) { return ret; }

        // 1. pull down keys from the candidate.
        PGPPublicKeyRingCollection pkrc = CHkpSearch.get
            (candidate.getKeyId(), getter);
        if ((pkrc == null) || (pkrc.size() <= 0)) {
            return ret;
        }
        // 2. Validate each keyring.
        Iterator<PGPPublicKeyRing> pkrit = pkrc.getKeyRings();
        while (pkrit.hasNext()) {
            Result r = validateKeyRing(pkrit.next(), q, getter);
            if (r != null) { ret.add(r); }
        }
        return ret;
    }

    // Quick filter to see whether we even need to continue with this
    // candidate.
    private final static boolean shouldSkip
        (CHkpSearch.Info candidate, String q)
    {
        boolean skip = true;
        for (String uid: candidate.getUids()) {
            for (IVerifier v: s_verifiers) {
                if (v.fromUid(uid, q) != null) {
                    skip = false;
                    break;
                }
            }
            if (!skip) { break; }
        }
        return skip;
    }

    private final static Result validateKeyRing
        (PGPPublicKeyRing unknown_pkr, String q, IGetter getter)
        throws IOException
    {
        CPGPUtils.PKR pkr;
        try { pkr = CPGPUtils.validate(unknown_pkr, null); }
        catch (PGPException pge) {
            CUtils.logw(CKeyFinder.class, "invalid key", pge);
            return null;
        }
        catch (SignatureException sige) {
            CUtils.logw(CKeyFinder.class, "invalid key", sige);
            return null;
        }

        if (pkr.getStatus() != CPGPUtils.PKR.Status.OK) {
            return null;
        }

        List<AProfile> confirmed = new ArrayList<AProfile>();
        // For each valid uid in the keyring, check whether
        // we have a confirming profile.
        String fp = CUtils.byte2hex
            (pkr.getOriginal().getPublicKey().getFingerprint());

        for (CPGPUtils.UserID uid: pkr.getUserIDs()) {
            for (IVerifier v: s_verifiers) {
                AProfile candidate = v.fromUid(uid.getName(), q);
                if (candidate == null) { continue; }
                AProfile verified = v.verify(candidate, fp, getter);
                if (verified != null) {
                    confirmed.add(verified);
                }
                else if (candidate.isCritical()) {
                    CUtils.logw
                        (CKeyFinder.class,
                         "Required confirmation for '"+uid.getName()+
                         "' is missing");
                    return null;
                }
            }
        }

        if (confirmed.size() == 0) { return null; }
        return new Result(pkr, confirmed);
    }

    public final static class Result
    {
        private Result(CPGPUtils.PKR pkr, List<AProfile> confirmed)
        {
            m_pkr = pkr;
            m_confirmed = confirmed;
        }
        public CPGPUtils.PKR getPKR()
        { return m_pkr; }
        public List<AProfile> getConfirmations()
        { return m_confirmed; }
        private final CPGPUtils.PKR m_pkr;
        private final List<AProfile> m_confirmed;
    }

    private final static IVerifier[] s_verifiers = new IVerifier[] {
        new CGithubVerifier(),
        new CGooglePlusVerifier(),
        new CTwitterVerifier()
    };
}
