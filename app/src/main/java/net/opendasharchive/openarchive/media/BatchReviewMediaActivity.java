package net.opendasharchive.openarchive.media;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.github.derlio.waveform.soundfile.SoundFile;
import com.squareup.picasso.Picasso;
import com.stfalcon.frescoimageviewer.ImageViewer;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.Project;
import net.opendasharchive.openarchive.fragments.MediaViewHolder;
import net.opendasharchive.openarchive.fragments.VideoRequestHandler;
import net.opendasharchive.openarchive.util.Globals;

import java.io.File;
import java.util.ArrayList;


public class BatchReviewMediaActivity extends AppCompatActivity {
    private static String TAG = "ReviewMediaActivity";

    private Context mContext = this;
    private ArrayList<Media> mediaList;

    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvAuthor;
    private TextView tvTags;
    private TextView tvLocation;
    private TextView tvUrl;
    private TextView tvLicenseUrl;
    private TextView tvFlagged;


    private LinearLayout viewMediaParent;

   // private SimpleWaveformView swMedia;
   // private ImageView ivMedia;

    private SwitchCompat tbDeriv, tbShare, tbComm;


    private ImageView ivEditTags, ivEditLocation, ivEditNotes, ivEditFlag;

    private static Picasso mPicasso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_review_media);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
     //   actionbar.setHomeAsUpIndicator(R.drawable.arrow_back);

        if (mPicasso == null) {
            VideoRequestHandler videoRequestHandler = new VideoRequestHandler(mContext);

            mPicasso = new Picasso.Builder(mContext)
                    .addRequestHandler(videoRequestHandler)
                    .build();
        }

        // instantiate values
       // swMedia = findViewById(R.id.swMedia);

        // display media preview if available
       // ivMedia = (ImageView) findViewById(R.id.ivMedia);

        viewMediaParent = findViewById(R.id.item_display);

        tvTitle = (TextView) findViewById(R.id.tv_title_lbl);
        tvDescription = (TextView) findViewById(R.id.tv_description_lbl);
        tvAuthor = (TextView) findViewById(R.id.tv_author_lbl);
        tvTags = (TextView) findViewById(R.id.tv_tags_lbl);
        tvLocation = (TextView) findViewById(R.id.tv_location_lbl);
        tvUrl = (TextView) findViewById(R.id.tv_url);
        tvFlagged = findViewById(R.id.tv_flag_lbl);


        ivEditTags = findViewById(R.id.ivEditTags);
        ivEditNotes = findViewById(R.id.ivEditNotes);
        ivEditLocation = findViewById(R.id.ivEditLocation);
        ivEditFlag = findViewById(R.id.ivEditFlag);

        /**
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
        });**/

        tvLicenseUrl = (TextView) findViewById(R.id.tv_cc_license);

        setTitle("");


    }

    private void updateFlagState ()
    {
        for (Media media : mediaList)
            media.setFlagged(!mediaList.get(0).isFlagged());

        updateFlagState(mediaList.get(0));
    }

    private void updateFlagState (Media media)
    {
        if (media.isFlagged())
            ivEditFlag.setImageResource(R.drawable.ic_flag_selected);
        else
            ivEditFlag.setImageResource(R.drawable.ic_flag_unselected);

        if (media.isFlagged())
            tvFlagged.setText(R.string.status_flagged);
        else
            tvFlagged.setText(R.string.hint_flag);

        if ((media.status != Media.STATUS_LOCAL
                && media.status != Media.STATUS_NEW) && (!media.isFlagged()))
        {
            ivEditFlag.setVisibility(View.GONE);
            tvFlagged.setVisibility(View.GONE);
        }
    }

    private void bindMedia ()
    {
        bindMedia(mediaList.get(0));

        viewMediaParent.removeAllViews();

        for (Media media: mediaList)
          showThumbnail(media);
    }

    private void bindMedia (Media media)
    {

        // set values
        tvTitle.setText(media.getTitle());

        if (!TextUtils.isEmpty(media.getDescription())) {
            tvDescription.setText(media.getDescription());
            ivEditNotes.setImageResource(R.drawable.ic_edit_selected);
        }



        if (!TextUtils.isEmpty(media.getLocation())) {
            tvLocation.setText(media.getLocation());
            ivEditLocation.setImageResource(R.drawable.ic_location_selected);
        }

        if (!TextUtils.isEmpty(media.getTags())) {
            tvTags.setText(media.getTags());
            ivEditTags.setImageResource(R.drawable.ic_tag_selected);
        }

        tvAuthor.setText(media.getAuthor());
        tvLicenseUrl.setText(media.getLicenseUrl());

        if (media.status != Media.STATUS_LOCAL
                && media.status != Media.STATUS_NEW)
        {

            if (media.status == Media.STATUS_UPLOADED||media.status == Media.STATUS_PUBLISHED) {
            //    tvUrl.setText(Html.fromHtml(getString(R.string.your_media_is_available) + " <a href=\"" + media.getServerUrl() + "\">" + media.getServerUrl() + "</a>"));
             //   tvUrl.setMovementMethod(LinkMovementMethod.getInstance());
              //  tvUrl.setVisibility(View.VISIBLE);
            }
            else if (media.status == Media.STATUS_QUEUED) {
                tvUrl.setText("Waiting for upload...");
                tvUrl.setVisibility(View.VISIBLE);
            }
            else if (media.status == Media.STATUS_UPLOADING) {
                tvUrl.setText("Uploading now...");
                tvUrl.setVisibility(View.VISIBLE);
            }

            tvLicenseUrl.setMovementMethod(LinkMovementMethod.getInstance());

            tvTitle.setEnabled(false);

            tvDescription.setEnabled(false);
            if (TextUtils.isEmpty(media.getDescription())) {
                ivEditNotes.setVisibility(View.GONE);
                tvDescription.setHint("");
            }

            tvAuthor.setEnabled(false);

            tvLocation.setEnabled(false);
            if (TextUtils.isEmpty(media.getLocation())) {
                ivEditLocation.setVisibility(View.GONE);
                tvLocation.setHint("");
            }

            tvTags.setEnabled(false);
            if (TextUtils.isEmpty(media.getTags())) {
                ivEditTags.setVisibility(View.GONE);
                tvTags.setHint("");
            }

            tvLicenseUrl.setEnabled(false);

            findViewById(R.id.groupLicenseChooser).setVisibility(View.GONE);
        }
        else
        {

            findViewById(R.id.row_flag).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    updateFlagState();
                }
            });


        }

        updateFlagState (media);

    }

    private void setLicense (Media media)
    {
        Project project = Project.getById(media.getProjectId());
        media.setLicenseUrl(project.getLicenseUrl());
    }

    private void saveMedia ()
    {
        for (Media media : mediaList)
            saveMedia(media);
    }

    private void saveMedia (Media media)
    {
        //if deleted
        if (media == null)
            return;

        if (tvTitle.getText().length() > 0)
            media.setTitle(tvTitle.getText().toString());
        else
        {
            //use the file name if the user doesn't set a title
            media.setTitle(new File(media.getOriginalFilePath()).getName());
        }

        media.setDescription(tvDescription.getText().toString());
        media.setAuthor(tvAuthor.getText().toString());
        media.setLocation(tvLocation.getText().toString());
        media.setTags(tvTags.getText().toString());

        setLicense(media);

        if (media.status == Media.STATUS_NEW)
            media.status = Media.STATUS_LOCAL;

        media.save();
    }

    @Override
    protected void onPause() {
        super.onPause();

        saveMedia();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {



        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent intent = null;
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;


            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void init() {
        Intent intent = getIntent();

        long[] mediaIds = intent.getLongArrayExtra(Globals.EXTRA_CURRENT_MEDIA_ID);

        mediaList = new ArrayList<Media>();
        for (long mediaId : mediaIds)
            mediaList.add(Media.getMediaById(mediaId));

        // get default metadata sharing values
        SharedPreferences sharedPref = this.getSharedPreferences(Globals.PREF_FILE_KEY, Context.MODE_PRIVATE);

        bindMedia();

    }

    private void showThumbnail (Media media)
    {
        ImageView ivMedia = new ImageView(this);
        int margin = 3;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(margin, margin, margin, margin);
        lp.height=600;
        lp.width=800;
        ivMedia.setLayoutParams(lp);
        ivMedia.setScaleType(ImageView.ScaleType.CENTER_CROP);


        if (media.getMimeType().startsWith("image")) {

            mPicasso.load(Uri.parse(media.getOriginalFilePath())).fit().centerCrop().into(ivMedia);
        }
        else if (media.getMimeType().startsWith("video")) {

            mPicasso.load(VideoRequestHandler.SCHEME_VIDEO + ":" + media.getOriginalFilePath()).fit().centerCrop().into(ivMedia);
        }
        else if (media.getMimeType().startsWith("audio")) {
            ivMedia.setImageDrawable(getResources().getDrawable(R.drawable.audio_waveform));

            SoundFile soundFile = MediaViewHolder.mSoundFileCache.get( media.getOriginalFilePath());
            if (soundFile != null) {
              //  swMedia.setAudioFile(soundFile);
               // swMedia.setVisibility(View.VISIBLE);
               // ivMedia.setVisibility(View.GONE);
            }
        }
        else
            ivMedia.setImageDrawable(getResources().getDrawable(R.drawable.no_thumbnail));

        viewMediaParent.addView(ivMedia);

    }

    private void showMedia (Media media)
    {

        if (media.mimeType.startsWith("image"))
        {
            ArrayList<Uri> list = new ArrayList<>();
            list.add(Uri.parse(media.getOriginalFilePath()));
            new ImageViewer.Builder(this,list)
                    .setStartPosition(0)
                    .show();
        }
        else {
            /**
            Intent viewMediaIntent = new Intent();
            viewMediaIntent.setAction(Intent.ACTION_VIEW);

            Uri uriFile = FileProvider.getUriForFile(this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    FileUtils.getFile(this,Uri.parse(media.getOriginalFilePath())));

            viewMediaIntent.setDataAndType(uriFile, media.getMimeType());
            viewMediaIntent.putExtra(Intent.EXTRA_STREAM,uriFile);
            viewMediaIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(viewMediaIntent);
             **/
        }
    }



    @Override
    protected void onResume() {
        super.onResume();

        init();
        bindMedia();

    }





    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        super.onDestroy();
    }

}