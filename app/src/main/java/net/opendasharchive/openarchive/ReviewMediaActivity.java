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
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TableRow;
import android.widget.TextView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_media);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        init();

        setTitle(mMedia.getTitle());

        // instantiate values
        tvTitle = (TextView) findViewById(R.id.tv_title_lbl);
        tvDescription = (TextView) findViewById(R.id.tv_description_lbl);
        tvAuthor = (TextView) findViewById(R.id.tv_author_lbl);
        tvTags = (TextView) findViewById(R.id.tv_tags_lbl);
        tvLocation = (TextView) findViewById(R.id.tv_location_lbl);
        rgLicense = (RadioGroup) findViewById(R.id.radioGroupCC);

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
        getMenuInflater().inflate(R.menu.menu_review_media, menu);
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

    private void getMetadataValues() {
/**
        EditText edTitle = findViewById(R.id.tv_)
        if (mMedia != null) {
            tvTitle.setText(mMedia.getTitle());
            tvDescription.setText(mMedia.getDescription());
            tvAuthor.setText(mMedia.getAuthor());
            tvLocation.setText(mMedia.getLocation());
            tvTags.setText(mMedia.getTags());
        }
 **/
    }

    @Override
    protected void onResume() {
        super.onResume();

        init();
        getMetadataValues();
    }

    static HandlerThread bgThread = new HandlerThread("VideoRenderHandlerThread");
    static {
        bgThread.start();
    }
    public Handler mHandler = new Handler(bgThread.getLooper()) {
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
                    showPublished(resultUrl);
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

    public void showPublished(final String postUrl) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                Log.d(TAG, "dialog for showing published url: " + postUrl);
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:

                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(postUrl));
                                startActivity(i);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:

                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(ReviewMediaActivity.this);
                builder.setMessage(getString(R.string.view_published_media_online))
                        .setPositiveButton(android.R.string.yes, dialogClickListener)
                        .setNegativeButton(android.R.string.no, dialogClickListener).show();
            }
        });
    }
}