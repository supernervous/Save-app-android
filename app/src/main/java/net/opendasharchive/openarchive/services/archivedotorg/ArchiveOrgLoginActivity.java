package net.opendasharchive.openarchive.services.archivedotorg;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.Project;
import net.opendasharchive.openarchive.db.Space;
import net.opendasharchive.openarchive.util.Prefs;

import java.util.List;

import io.scal.secureshareui.controller.ArchiveSiteController;
import io.scal.secureshareui.controller.SiteController;

import static io.scal.secureshareui.controller.ArchiveSiteController.ARCHIVE_BASE_URL;

/**
 * A login screen that offers login via email/password.
 */
public class ArchiveOrgLoginActivity extends AppCompatActivity {

    private final static String TAG = "Login";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mAccessKeyView, mSecretKeyView;

    private Space mSpace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive_key_login);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        if (getIntent().hasExtra("space")) {
            mSpace = Space.findById(Space.class, getIntent().getLongExtra("space", -1L));
            findViewById(R.id.action_remove_space).setVisibility(View.VISIBLE);

        }

        if (mSpace == null) {
            mSpace = new Space();
            mSpace.type = Space.TYPE_INTERNET_ARCHIVE;
            mSpace.host = ARCHIVE_BASE_URL;
            mSpace.name = getString(R.string.label_ia);
        }


        // Set up the login form.
        mAccessKeyView = findViewById(R.id.accesskey);
        mSecretKeyView = findViewById(R.id.secretkey);

        if (!TextUtils.isEmpty(mSpace.username))
            mAccessKeyView.setText(mSpace.username);

        mSecretKeyView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

    }

    public void onAcquireKeys (View view)
    {

        SiteController siteController = SiteController.getSiteController(ArchiveSiteController.SITE_KEY, this, null, null);
        siteController.setOnEventListener(new SiteController.OnEventListener() {
            @Override
            public void onSuccess(Space space) {

                space.save();
            }

            @Override
            public void onFailure(Space space, String failureMessage) {

            }

            @Override
            public void onRemove(Space space) {

            }
        });

        siteController.startAuthentication(mSpace);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == SiteController.CONTROLLER_REQUEST_CODE) {
            if (resultCode == android.app.Activity.RESULT_OK) {

                String credentials = intent.getStringExtra(SiteController.EXTRAS_KEY_CREDENTIALS);
                mSpace.password = (credentials != null ? credentials : "");

                String username = intent.getStringExtra(SiteController.EXTRAS_KEY_USERNAME);
                mSpace.username = (username != null ? username : "");

                mAccessKeyView.setText(username);

                mSecretKeyView.setText(credentials);
                mSpace.name = getString(R.string.label_ia);

                mSpace.type = Space.TYPE_INTERNET_ARCHIVE;

                mSpace.save();

            }
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }


        // Store values at the time of the login attempt.
        String accessKey = mAccessKeyView.getText().toString();
        String secretKey = mSecretKeyView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(secretKey) && !isPasswordValid(secretKey)) {
            mSecretKeyView.setError(getString(R.string.error_invalid_password));
            focusView = mSecretKeyView;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(accessKey) && !isPasswordValid(accessKey)) {
            mAccessKeyView.setError(getString(R.string.error_invalid_password));
            focusView = mAccessKeyView;
            cancel = true;
        }


        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mAuthTask = new UserLoginTask(accessKey, secretKey);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        return !TextUtils.isEmpty(email);
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 0;
    }


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mAccessKey;
        private final String mSecretKey;

        UserLoginTask(String accessKey, String secretKey) {
            mAccessKey = accessKey;
            mSecretKey = secretKey;
        }

        @Override
        protected Boolean doInBackground(Void... params) {



            try {




                return true;

            } catch (Exception e) {

                Log.e(TAG, "error on login", e);

                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;

            if (success) {

                if (mSpace != null)
                    Prefs.setCurrentSpaceId(mSpace.getId());

                finish();
            } else {
                mSecretKeyView.setError(getString(R.string.error_incorrect_password));
                mSecretKeyView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
          //  showProgress(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_login, menu);


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sign_in:
                attemptLogin();
                return true;

            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                //NavUtils.navigateUpFromSameTask(this);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void removeProject (View view) {

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        confirmRemoveSpace();
                        finish();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        String message = getString(R.string.confirm_remove_space);


        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setTitle(R.string.remove_from_app)
                .setMessage(message).setPositiveButton(R.string.action_remove, dialogClickListener)
                .setNegativeButton(R.string.action_cancel, dialogClickListener).show();
    }

    private void confirmRemoveSpace () {
        mSpace.delete();

        List<Project> listProjects = Project.getAllBySpace(mSpace.getId());

        for (Project project : listProjects)
        {

            List<Media> listMedia = Media.getMediaByProject(project.getId());

            for (Media media : listMedia)
            {
                media.delete();
            }

            project.delete();
        }

        finish();
    }

}

