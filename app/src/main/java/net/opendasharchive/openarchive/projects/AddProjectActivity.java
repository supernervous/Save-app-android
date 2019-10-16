package net.opendasharchive.openarchive.projects;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Space;
import net.opendasharchive.openarchive.onboarding.FirstStartActivity;

public class AddProjectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_project);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


    }

    public void onNewProjectClicked (View view) {

        Space space = Space.getCurrentSpace();
        if (space != null) {
            Intent intent = new Intent(this, CreateNewProjectActivity.class);
            startActivityForResult(intent, 1000);
        }
        else
        {
            finish();
            startActivity(new Intent(this, FirstStartActivity.class));
        }
    }

    public void onBrowseProjects (View view) {

        Space space = Space.getCurrentSpace();
        if (space != null) {
            Intent intent = new Intent(this, BrowseProjectsActivity.class);
            startActivityForResult(intent, 1001);
        }
        else
        {
            finish();
            startActivity(new Intent(this, FirstStartActivity.class));
        }

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
