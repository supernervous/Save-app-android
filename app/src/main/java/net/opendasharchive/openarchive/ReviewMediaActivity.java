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
import android.os.Message;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.fragments.VideoRequestHandler;
import net.opendasharchive.openarchive.onboarding.FirstStartActivity;
import net.opendasharchive.openarchive.publish.PublishService;
import net.opendasharchive.openarchive.util.Globals;
import net.opendasharchive.openarchive.util.Utility;

import java.io.File;
import java.util.HashMap;

import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.model.Account;


public class ReviewMediaActivity extends AppCompatActivity {
    private static String TAG = "ReviewMediaActivity";

    private Context mContext = this;
    private Media mMedia;
    private ProgressDialog progressDialog = null;

    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvAuthor;
    private TextView tvTags;
    private TextView tvLocation;
    private TextView tvUrl;
    private TextView tvLicenseUrl;


    private SwitchCompat tbDeriv, tbShare, tbComm;

    private MenuItem menuShare;
    private MenuItem menuPublish;


    private static Picasso mPicasso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_media);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (mPicasso == null) {
            VideoRequestHandler videoRequestHandler = new VideoRequestHandler(mContext);

            mPicasso = new Picasso.Builder(mContext)
                    .addRequestHandler(videoRequestHandler)
                    .build();
        }

        // instantiate values
        tvTitle = (TextView) findViewById(R.id.tv_title_lbl);
        tvDescription = (TextView) findViewById(R.id.tv_description_lbl);
        tvAuthor = (TextView) findViewById(R.id.tv_author_lbl);
        tvTags = (TextView) findViewById(R.id.tv_tags_lbl);
        tvLocation = (TextView) findViewById(R.id.tv_location_lbl);
        tvUrl = (TextView) findViewById(R.id.tv_url);

        tbDeriv = (SwitchCompat)findViewById(R.id.tb_cc_deriv);
        tbDeriv.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setLicense();
            }
        });
        tbShare = (SwitchCompat)findViewById(R.id.tb_cc_sharealike);
        tbShare.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setLicense();
            }
        });
        tbComm = (SwitchCompat)findViewById(R.id.tb_cc_comm);
        tbComm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setLicense();
            }
        });

        tvLicenseUrl = (TextView) findViewById(R.id.tv_cc_license);

        setTitle("");

    }

    private void bindMedia ()
    {

        // set values
        tvTitle.setText(mMedia.getTitle());
        tvDescription.setText(mMedia.getDescription());
        tvAuthor.setText(mMedia.getAuthor());
        tvLocation.setText(mMedia.getLocation());
        tvTags.setText(mMedia.getTags());

        tvLicenseUrl.setText(mMedia.getLicenseUrl());


        if (mMedia.getServerUrl() != null)
        {
            mMedia.status = Media.STATUS_PUBLISHED;

            tvUrl.setText( Html.fromHtml("Your media is available at <a href=\"" + mMedia.getServerUrl() + "\">" + mMedia.getServerUrl() + "</a>"));
            tvUrl.setMovementMethod(LinkMovementMethod.getInstance());
            tvUrl.setVisibility(View.VISIBLE);

            tvLicenseUrl.setMovementMethod(LinkMovementMethod.getInstance());

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

            tvLicenseUrl.setEnabled(false);
            if (TextUtils.isEmpty(mMedia.getLicenseUrl()))
                tvTags.setVisibility(View.GONE);

            findViewById(R.id.groupLicenseChooser).setVisibility(View.GONE);
        }
        else if (mMedia.status != Media.STATUS_QUEUED)
        {
            mMedia.status = Media.STATUS_LOCAL;
        }

        if (menuPublish != null) {
            if (mMedia.getServerUrl() == null) {
                menuPublish.setVisible(true);
                menuShare.setVisible(false);

            } else {
                menuShare.setVisible(true);
                menuPublish.setVisible(false);

            }
        }

    }

    private void setLicense ()
    {

        //the default
        String licenseUrl = "https://creativecommons.org/licenses/by/4.0/";

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

        tvLicenseUrl.setText(licenseUrl);

        mMedia.setLicenseUrl(licenseUrl);
    }

    private void saveMedia ()
    {
        if (tvTitle.getText().length() > 0)
            mMedia.setTitle(tvTitle.getText().toString());
        else
        {
            //use the file name if the user doesn't set a title
            mMedia.setTitle(new File(mMedia.getOriginalFilePath()).getName());
        }

        mMedia.setDescription(tvDescription.getText().toString());
        mMedia.setAuthor(tvAuthor.getText().toString());
        mMedia.setLocation(tvLocation.getText().toString());
        mMedia.setTags(tvTags.getText().toString());

        setLicense();

        mMedia.save();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveMedia();
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_review_media, menu);

        menuShare = menu.findItem(R.id.menu_item_share);
        menuPublish = menu.findItem(R.id.menu_item_publish);

        if (mMedia.getServerUrl() == null)
            menuPublish.setVisible(true);
        else
            menuShare.setVisible(true);

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
            case R.id.menu_item_publish:
                uploadMedia();
                break;
            case R.id.menu_item_nearby:
                startNearby();
                break;
            case R.id.menu_item_share_link:
                shareLink();
                break;
            case R.id.menu_item_share_media:
                shareMedia();
                break;
            case R.id.menu_item_share_torrent:
                shareTorrentLink();
                break;
           // case R.id.menu_item_stamp:
            //    stampMedia();
             //   break;
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

        if (mMedia.getMimeType().startsWith("image")) {

            mPicasso.load(Uri.parse(mMedia.getOriginalFilePath())).fit().centerCrop().into(ivMedia);
        }
        else if (mMedia.getMimeType().startsWith("video")) {

            mPicasso.load(VideoRequestHandler.SCHEME_VIDEO + ":" + mMedia.getOriginalFilePath()).fit().centerCrop().into(ivMedia);
        }
        else if (mMedia.getMimeType().startsWith("audio"))
            ivMedia.setImageDrawable(getResources().getDrawable(R.drawable.audio_waveform));
        else
            ivMedia.setImageDrawable(getResources().getDrawable(R.drawable.no_thumbnail));

        ivMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMedia();
            }
        });
    }

    private void startNearby ()
    {
        Intent intent = new Intent(this, NearbyActivity.class);
        intent.putExtra("isServer",true);

        intent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, mMedia.getId());

        startActivity(intent);
    }

    private void showMedia ()
    {
        Intent viewMediaIntent = new Intent();
        viewMediaIntent.setAction(android.content.Intent.ACTION_VIEW);

        Uri sharedFileUri = null;

        String mediaPath = mMedia.getOriginalFilePath();
        if (mediaPath.startsWith("content:"))
            sharedFileUri = Uri.parse(mediaPath);
        else
            sharedFileUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider"
                    , new File(mMedia.getOriginalFilePath()));

        viewMediaIntent.setDataAndType(sharedFileUri, mMedia.getMimeType().split("/")[0] + "/*");
        startActivity(viewMediaIntent);
    }

    private void uploadMedia ()
    {
        Account account = new Account(this, null);

        // if user doesn't have an account
        if(!account.isAuthenticated()) {
            Intent firstStartIntent = new Intent(this, FirstStartActivity.class);
            startActivity(firstStartIntent);

        }
        else {

            //mark queued
            mMedia.status = Media.STATUS_QUEUED;
            saveMedia();

            startService(new Intent(this, PublishService.class));
        }


    }

    //share the link to the file on the IA
    private void shareLink ()
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

    //share the link to the file on the IA
    private void shareTorrentLink ()
    {

        StringBuffer sb = new StringBuffer();
        sb.append("\"").append(mMedia.getTitle()).append("\"").append(' ');
        sb.append(getString(R.string.share_torrent_text)).append(' ');

        StringBuffer sbTorrentUrl = new StringBuffer();

        String tagId = Uri.parse(mMedia.getServerUrl()).getLastPathSegment();

        sb.append("https://archive.org/download/");
        sb.append(tagId);
        sb.append("/");
        sb.append(tagId);
        sb.append("_archive.torrent");

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mMedia.getTitle());
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_using)));
    }

    //share the link to the file on the IA
    private void shareMedia ()
    {

        StringBuffer sb = new StringBuffer();
        sb.append("\"").append(mMedia.getTitle()).append("\"").append(' ');
        sb.append(getString(R.string.share_media_text)).append(' ');
        sb.append(mMedia.getServerUrl());

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType(mMedia.getMimeType());
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mMedia.getTitle());
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, sb.toString());

        Uri sharedFileUri = null;
        String mediaPath = mMedia.getOriginalFilePath();
        if (mediaPath.startsWith("content:"))
            sharedFileUri = Uri.parse(mediaPath);
        else
            sharedFileUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider"
                    , new File(mMedia.getOriginalFilePath()));

        sharingIntent.putExtra(Intent.EXTRA_STREAM, sharedFileUri);

        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_using)));
    }





    @Override
    protected void onResume() {
        super.onResume();

        init();
        bindMedia();
    }



    private void showSuccess ()
    {
        Toast.makeText(this,getString(R.string.upload_success),Toast.LENGTH_SHORT).show();
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