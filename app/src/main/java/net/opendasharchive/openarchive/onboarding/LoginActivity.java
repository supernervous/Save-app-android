package net.opendasharchive.openarchive.onboarding;

import android.content.DialogInterface;
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

import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.Project;
import net.opendasharchive.openarchive.db.Space;
import net.opendasharchive.openarchive.services.WebDAVSiteController;
import net.opendasharchive.openarchive.util.Prefs;

import java.io.IOException;
import java.util.List;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    private final static String TAG = "Login";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mNameView, mEmailView, mServerView, mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private Space mSpace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mSpace = null;

        if (getIntent().hasExtra("space")) {
            mSpace = Space.findById(Space.class, getIntent().getLongExtra("space", -1L));
            findViewById(R.id.action_remove_space).setVisibility(View.VISIBLE);
        }
        else {
            mSpace = new Space();
            mSpace.type = Space.TYPE_WEBDAV;
        }

        // Set up the login form.
        mNameView = findViewById(R.id.servername);
        mEmailView = findViewById(R.id.email);
        mServerView = findViewById(R.id.server);


        if (!TextUtils.isEmpty(mSpace.name))
            mNameView.setText(mSpace.name);

        if (!TextUtils.isEmpty(mSpace.host))
            mServerView.setText(mSpace.host);


        if (!TextUtils.isEmpty(mSpace.username))
            mEmailView.setText(mSpace.username);

        mPasswordView = findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
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

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        mSpace.name = mNameView.getText().toString();
        mSpace.username = mEmailView.getText().toString();
        mSpace.password = mPasswordView.getText().toString();
        mSpace.host = mServerView.getText().toString();

        if (TextUtils.isEmpty(mSpace.name))
            mSpace.name = mSpace.host;

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(mSpace.password) && !isPasswordValid(mSpace.password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(mSpace.username)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(mSpace.username)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (!mSpace.host.toLowerCase().startsWith("http"))
        {
            //auto add nextcloud defaults
            mSpace.host = "https://" + mSpace.host + "/remote.php/dav/";
        }
        else if (!mSpace.host.contains("/dav"))
        {
            mSpace.host += "/remote.php/dav/";
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mAuthTask = new UserLoginTask();
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

        UserLoginTask() {

        }

        @Override
        protected Boolean doInBackground(Void... params) {


            StringBuffer siteUrl = new StringBuffer();
            siteUrl.append(mSpace.host);
            if (!mSpace.host.endsWith("/"))
                siteUrl.append("/");

            Sardine sardine = new OkHttpSardine();
            sardine.setCredentials(mSpace.username,mSpace.password);

            try {
                try
                {
                    sardine.getQuota(siteUrl.toString());

                    mSpace.save();
                    Prefs.setCurrentSpaceId(mSpace.getId());

                    return true;
                }
                catch (Exception e) {

                    siteUrl.append("remote.php/dav/");
                    sardine.getQuota(siteUrl.toString());
                    Prefs.setCurrentSpaceId(mSpace.getId());
                    mSpace.save();

                    return true;
                }


            } catch (IOException e) {

                Log.e(TAG,"error on login: " + siteUrl.toString(),e);

                return false;
            }

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
          //  showProgress(false);

            if (success) {
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
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

