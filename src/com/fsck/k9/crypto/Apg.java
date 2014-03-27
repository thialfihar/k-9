package com.fsck.k9.crypto;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeUtility;

import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.ui.SelectSecretKeyActivity;
import org.thialfihar.android.apg.ui.SelectPublicKeyActivity;

/**
 * APG integration.
 */
public class Apg extends CryptoProvider {
    static final long serialVersionUID = 0x21071235;
    public static final String NAME = "apg";

    private static final String mApgPackageName = "org.thialfihar.android.apg";
    private static final int mMinRequiredVersion = 16;

    public static final String AUTHORITY = "org.thialfihar.android.apg.provider";
    public static final Uri CONTENT_URI_SECRET_KEY_RING_BY_KEY_ID =
        Uri.parse("content://" + AUTHORITY + "/key_rings/secret/key_id/");
    public static final Uri CONTENT_URI_SECRET_KEY_RING_BY_EMAILS =
        Uri.parse("content://" + AUTHORITY + "/key_rings/secret/emails/");

    public static final Uri CONTENT_URI_PUBLIC_KEY_RING_BY_KEY_ID =
        Uri.parse("content://" + AUTHORITY + "/key_rings/public/key_id/");
    public static final Uri CONTENT_URI_PUBLIC_KEY_RING_BY_EMAILS =
        Uri.parse("content://" + AUTHORITY + "/key_rings/public/emails/");

    public static class Intent {
        public static final String DECRYPT = "org.thialfihar.android.apg.intent.DECRYPT";
        public static final String ENCRYPT = "org.thialfihar.android.apg.intent.ENCRYPT";
        public static final String DECRYPT_FILE = "org.thialfihar.android.apg.intent.DECRYPT_FILE";
        public static final String ENCRYPT_FILE = "org.thialfihar.android.apg.intent.ENCRYPT_FILE";
        public static final String DECRYPT_AND_RETURN = "org.thialfihar.android.apg.intent.DECRYPT_AND_RETURN";
        public static final String ENCRYPT_AND_RETURN = "org.thialfihar.android.apg.intent.ENCRYPT_AND_RETURN";
        public static final String SELECT_PUBLIC_KEYS = "org.thialfihar.android.apg.intent.SELECT_PUBLIC_KEYS";
        public static final String SELECT_SECRET_KEY = "org.thialfihar.android.apg.intent.SELECT_SECRET_KEY";
    }

    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_DATA = "data";
    public static final String EXTRA_ERROR = "error";
    public static final String EXTRA_DECRYPTED_MESSAGE = "decryptedMessage";
    public static final String EXTRA_ENCRYPTED_MESSAGE = "encryptedMessage";
    public static final String EXTRA_SIGNATURE = "signature";
    public static final String EXTRA_SIGNATURE_KEY_ID = "signatureKeyId";
    public static final String EXTRA_SIGNATURE_USER_ID = "signatureUserId";
    public static final String EXTRA_SIGNATURE_SUCCESS = "signatureSuccess";
    public static final String EXTRA_SIGNATURE_UNKNOWN = "signatureUnknown";
    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_KEY_ID = "keyId";
    public static final String EXTRA_ENCRYPTION_KEY_IDS = "encryptionKeyIds";
    public static final String EXTRA_SELECTION = "selection";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_INTENT_VERSION = "intentVersion";

    public static final String INTENT_VERSION = "1";

    // Note: The support package only allows us to use the lower 16 bits of a request code.
    public static final int DECRYPT_MESSAGE = 0x0000A001;
    public static final int ENCRYPT_MESSAGE = 0x0000A002;
    public static final int SELECT_PUBLIC_KEYS = 0x0000A003;
    public static final int SELECT_SECRET_KEY = 0x0000A004;

    public static Pattern PGP_MESSAGE =
        Pattern.compile(".*?(-----BEGIN PGP MESSAGE-----.*?-----END PGP MESSAGE-----).*",
                        Pattern.DOTALL);

    public static Pattern PGP_SIGNED_MESSAGE =
        Pattern.compile(".*?(-----BEGIN PGP SIGNED MESSAGE-----.*?-----BEGIN PGP SIGNATURE-----.*?-----END PGP SIGNATURE-----).*",
                        Pattern.DOTALL);

