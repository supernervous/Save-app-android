package net.opendasharchive.openarchive.projects;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Project;
import net.opendasharchive.openarchive.util.Globals;

public class EditProjectActivity extends AppCompatActivity {

    Project mProject = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_project);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        long projectId = getIntent().getLongExtra(Globals.EXTRA_CURRENT_PROJECT_ID,-1L);

        if (projectId != -1L)
            mProject = Project.getById(projectId);
        else
            finish();

        updateProject();
    }

    private void updateProject ()
    {
        EditText et = findViewById(R.id.edtProjectName);
        et.setText(mProject.description);
        et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId== EditorInfo.IME_ACTION_DONE){

                    String newProjectName = et.getText().toString();
                    if (!TextUtils.isEmpty(newProjectName))
                    {
                        mProject.description = newProjectName;
                        mProject.save();
                    }

                }
                return false;
            }
        });
        TextView tv = findViewById(R.id.action_archive_project);

        if (mProject.isArchived())
            tv.setText(R.string.action_unarchive_project);
        else
            tv.setText(R.string.action_archive_project);
    }

    public void removeProject (View view) {


        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        mProject.delete();
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
