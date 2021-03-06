package com.fsck.k9.crypto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.fsck.k9.mail.Message;

/**
 * A CryptoProvider provides functionalities such as encryption, decryption, digital signatures.
 * It currently also stores the results of such encryption or decryption.
 * TODO: separate the storage from the provider
 *
 * Modified by Adam Wasserman to include PGPKeyRing provider. (9 May 2013)
 */
abstract public class CryptoProvider {
    static final long serialVersionUID = 0x21071234;

    abstract public boolean isEncrypted(Message message);
    abstract public boolean isSigned(Message message);
    abstract public boolean onActivityResult(Activity activity, int requestCode, int resultCode,
            Intent data, PgpData pgpData);
    abstract public boolean onDecryptActivityResult(CryptoDecryptCallback callback,
            int requestCode, int resultCode, Intent data, PgpData pgpData);
    abstract public boolean selectSecretKey(Activity activity, PgpData pgpData);
    abstract public boolean selectEncryptionKeys(Activity activity, String emails, PgpData pgpData);
    abstract public boolean encrypt(Activity activity, String textData, byte[] binaryData, PgpData pgpData);
    abstract public boolean encryptFile( Activity activity, String filename, PgpData pgpData );
    abstract public boolean sign( Activity activity, String filename, PgpData pgpData );
    abstract public boolean decrypt(Fragment fragment, String filename, PgpData pgpData);
    abstract public boolean decryptFile( Fragment fragment, String filename, boolean showFile, PgpData pgpData );
    abstract public boolean verify( Fragment fragment, String filename, String sig, PgpData pgpData );
    abstract public long[] getSecretKeyIdsFromEmail(Context context, String email);
    abstract public long[] getPublicKeyIdsFromEmail(Context context, String email);
    abstract public boolean hasSecretKeyForEmail(Context context, String email);
    abstract public boolean hasPublicKeyForEmail(Context context, String email);
    abstract public String getUserId(Context context, long keyId);

    public boolean supportsAttachments( Context context ) {
    	return false;
    }

    public boolean supportsPgpMimeReceive( Context context ) {
    	return false;
    }

    public boolean supportsPgpMimeSend( Context context ) {
    	return false;
    }

    public static CryptoProvider createInstance() {
        return new Apg();
    }

    public interface CryptoDecryptCallback {
        void onDecryptDone(PgpData pgpData);
        void onDecryptFileDone(PgpData pgpData);
    }

}
