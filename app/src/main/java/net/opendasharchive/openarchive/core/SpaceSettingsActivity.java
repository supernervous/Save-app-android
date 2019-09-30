package net.opendasharchive.openarchive.core;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Project;
import net.opendasharchive.openarchive.db.ProjectListAdapter;
import net.opendasharchive.openarchive.onboarding.LoginActivity;
import net.opendasharchive.openarchive.onboarding.OAAppIntro;
import net.opendasharchive.openarchive.services.WebDAVSiteController;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import io.scal.secureshareui.model.Account;

public class SpaceSettingsActivity extends AppCompatActivity {

    RecyclerView mProjectList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_space_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mProjectList = findViewById(R.id.listProjects);
        mProjectList.setLayoutManager(new LinearLayoutManager(this));
        mProjectList.setHasFixedSize(false);

        Account account = new Account(this, WebDAVSiteController.SITE_NAME);

        TextView txtSpaceName = findViewById(R.id.txtSpaceName);
        TextView txtSpaceUser = findViewById(R.id.txtSpaceUser);

        if (account != null && (!TextUtils.isEmpty(account.getSite())))
        {
            Uri uriServer = Uri.parse(account.getSite());
            txtSpaceName.setText(uriServer.getHost());
            txtSpaceUser.setText(account.getUserName());
        }


        findViewById(R.id.section_space).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SpaceSettingsActivity.this, LoginActivity.class));

            }
        });

        findViewById(R.id.btnDataUse).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SpaceSettingsActivity.this, SettingsActivity.class));

            }
        });


        updateProjects();
    }

    public void updateProjects ()
    {
        List<Project> listProjects = Project.getAllAsList();

        ProjectListAdapter adapter = new ProjectListAdapter(this,listProjects, mProjectList);

        mProjectList.setAdapter(adapter);
    }

    public void onAboutClick (View view)
    {
        startActivity(new Intent(SpaceSettingsActivity.this, OAAppIntro.class));

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
