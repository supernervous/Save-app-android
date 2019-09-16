package net.opendasharchive.openarchive;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import net.opendasharchive.openarchive.db.Project;

import java.util.Date;

public class CreateNewProjectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_project);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("New Project");


         final EditText etNewProjectName = findViewById(R.id.edtNewProject);
         etNewProjectName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId== EditorInfo.IME_ACTION_DONE){

                    String newProjectName = etNewProjectName.getText().toString();
                    if (!TextUtils.isEmpty(newProjectName))
                    {
                        createProject(newProjectName);
                        setResult(RESULT_OK);
                        finish();
                    }

                }
            return false;
        }
        });

    }

    private void createProject (String description)
    {
        Project project = new Project ();
        project.created = new Date();
        project.description = description;
        project.save();
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


}
