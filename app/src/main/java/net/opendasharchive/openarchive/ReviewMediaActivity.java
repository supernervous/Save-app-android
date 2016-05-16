package net.opendasharchive.openarchive;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.ShareActionProvider;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import net.opendasharchive.openarchive.db.Media;

import java.util.HashMap;

import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.model.Account;


public class ReviewMediaActivity extends ActionBarActivity {
    private static String TAG = "ReviewMediaActivity";

    private Context mContext = this;
    private Media mMedia;
    private ProgressDialog progressDialog = null;

    private RadioGroup rgLicense;

    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvAuthor;
    private TextView tvTags;
    private TextView tvLocation;
    private TextView tvUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_media);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // instantiate values
        tvTitle = (TextView) findViewById(R.id.tv_title_lbl);
        tvDescription = (TextView) findViewById(R.id.tv_description_lbl);
        tvAuthor = (TextView) findViewById(R.id.tv_author_lbl);
        tvTags = (TextView) findViewById(R.id.tv_tags_lbl);
        tvLocation = (TextView) findViewById(R.id.tv_location_lbl);
        rgLicense = (RadioGroup) findViewById(R.id.radioGroupCC);
        tvUrl = (TextView) findViewById(R.id.tv_url);

    }

    private void bindMedia ()
    {
        setTitle(mMedia.getTitle());

        // set up ccLicense link
        final TextView tvCCLicenseLink = (TextView) findViewById(R.id.tv_cc_license);
        tvCCLicenseLink.setMovementMethod(LinkMovementMethod.getInstance());
        setCCLicenseText(rgLicense.getCheckedRadioButtonId(), tvCCLicenseLink);

        rgLicense.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                setCCLicenseText(rgLicense.getCheckedRadioButtonId(), tvCCLicenseLink);
            }
        });

        // set values
        tvTitle.setText(mMedia.getTitle());
        tvDescription.setText(mMedia.getDescription());
        tvAuthor.setText(mMedia.getAuthor());
        tvLocation.setText(mMedia.getLocation());
        tvTags.setText(mMedia.getTags());

        if (mMedia.getServerUrl() != null)
        {
            tvUrl.setText( Html.fromHtml("Your media is available on the Internet Archive at <a href=\"" + mMedia.getServerUrl() + "\">" + mMedia.getServerUrl() + "</a>"));
            tvUrl.setMovementMethod(LinkMovementMethod.getInstance());

            tvUrl.setVisibility(View.VISIBLE);


            tvTitle.setEnabled(false);

            tvDescription.setEnabled(false);
            if (TextUtils.isEmpty(mMedia.getDescription()))
                tvDescription.setVisibility(View.GONE);

            tvAuthor.setEnabled(false);
            if (TextUtils.isEmpty(mMedia.getAuthor()))
                tvAuthor.setVisibility(View.GONE);

            tvLocation.setEnabled(false);
            if (TextUtils.isEmpty(mMedia.getLocation()))
                tvLocation.setVisibility(View.GONE);

            tvTags.setEnabled(false);
            if (TextUtils.isEmpty(mMedia.getTags()))
                tvTags.setVisibility(View.GONE);

            findViewById(R.id.groupLicenseChooser).setVisibility(View.GONE);
        }


    }

    private void saveMedia ()
    {
        mMedia.setTitle(tvTitle.getText().toString());
        mMedia.setDescription(tvDescription.getText().toString());
        mMedia.setAuthor(tvAuthor.getText().toString());
        mMedia.setLocation(tvLocation.getText().toString());
        mMedia.setTags(tvTags.getText().toString());

        mMedia.save();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveMedia();
    }

    private void setCCLicenseText(int licenseId, TextView tvCCLicenseLink) {
        if (licenseId == R.id.radioBy) {
            tvCCLicenseLink.setText(R.string.archive_license_by);
        } else if (licenseId == R.id.radioBySa) {
            tvCCLicenseLink.setText(R.string.archive_license_bysa);
        } else { // ByNcNd is default
            tvCCLicenseLink.setText(R.string.archive_license_byncnd);
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (mMedia.getServerUrl() == null)
            getMenuInflater().inflate(R.menu.menu_review_media, menu);
        else {
            //show a different menu!
            getMenuInflater().inflate(R.menu.menu_share_media, menu);

            // Locate MenuItem with ShareActionProvider
            MenuItem item = menu.findItem(R.id.menu_item_share);





        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub

//        if (drawerListener.onOptionsItemSelected(item)) {
//            return true;
//        }

        Intent intent = null;
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_upload:
                uploadMedia();
                break;

            case R.id.menu_item_share:
                shareMedia();
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void init() {
        Intent intent = getIntent();

        // get intent extras
        long currentMediaId = intent.getLongExtra(Globals.EXTRA_CURRENT_MEDIA_ID, -1);

        // get default metadata sharing values
        SharedPreferences sharedPref = this.getSharedPreferences(Globals.PREF_FILE_KEY, Context.MODE_PRIVATE);

        // check for new file or existing media
        if (currentMediaId >= 0) {
            mMedia = Media.findById(Media.class, currentMediaId);
        } else {
            Utility.toastOnUiThread(this, getString(R.string.error_no_media));
            finish();
            return;
        }

        // display media preview if available
        ImageView ivMedia = (ImageView) findViewById(R.id.ivMedia);
        ivMedia.setImageBitmap(mMedia.getThumbnail(mContext));


    }

    //share the link to the file on the IA
    private void shareMedia ()
    {

        StringBuffer sb = new StringBuffer();
        sb.append("\"").append(mMedia.getTitle()).append("\"").append(' ');
        sb.append(getString(R.string.share_text)).append(' ');
        sb.append(mMedia.getServerUrl());

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mMedia.getTitle());
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_using)));
    }

    private void uploadMedia ()
    {
        saveMedia();

        Context context = ReviewMediaActivity.this;
        SiteController siteController = SiteController.getSiteController("archive", context, mHandler, null);

        Account account = new Account(context, null);

        HashMap<String, String> valueMap = ArchiveSettingsActivity.getMediaMetadata(ReviewMediaActivity.this, mMedia);
        boolean useTor = false;
        siteController.upload(account, valueMap, useTor);
        showProgressSpinner();
    }

    private void closeProgressSpinner() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void showProgressSpinner() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.loading_title));
        progressDialog.setMessage(getString(R.string.loading_message));
        progressDialog.show();

    }

    @Override
    protected void onResume() {
        super.onResume();

        init();
        bindMedia();
    }

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();

            String jobIdString = data.getString(SiteController.MESSAGE_KEY_JOB_ID);
            int jobId = (jobIdString != null) ? Integer.parseInt(jobIdString) : -1;

            int messageType = data.getInt(SiteController.MESSAGE_KEY_TYPE);
            switch (messageType) {
                case SiteController.MESSAGE_TYPE_SUCCESS:
                    String result = data.getString(SiteController.MESSAGE_KEY_RESULT);
                    String resultUrl = getDetailsUrlFromResult(result);
                    mMedia.setServerUrl(resultUrl);
                    mMedia.save();
                    showSuccess();
                    bindMedia();
                    closeProgressSpinner();
                    break;
                case SiteController.MESSAGE_TYPE_FAILURE:
                    int errorCode = data.getInt(SiteController.MESSAGE_KEY_CODE);
                    String errorMessage = data.getString(SiteController.MESSAGE_KEY_MESSAGE);
                    String error = "Error " + errorCode + ": " + errorMessage;
                    closeProgressSpinner();
                    showError(error);
                    Log.d(TAG, "upload error: " + error);
                    break;
                case SiteController.MESSAGE_TYPE_PROGRESS:
                    String message = data.getString(SiteController.MESSAGE_KEY_MESSAGE);
                    float progress = data.getFloat(SiteController.MESSAGE_KEY_PROGRESS);
                    Log.d(TAG, "upload progress: " + progress);
                    // TODO implement a progress dialog to show this
                    break;
            }
        }
    };

    private void showSuccess ()
    {
        Toast.makeText(this,getString(R.string.upload_success),Toast.LENGTH_SHORT).show();
    }

    // result is formatted like http://s3.us.archive.org/Default-Title-19db/JPEG_20150123_160341_-1724212344_thumbnail.png
    public String getDetailsUrlFromResult(String result) {
//        String slug = ArchiveSettingsActivity.getSlug(mMedia.getTitle());
        String[] splits = result.split("/");
        String slug = splits[3];

        return "http://archive.org/details/" + slug;
    }

    public void showError(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if(!isFinishing()){
                    new AlertDialog.Builder(ReviewMediaActivity.this)
                            .setTitle("Upload Error")
                            .setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ReviewMediaActivity.this.finish();
                                }
                            }).create().show();
                }
            }
        });
    }


}