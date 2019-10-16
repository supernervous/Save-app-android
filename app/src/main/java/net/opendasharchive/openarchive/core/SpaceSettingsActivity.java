package net.opendasharchive.openarchive.core;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Project;
import net.opendasharchive.openarchive.db.ProjectListAdapter;
import net.opendasharchive.openarchive.db.Space;
import net.opendasharchive.openarchive.onboarding.ArchiveOrgLoginActivity;
import net.opendasharchive.openarchive.onboarding.FirstStartActivity;
import net.opendasharchive.openarchive.onboarding.LoginActivity;
import net.opendasharchive.openarchive.services.WebDAVSiteController;
import net.opendasharchive.openarchive.util.Prefs;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.abdularis.civ.AvatarImageView;

import java.util.Iterator;
import java.util.List;


public class SpaceSettingsActivity extends AppCompatActivity {

    RecyclerView mProjectList;
    Space mSpace;

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


        findViewById(R.id.section_space).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSpaceAuthActivity();
            }
        });

        findViewById(R.id.btnDataUse).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SpaceSettingsActivity.this, SettingsActivity.class));

            }
        });

    }


    private void loadSpaces ()
    {
        Iterator<Space> listSpaces = Space.getAllAsList();

        LinearLayout viewSpace = findViewById(R.id.spaceview);
        viewSpace.removeAllViews();

        while (listSpaces.hasNext()) {
            Space space = listSpaces.next();

            ImageView image = getSpaceIcon(space);

            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Prefs.setCurrentSpaceId(space.getId());
                    showCurrentSpace();
                }
            });

            viewSpace.addView(image);
        }
    }

    private ImageView getSpaceIcon (Space space)
    {
        AvatarImageView image = new AvatarImageView(this);

        image.setAvatarBackgroundColor(getResources().getColor(R.color.oablue));

        if (space.type == Space.TYPE_INTERNET_ARCHIVE) {
            image.setImageResource(R.drawable.ialogo128);
            image.setState(AvatarImageView.SHOW_IMAGE);
        }
        else {
            image.setText(space.name.substring(0, 1).toUpperCase());
            image.setState(AvatarImageView.SHOW_INITIAL);
        }

        int iconSize = 128;

        int margin = 3;

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(margin, margin, margin, margin);
        lp.height =iconSize;
        lp.width = iconSize;
        image.setLayoutParams(lp);

        return image;
    }

    @Override
    protected void onResume() {
        super.onResume();

        showCurrentSpace();
        loadSpaces();
    }

    private void showCurrentSpace () {

        mSpace = Space.getCurrentSpace();
        if (mSpace == null)
        {
            Iterator<Space> listSpaces = Space.getAllAsList();
            if (listSpaces.hasNext())
            {
                mSpace = listSpaces.next();
                Prefs.setCurrentSpaceId(mSpace.getId());
            }
        }

        TextView txtSpaceName = findViewById(R.id.txtSpaceName);
        TextView txtSpaceUser = findViewById(R.id.txtSpaceUser);

        if (mSpace != null) {
            Uri uriServer = Uri.parse(mSpace.host);

            if (!TextUtils.isEmpty(mSpace.name))
                txtSpaceName.setText(mSpace.name);
            else
                txtSpaceName.setText(uriServer.getHost());

            txtSpaceUser.setText(mSpace.username);

            updateProjects();

            AvatarImageView image = findViewById(R.id.space_avatar);
            image.setAvatarBackgroundColor(getResources().getColor(R.color.oablue));
            if (mSpace.type == Space.TYPE_INTERNET_ARCHIVE) {
                image.setImageResource(R.drawable.ialogo128);
                image.setState(AvatarImageView.SHOW_IMAGE);
            }
            else {
                image.setText(mSpace.name.substring(0, 1).toUpperCase());
                image.setState(AvatarImageView.SHOW_INITIAL);
            }

            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   startSpaceAuthActivity ();

                }
            });


        }
    }

    private void startSpaceAuthActivity ()
    {
        Intent intent = null;

        if (mSpace.type == Space.TYPE_WEBDAV)
            intent = new Intent(SpaceSettingsActivity.this, LoginActivity.class);
        else
            intent = new Intent(SpaceSettingsActivity.this, ArchiveOrgLoginActivity.class);

        intent.putExtra("space",mSpace.getId());

        startActivity(intent);
    }

    public void updateProjects ()
    {
        List<Project> listProjects = Project.getAllBySpace(Space.getCurrentSpace().getId());

        ProjectListAdapter adapter = new ProjectListAdapter(this,listProjects, mProjectList);

        mProjectList.setAdapter(adapter);
    }

    public void onAboutClick (View view)
    {
        //startActivity(new Intent(SpaceSettingsActivity.this, OAAppIntro.class));
        openBrowser("https://open-archive.org/");
    }

    public void onPrivacyClick (View view)
    {
        openBrowser("https://open-archive.org/privacy/");

    }

    private void openBrowser (String link)
    {
        try {
            Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            startActivity(myIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No application can handle this request."
                    + " Please install a webbrowser",  Toast.LENGTH_LONG).show();
            e.printStackTrace();
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

    public void onNewSpaceClicked (View view)
    {
        Intent myIntent = new Intent(this, FirstStartActivity.class);
        startActivity(myIntent);
    }
}
