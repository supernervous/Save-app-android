package net.opendasharchive.openarchive.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.util.Globals;

import java.util.HashMap;

import androidx.appcompat.app.AppCompatActivity;
import io.scal.secureshareui.controller.SiteController;


public class ArchiveSettingsActivity extends AppCompatActivity {
    public static final String TAG = "ArchiveMetadataActivity";

    private Context mContext = this;
    private Media mMedia;

    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvAuthor;
    private TextView tvTags;
    private TextView tvLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive_metadata);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // set defaults based on previous selections
        SharedPreferences sharedPref = this.getSharedPreferences(Globals.PREF_FILE_KEY, Context.MODE_PRIVATE);

        // get current media
        final long mediaId = getIntent().getLongExtra(Globals.EXTRA_CURRENT_MEDIA_ID, -1);
        // init listeners for textviews
        initViews(mediaId);

        // set up ccLicense link
        final TextView tvCCLicenseLink = (TextView) findViewById(R.id.tv_cc_license);
        tvCCLicenseLink.setMovementMethod(LinkMovementMethod.getInstance());
//        setCCLicenseText(rgLicense.getCheckedRadioButtonId(), tvCCLicenseLink);


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                saveMediaMetadata();
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        saveMediaMetadata();
        super.onBackPressed();
    }

    private void setCCLicenseText(int licenseId, TextView tvCCLicenseLink) {
        /**
        if (licenseId == R.id.radioBy) {
            tvCCLicenseLink.setText(R.string.archive_license_by);
        } else if (licenseId == R.id.radioBySa) {
            tvCCLicenseLink.setText(R.string.archive_license_bysa);
        } else { // ByNcNd is default
            tvCCLicenseLink.setText(R.string.archive_license_byncnd);
        }*/
    }

    private void initViews(long mediaId) {
        // instantiate values
        tvTitle = (TextView) findViewById(R.id.tv_title_lbl);

        tvDescription = (TextView) findViewById(R.id.tv_description_lbl);
        tvAuthor = (TextView) findViewById(R.id.tv_author_lbl);
        tvTags = (TextView) findViewById(R.id.tv_tags_lbl);
        tvLocation = (TextView) findViewById(R.id.tv_location_lbl);

        // if valid media id
        if(mediaId >= 0) {
            mMedia = Media.getMediaById(mediaId);
        } else {
            tvTitle.setVisibility(View.GONE);
            tvDescription.setVisibility(View.GONE);
            tvAuthor.setVisibility(View.GONE);
            tvTags.setVisibility(View.GONE);
            tvLocation.setVisibility(View.GONE);

            return;
        }

        // set to visible
        tvTitle.setVisibility(View.VISIBLE);
        tvDescription.setVisibility(View.VISIBLE);
        tvAuthor.setVisibility(View.VISIBLE);
        tvTags.setVisibility(View.VISIBLE);
        tvLocation.setVisibility(View.VISIBLE);

        // set values
        tvTitle.setText(mMedia.getTitle());
        tvDescription.setText(mMedia.getDescription());
        tvAuthor.setText(mMedia.getAuthor());
        tvLocation.setText(mMedia.getLocation());
        tvTags.setText(mMedia.getTags());

    }


    // TODO helper method to grab the shared prefs and return a values hashmap

    // TODO this also needs to store what the sharing prefs were when this was submitted I believe
    private void saveMediaMetadata() {
        String licenseUrl = "https://creativecommons.org/licenses/by/4.0/";
        /**
        int licenseId = rgLicense.getCheckedRadioButtonId();
        if (licenseId == R.id.radioBy) {

        } else if (licenseId == R.id.radioBySa) {
            licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0/";
        } else { // ByNcNd is default
            licenseUrl = "http://creativecommons.org/licenses/by-nc-nd/4.0/";
        }*/

        // save defaults for future selections
        SharedPreferences sharedPref = this.getSharedPreferences(Globals.PREF_FILE_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Globals.PREF_LICENSE_URL, licenseUrl); // FIXME this should store the license url not the idx
        editor.apply();

        // save value changes in db
        if(null != mMedia) {
            // set values
            mMedia.setTitle(tvTitle.getText().toString().trim());
            mMedia.setDescription(tvDescription.getText().toString().trim());
            mMedia.setAuthor(tvAuthor.getText().toString().trim());
            mMedia.setLocation(tvLocation.getText().toString().trim());
            mMedia.setTags(tvTags.getText().toString().trim());

            mMedia.save();
        }
    }

    public static String getSlug(String title) {
        return title.replaceAll("[^A-Za-z0-9]", "-");
    }

    public static HashMap<String, String> getMediaMetadata(Context context, Media mMedia) {

        HashMap<String, String> valueMap = new HashMap<String, String>();
        SharedPreferences sharedPref = context.getSharedPreferences(Globals.PREF_FILE_KEY, Context.MODE_PRIVATE);

        valueMap.put(SiteController.VALUE_KEY_MEDIA_PATH, mMedia.getOriginalFilePath());
        valueMap.put(SiteController.VALUE_KEY_MIME_TYPE, mMedia.getMimeType());
        valueMap.put(SiteController.VALUE_KEY_SLUG, getSlug(mMedia.getTitle()));
        valueMap.put(SiteController.VALUE_KEY_TITLE, mMedia.getTitle());

        if (!TextUtils.isEmpty(mMedia.getLicenseUrl()))
            valueMap.put(SiteController.VALUE_KEY_LICENSE_URL, mMedia.getLicenseUrl());
        else
            valueMap.put(SiteController.VALUE_KEY_LICENSE_URL, "https://creativecommons.org/licenses/by/4.0/");

        if (!TextUtils.isEmpty(mMedia.getTags())) {
            String tags = context.getString(R.string.default_tags) + ";" + mMedia.getTags(); // FIXME are keywords/tags separated by spaces or commas?
            valueMap.put(SiteController.VALUE_KEY_TAGS, tags);
        }

        if (!TextUtils.isEmpty(mMedia.getAuthor()))
            valueMap.put(SiteController.VALUE_KEY_AUTHOR, mMedia.getAuthor());

        //valueMap.put(SiteController.VALUE_KEY_PROFILE_URL, "TESTING"); // TODO

        if (!TextUtils.isEmpty(mMedia.getLocation()))
            valueMap.put(SiteController.VALUE_KEY_LOCATION_NAME, mMedia.getLocation()); // TODO

        if (!TextUtils.isEmpty(mMedia.getDescription()))
            valueMap.put(SiteController.VALUE_KEY_BODY, mMedia.getDescription());

        return valueMap;
    }
}