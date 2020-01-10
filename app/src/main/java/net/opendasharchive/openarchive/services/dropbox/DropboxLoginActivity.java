package net.opendasharchive.openarchive.services.dropbox;

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
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;


import com.dropbox.core.android.Auth;

import net.opendasharchive.openarchive.BuildConfig;
import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.Project;
import net.opendasharchive.openarchive.db.Space;
import net.opendasharchive.openarchive.util.Prefs;

import java.io.IOException;
import java.util.List;

/**
 * A login screen that offers login via email/password.
 */
public class DropboxLoginActivity extends AppCompatActivity {

    private final static String TAG = "Login";


    // UI references.
    private TextView  mEmailView;
    private View mProgressView;
    private View mLoginFormView;
    private Space mSpace;

    private boolean isNewSpace = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_dropbox);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mSpace = null;


        mEmailView = findViewById(R.id.email);


        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        if (getIntent().hasExtra("space")) {
            mSpace = Space.findById(Space.class, getIntent().getLongExtra("space", -1L));
            findViewById(R.id.action_remove_space).setVisibility(View.VISIBLE);

            if (!TextUtils.isEmpty(mSpace.username))
                mEmailView.setText(mSpace.username);

        }
        else {
            isNewSpace = true;
            mSpace = new Space();
            mSpace.type = Space.TYPE_DROPBOX;
            if (TextUtils.isEmpty(mSpace.password))
                attemptLogin();

        }




    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isNewSpace) {
            String accessToken = Auth.getOAuth2Token();

            if (!TextUtils.isEmpty(accessToken) && mSpace != null) {
                mSpace.username = Auth.getUid();
                mSpace.password = accessToken;
                mSpace.save();
                finish();
            }
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {


        // Store values at the time of the login attempt.
        mSpace.username = "dropbox";
        mSpace.name = "dropbox";
        mSpace.host = "dropbox.com";

        boolean cancel = false;
        View focusView = null;

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            Auth.startOAuth2Authentication(DropboxLoginActivity.this, BuildConfig.dropbox_key);

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