    /**
     * Select the signature key.
     *
     * @param activity
     * @param pgpData
     * @return success or failure
     */
    @Override
    public boolean selectSecretKey(Activity activity, PgpData pgpData) {
        android.content.Intent intent = new android.content.Intent(activity, SelectSecretKeyActivity.class);
        try {
            activity.startActivityForResult(intent, Apg.SELECT_SECRET_KEY);
            return true;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity,
                           R.string.error_activity_not_found,
                           Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Select encryption keys.
     *
     * @param activity
     * @param emails The emails that should be used for preselection.
     * @param pgpData
     * @return success or failure
     */
    @Override
    public boolean selectEncryptionKeys(Activity activity, String emails, PgpData pgpData) {
        android.content.Intent intent = new android.content.Intent(activity, SelectPublicKeyActivity.class);
        long[] initialKeyIds = null;
        if (!pgpData.hasEncryptionKeys()) {
            List<Long> keyIds = new ArrayList<Long>();
            if (pgpData.hasSignatureKey()) {
                keyIds.add(pgpData.getSignatureKeyId());
            }

            try {
                Uri contentUri = Uri.withAppendedPath(
                                     Apg.CONTENT_URI_PUBLIC_KEY_RING_BY_EMAILS,
                                     emails);
                Cursor c = activity.getContentResolver().query(contentUri,
                           new String[] { "master_key_id" },
                           null, null, null);
                if (c != null) {
                    while (c.moveToNext()) {
                        keyIds.add(c.getLong(0));
                    }
                }

                if (c != null) {
                    c.close();
                }
            } catch (SecurityException e) {
                Toast.makeText(activity,
                               activity.getResources().getString(R.string.insufficient_apg_permissions),
                               Toast.LENGTH_LONG).show();
            }
            if (!keyIds.isEmpty()) {
                initialKeyIds = new long[keyIds.size()];
                for (int i = 0, size = keyIds.size(); i < size; ++i) {
                    initialKeyIds[i] = keyIds.get(i);
                }
            }
        } else {
            initialKeyIds = pgpData.getEncryptionKeys();
        }
        intent.putExtra(Apg.EXTRA_SELECTION, initialKeyIds);
        try {
            activity.startActivityForResult(intent, Apg.SELECT_PUBLIC_KEYS);
            return true;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity,
                           R.string.error_activity_not_found,
                           Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Get secret key ids based on a given email.
     *
     * @param context
     * @param email The email in question.
     * @return key ids
     */
    @Override
    public long[] getSecretKeyIdsFromEmail(Context context, String email) {
        long ids[] = null;
        try {
            Uri contentUri = Uri.withAppendedPath(Apg.CONTENT_URI_SECRET_KEY_RING_BY_EMAILS,
                                                  email);
            Cursor c = context.getContentResolver().query(contentUri,
                       new String[] { "master_key_id" },
                       null, null, null);
            if (c != null && c.getCount() > 0) {
                ids = new long[c.getCount()];
                while (c.moveToNext()) {
                    ids[c.getPosition()] = c.getLong(0);
                }
            }

            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            Toast.makeText(context,
                           context.getResources().getString(R.string.insufficient_apg_permissions),
                           Toast.LENGTH_LONG).show();
        }

        return ids;
    }

    /**
     * Get public key ids based on a given email.
     *
     * @param context
     * @param email The email in question.
     * @return key ids
     */
    @Override
    public long[] getPublicKeyIdsFromEmail(Context context, String email) {
        long ids[] = null;
        try {
            Uri contentUri = Uri.withAppendedPath(Apg.CONTENT_URI_PUBLIC_KEY_RING_BY_EMAILS, email);
            Cursor c = context.getContentResolver().query(contentUri,
                       new String[] { "master_key_id" }, null, null, null);
            if (c != null && c.getCount() > 0) {
                ids = new long[c.getCount()];
                while (c.moveToNext()) {
                    ids[c.getPosition()] = c.getLong(0);
                }
            }

            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            Toast.makeText(context,
                           context.getResources().getString(R.string.insufficient_apg_permissions),
                           Toast.LENGTH_LONG).show();
        }

        return ids;
    }

    /**
     * Find out if a given email has a secret key.
     *
     * @param context
     * @param email The email in question.
     * @return true if there is a secret key for this email.
     */
    @Override
    public boolean hasSecretKeyForEmail(Context context, String email) {
        try {
            Uri contentUri = Uri.withAppendedPath(Apg.CONTENT_URI_SECRET_KEY_RING_BY_EMAILS, email);
            Cursor c = context.getContentResolver().query(contentUri,
                    new String[] { "master_key_id" }, null, null, null);
            if (c != null && c.getCount() > 0) {
                c.close();
                return true;
            }
            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            Toast.makeText(context,
                    context.getResources().getString(R.string.insufficient_apg_permissions),
                    Toast.LENGTH_LONG).show();
        }
        return false;
    }

    /**
     * Find out if a given email has a public key.
     *
     * @param context
     * @param email The email in question.
     * @return true if there is a public key for this email.
     */
    @Override
    public boolean hasPublicKeyForEmail(Context context, String email) {
        try {
            Uri contentUri = Uri.withAppendedPath(Apg.CONTENT_URI_PUBLIC_KEY_RING_BY_EMAILS, email);
            Cursor c = context.getContentResolver().query(contentUri,
                    new String[] { "master_key_id" }, null, null, null);
            if (c != null && c.getCount() > 0) {
                c.close();
                return true;
            }
            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            Toast.makeText(context,
                    context.getResources().getString(R.string.insufficient_apg_permissions),
                    Toast.LENGTH_LONG).show();
        }
        return false;
    }

    /**
     * Get the user id based on the key id.
     *
     * @param context
     * @param keyId
     * @return user id
     */
    @Override
    public String getUserId(Context context, long keyId) {
        String userId = null;
        try {
            Uri contentUri = ContentUris.withAppendedId(
                                 Apg.CONTENT_URI_SECRET_KEY_RING_BY_KEY_ID,
                                 keyId);
            Cursor c = context.getContentResolver().query(contentUri,
                       new String[] { "user_id" },
                       null, null, null);
            if (c != null && c.moveToFirst()) {
                userId = c.getString(0);
            }

            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            Toast.makeText(context,
                           context.getResources().getString(R.string.insufficient_apg_permissions),
                           Toast.LENGTH_LONG).show();
        }

        if (userId == null) {
            userId = context.getString(R.string.unknown_crypto_signature_user_id);
        }
        return userId;
    }

    /**
     * Handle the activity results that concern us.
     *
     * @param activity
     * @param requestCode
     * @param resultCode
     * @param data
     * @return handled or not
     */
    @Override
    public boolean onActivityResult(Activity activity, int requestCode, int resultCode,
                                    android.content.Intent data, PgpData pgpData) {
        switch (requestCode) {
        case Apg.SELECT_SECRET_KEY:
            if (resultCode != Activity.RESULT_OK || data == null) {
                break;
            }
            pgpData.setSignatureKeyId(data.getLongExtra(Apg.EXTRA_KEY_ID, 0));
            pgpData.setSignatureUserId(data.getStringExtra(Apg.EXTRA_USER_ID));
            ((MessageCompose) activity).updateEncryptLayout();
            break;

        case Apg.SELECT_PUBLIC_KEYS:
            if (resultCode != Activity.RESULT_OK || data == null) {
                pgpData.setEncryptionKeys(null);
                ((MessageCompose) activity).onEncryptionKeySelectionDone();
                break;
            }
            pgpData.setEncryptionKeys(data.getLongArrayExtra(Apg.EXTRA_SELECTION));
            ((MessageCompose) activity).onEncryptionKeySelectionDone();
            break;

        case Apg.ENCRYPT_MESSAGE:
            if (resultCode != Activity.RESULT_OK || data == null) {
                pgpData.setEncryptionKeys(null);
                ((MessageCompose) activity).onEncryptDone();
                break;
            }
            pgpData.setEncryptedData(data.getStringExtra(Apg.EXTRA_ENCRYPTED_MESSAGE));
            // this was a stupid bug in an earlier version, just gonna leave this in for an APG
            // version or two
            if (pgpData.getEncryptedData() == null) {
                pgpData.setEncryptedData(data.getStringExtra(Apg.EXTRA_DECRYPTED_MESSAGE));
            }
            if (pgpData.getEncryptedData() != null) {
                ((MessageCompose) activity).onEncryptDone();
            }
            break;

        default:
            return false;
        }

        return true;
    }

    @Override
    public boolean onDecryptActivityResult(CryptoDecryptCallback callback, int requestCode,
            int resultCode, android.content.Intent data, PgpData pgpData) {

        switch (requestCode) {
            case Apg.DECRYPT_MESSAGE: {
                if (resultCode != Activity.RESULT_OK || data == null) {
                    break;
                }

                pgpData.setSignatureUserId(data.getStringExtra(Apg.EXTRA_SIGNATURE_USER_ID));
                pgpData.setSignatureKeyId(data.getLongExtra(Apg.EXTRA_SIGNATURE_KEY_ID, 0));
                pgpData.setSignatureSuccess(data.getBooleanExtra(Apg.EXTRA_SIGNATURE_SUCCESS, false));
                pgpData.setSignatureUnknown(data.getBooleanExtra(Apg.EXTRA_SIGNATURE_UNKNOWN, false));

                pgpData.setDecryptedData(data.getStringExtra(Apg.EXTRA_DECRYPTED_MESSAGE));
                callback.onDecryptDone(pgpData);

                break;
            }
            default: {
                return false;
            }
        }

        return true;
    }

    /**
     * Start the encrypt activity.
     *
     * @param activity
     * @param data
     * @param pgpData
     * @return success or failure
     */
    @Override
    public boolean encrypt(Activity activity, String data, PgpData pgpData) {
        android.content.Intent intent = new android.content.Intent(Intent.ENCRYPT_AND_RETURN);
        intent.putExtra(EXTRA_INTENT_VERSION, INTENT_VERSION);
        intent.setType("text/plain");
        intent.putExtra(Apg.EXTRA_TEXT, data);
        intent.putExtra(Apg.EXTRA_ENCRYPTION_KEY_IDS, pgpData.getEncryptionKeys());
        intent.putExtra(Apg.EXTRA_SIGNATURE_KEY_ID, pgpData.getSignatureKeyId());
        try {
            activity.startActivityForResult(intent, Apg.ENCRYPT_MESSAGE);
            return true;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity,
                           R.string.error_activity_not_found,
                           Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Start the decrypt activity.
     *
     * @param fragment
     * @param data
     * @param pgpData
     * @return success or failure
     */
    @Override
    public boolean decrypt(Fragment fragment, String data, PgpData pgpData) {
        android.content.Intent intent = new android.content.Intent(Apg.Intent.DECRYPT_AND_RETURN);
        intent.putExtra(EXTRA_INTENT_VERSION, INTENT_VERSION);
        intent.setType("text/plain");
        if (data == null) {
            return false;
        }
        try {
            intent.putExtra(EXTRA_TEXT, data);
            fragment.startActivityForResult(intent, Apg.DECRYPT_MESSAGE);
            return true;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(fragment.getActivity(), R.string.error_activity_not_found, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    @Override
    public boolean isEncrypted(Message message) {
        String data = null;
        try {
            Part part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
            if (part == null) {
                part = MimeUtility.findFirstPartByMimeType(message, "text/html");
            }
            if (part != null) {
                data = MimeUtility.getTextFromPart(part);
            }
        } catch (MessagingException e) {
            // guess not...
            // TODO: maybe log this?
        }

        if (data == null) {
            return false;
        }

        Matcher matcher = PGP_MESSAGE.matcher(data);
        return matcher.matches();
    }

    @Override
    public boolean isSigned(Message message) {
        String data = null;
        try {
            Part part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
            if (part == null) {
                part = MimeUtility.findFirstPartByMimeType(message, "text/html");
            }
            if (part != null) {
                data = MimeUtility.getTextFromPart(part);
            }
        } catch (MessagingException e) {
            // guess not...
            // TODO: maybe log this?
        }

        if (data == null) {
            return false;
        }

        Matcher matcher = PGP_SIGNED_MESSAGE.matcher(data);
        return matcher.matches();
    }
}
