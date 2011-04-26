package com.fsck.k9.activity;

import java.io.File;
import com.fsck.k9.R;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Activity that enables the user to pick a directory on the internal or external storage.
 *
 * TODO: If this is ever used for anything else than selecting the save path for attachments
 * the dialog title and description text have to be configurable. Places to change:
 * - AndroidManifest.xml / "android:label" for this Activity
 * - res/layout/pick_directory.xml / "android:text" of the TextView
 */
public class PickDirectory extends Activity {
    private static final String EXTRA_START_PATH = "startPath";

    /**
     * Start an activity that lets a user pick a directory.
     *
     * If a third-party application (usually a file manager/browser) that handles a PICK intent is
     * installed, it is used for the task. If no compatible application can be found an input field
     * is displayed to enter the path manually.
     *
     * The selected directory will be encoded as file URI in the data field of the intent handed to
     * {@link Activity#onActivityResult(int,int,Intent)}.
     *
     * @param activity The activity that will receive the result
     * @param requestCode The request code that will be returned in onActivityResult()
     * @param startPath Optional: an initial value for the path
     */
    public static void actionPick(Activity activity, int requestCode, String startPath) {
        Uri uri = (startPath != null) ? Uri.fromFile(new File(startPath)) : null;

        try {
            Intent intent = new Intent("1org.openintents.action.PICK_DIRECTORY");
            intent.setData(uri);
            activity.startActivityForResult(intent, requestCode);
            return;
        } catch (ActivityNotFoundException e) { /* Ignore */ }

        try {
            Intent intent = new Intent("com.androidworkz.action.PICK_DIRECTORY");
            intent.setData(uri);
            activity.startActivityForResult(intent, requestCode);
            return;
        } catch (ActivityNotFoundException e) { /* Ignore */ }

        Intent intent = new Intent(activity, PickDirectory.class);
        intent.putExtra(EXTRA_START_PATH, startPath);
        activity.startActivityForResult(intent, requestCode);
    }


    private EditText mDirectory;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent intent = getIntent();
        String startPath = intent.getStringExtra(EXTRA_START_PATH);

        setContentView(R.layout.pick_directory);

        mDirectory = (EditText) findViewById(R.id.directory);
        mDirectory.setText(startPath);

        Button pick = (Button) findViewById(R.id.pick_directory);
        pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = mDirectory.getText().toString();
                new ValidateDirectory(path).execute();
            }
        });

        Button cancel = (Button) findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    private class ValidateDirectory extends AsyncTask<Void, Void, Boolean> {
        private String mPath;

        ValidateDirectory(String path) {
            mPath = path;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            File f = new File(mPath);
            return (f.exists()&& f.isDirectory());
        }

        @Override
        protected void onPostExecute(Boolean valid) {
            if (valid) {
                Intent result = new Intent();
                result.setData(Uri.fromFile(new File(mPath)));
                setResult(RESULT_OK, result);
                finish();
            } else {
                Toast.makeText(PickDirectory.this, R.string.pick_directory_invalid_directoy,
                        Toast.LENGTH_LONG).show();
                mDirectory.requestFocus();
            }
        }
    }
}
