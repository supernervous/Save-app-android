package net.opendasharchive.openarchive.projects;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Project;
import net.opendasharchive.openarchive.util.Globals;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class EditProjectActivity extends AppCompatActivity {

    Project mProject = null;


    SwitchCompat switchCC;
    SwitchCompat tbDeriv;
    SwitchCompat tbComm;
    SwitchCompat tbShare;

    TextView tvCCLicense;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_project);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        long projectId = getIntent().getLongExtra(Globals.EXTRA_CURRENT_PROJECT_ID,-1L);

        if (projectId != -1L) {
            mProject = Project.getById(projectId);
            if (mProject == null)
                finish();

        }
        else
            finish();


        updateProject();
    }

    @Override
    protected void onPause() {
        super.onPause();

        updateLicense();
    }

    private void updateProject ()
    {
        EditText et = findViewById(R.id.edtProjectName);

        if (mProject == null)
            return;

        if (!TextUtils.isEmpty(mProject.description)) {
            et.setText(mProject.description);
            et.setEnabled(false);
        }
        else {
            et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {

                        String newProjectName = et.getText().toString();
                        if (!TextUtils.isEmpty(newProjectName)) {
                            mProject.description = newProjectName;
                            mProject.save();
                        }

                    }
                    return false;
                }
            });
        }

        TextView tv = findViewById(R.id.action_archive_project);

        if (mProject.isArchived())
            tv.setText(R.string.action_unarchive_project);
        else
            tv.setText(R.string.action_archive_project);


        switchCC = findViewById(R.id.tb_cc_deriv_enable);
        tbDeriv = findViewById(R.id.tb_cc_deriv);
        tbComm = findViewById(R.id.tb_cc_comm);
        tbShare = findViewById(R.id.tb_cc_sharealike);
        tvCCLicense = findViewById(R.id.cc_license_display);

        switchCC.setChecked(mProject.getLicenseUrl() != null);

        findViewById(R.id.cc_row_1).setVisibility(switchCC.isChecked() ? VISIBLE : GONE);
        findViewById(R.id.cc_row_2).setVisibility(switchCC.isChecked() ? VISIBLE : GONE);
        findViewById(R.id.cc_row_3).setVisibility(switchCC.isChecked() ? VISIBLE : GONE);

        switchCC.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                findViewById(R.id.cc_row_1).setVisibility(isChecked ? VISIBLE : GONE);
                findViewById(R.id.cc_row_2).setVisibility(isChecked ? VISIBLE : GONE);
                findViewById(R.id.cc_row_3).setVisibility(isChecked ? VISIBLE : GONE);

                updateLicense();
            }
        });



        if (!TextUtils.isEmpty(mProject.getLicenseUrl()))
        {
            if (mProject.getLicenseUrl().equals("http://creativecommons.org/licenses/by-sa/4.0/"))
            {
                tbDeriv.setChecked(true);
                tbComm.setChecked(true);
                tbShare.setChecked(true);
            }
            else if (mProject.getLicenseUrl().equals("http://creativecommons.org/licenses/by-nc-sa/4.0/"))
            {
                tbDeriv.setChecked(true);
                tbShare.setChecked(true);
            }
            else if (mProject.getLicenseUrl().equals("http://creativecommons.org/licenses/by/4.0/"))
            {
                tbDeriv.setChecked(true);
                tbComm.setChecked(true);
            }
            else if (mProject.getLicenseUrl().equals("http://creativecommons.org/licenses/by-nc/4.0/"))
            {
                tbDeriv.setChecked(true);
            }
            else if (mProject.getLicenseUrl().equals("http://creativecommons.org/licenses/by-nd/4.0/"))
            {
                tbComm.setChecked(true);
            }
        }

        tbDeriv.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateLicense();

                tbShare.setEnabled(isChecked);
            }
        });

        tbComm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateLicense();
            }
        });

        tbShare.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateLicense();
            }
        });

        tbShare.setEnabled(tbDeriv.isChecked());
        tvCCLicense.setText(mProject.getLicenseUrl());




    }



    public void updateLicense ()
    {
        if (mProject == null)
            finish();

        //the default
        String licenseUrl = "https://creativecommons.org/licenses/by/4.0/";

        if (!switchCC.isChecked()){
            tvCCLicense.setText("");
            mProject.setLicenseUrl(null);
            mProject.save();
            return;
        }

        if (tbDeriv.isChecked() && tbComm.isChecked() && tbShare.isChecked())
        {
            licenseUrl = "http://creativecommons.org/licenses/by-sa/4.0/";
        }
        else if (tbDeriv.isChecked() && tbShare.isChecked())
        {
            licenseUrl = "http://creativecommons.org/licenses/by-nc-sa/4.0/";
        }
        else if (tbDeriv.isChecked() && tbComm.isChecked())
        {
            licenseUrl = "http://creativecommons.org/licenses/by/4.0/";
        }
        else if (tbDeriv.isChecked())
        {
            licenseUrl = "http://creativecommons.org/licenses/by-nc/4.0/";
        }
        else if (tbComm.isChecked())
        {
            licenseUrl = "http://creativecommons.org/licenses/by-nd/4.0/";
        }

        tvCCLicense.setText(licenseUrl);
        mProject.setLicenseUrl(licenseUrl);
        mProject.save();
    }

    public void removeProject (View view) {


        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        mProject.delete();
                        mProject = null;
                        finish();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        String message = getString(R.string.action_remove_project);


        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setTitle(R.string.remove_from_app)
                .setMessage(message).setPositiveButton(R.string.action_remove, dialogClickListener)
                .setNegativeButton(R.string.action_cancel, dialogClickListener).show();

    }

    public void archiveProject (View view) {

        mProject.setArchived(!mProject.isArchived());
        mProject.save();

        TextView tv = findViewById(R.id.action_archive_project);

        if (mProject.isArchived())
            tv.setText(R.string.action_unarchive_project);
        else
            tv.setText(R.string.action_archive_project);

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                //NavUtils.navigateUpFromSameTask(this);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            finish();
        }
    }
}
