package com.fsck.k9.mail.store;

import javax.net.ssl.X509TrustManager;
import com.fsck.k9.K9;
import java.io.File;
import java.lang.reflect.Method;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.test.AndroidTestCase;

/**
 * Test the functionality of {@link TrustManagerFactory}.
 *
 * <p>
 * <strong>Important:</strong>
 * Due to {@code TrustManagerFactory} keeping its state in static fields it's difficult to test
 * that class. So the tests contained in this class should be run one at a time to avoid
 * interference caused by other tests.
 * </p>
 */
public class TrustManagerFactoryTest extends AndroidTestCase {
    public static final String MATCHING_HOST = "k9.example.com";
    public static final String NOT_MATCHING_HOST = "bla.example.com";
    public static final int PORT1 = 993;
    public static final int PORT2 = 465;

    private Context mTestContext;
    private X509Certificate[] mCertChain1;
    private X509Certificate[] mCertChain2;

    @Override
    public void setUp() throws Exception {
        // Source: https://kmansoft.wordpress.com/2011/04/18/accessing-resources-in-an-androidtestcase/
        Method m = AndroidTestCase.class.getMethod("getTestContext", new Class[] {});
        mTestContext = (Context) m.invoke(this, (Object[]) null);

        // Delete the keystore file to make sure we start without any stored certificates
        File keyStoreDir = getContext().getDir("KeyStore", Context.MODE_PRIVATE);
        new File(keyStoreDir + File.separator + "KeyStore.bks").delete();

        // Hack to make sure the static initializer of TrustManagerFactory can create the keystore file
        K9.app = new DummyApplication(getContext());

        // Load certificates
        AssetManager assets = mTestContext.getAssets();

        CertificateFactory certFactory = CertificateFactory.getInstance("X509");
        X509Certificate cert1 = (X509Certificate) certFactory.generateCertificate(assets.open("cert1.der"));
        X509Certificate cert2 = (X509Certificate) certFactory.generateCertificate(assets.open("cert2.der"));

        mCertChain1 = new X509Certificate[] { cert1 };
        mCertChain2 = new X509Certificate[] { cert2 };
    }

    /**
     * Checks if TrustManagerFactory supports a host with different certificates for different
     * services (e.g. SMTP and IMAP).
     *
     * <p>
     * This test is to make sure entries in the keystore file aren't overwritten.
     * See <a href="https://code.google.com/p/k9mail/issues/detail?id=1326">Issue 1326</a>.
     * </p>
     *
     * @throws Exception
     *         if anything goes wrong
     */
    public void testDifferentCertificatesOnSameServer() throws Exception {
        TrustManagerFactory.addCertificateChain(NOT_MATCHING_HOST, PORT1, mCertChain1);
        TrustManagerFactory.addCertificateChain(NOT_MATCHING_HOST, PORT2, mCertChain2);

        X509TrustManager trustManager1 = TrustManagerFactory.get(NOT_MATCHING_HOST, true);
        X509TrustManager trustManager2 = TrustManagerFactory.get(NOT_MATCHING_HOST, true);
        trustManager2.checkServerTrusted(mCertChain2, "authType");
        trustManager1.checkServerTrusted(mCertChain1, "authType");
    }

    public void testSelfSignedCertificateMatchingHost() throws Exception {
        TrustManagerFactory.addCertificateChain(MATCHING_HOST, PORT1, mCertChain1);
        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, true);
        trustManager.checkServerTrusted(mCertChain1, "authType");
    }

    public void testSelfSignedCertificateNotMatchingHost() throws Exception {
        TrustManagerFactory.addCertificateChain(NOT_MATCHING_HOST, PORT1, mCertChain1);
        X509TrustManager trustManager = TrustManagerFactory.get(NOT_MATCHING_HOST, true);
        trustManager.checkServerTrusted(mCertChain1, "authType");
    }

    public void testWrongCertificate() throws Exception {
        TrustManagerFactory.addCertificateChain(MATCHING_HOST, PORT1, mCertChain1);
        X509TrustManager trustManager = TrustManagerFactory.get(MATCHING_HOST, true);
        boolean certificateValid;
        try {
            trustManager.checkServerTrusted(mCertChain2, "authType");
            certificateValid = true;
        } catch (CertificateException e) {
            certificateValid = false;
        }
        assertFalse(certificateValid);
    }

    private static class DummyApplication extends Application {
        private final Context mContext;

        DummyApplication(Context context) {
            mContext = context;
        }

        public File getDir(String name, int mode) {
            return mContext.getDir(name, mode);
        }
    }
}
