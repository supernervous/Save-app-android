package net.opendasharchive.openarchive.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.derlio.waveform.SimpleWaveformView;
import com.github.derlio.waveform.soundfile.SoundFile;
import com.squareup.picasso.Picasso;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.util.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.HashMap;

import androidx.recyclerview.widget.RecyclerView;

public class MediaViewHolder extends RecyclerView.ViewHolder {

    private View mView;
    private ImageView ivIcon;
    private TextView tvTitle;
    private TextView tvCreateDate;
    private SimpleWaveformView tvWave;
    private ImageView ivStatus;

    private Context mContext;

    private static Picasso mPicasso;
    private AsyncTask<Void, Void, SoundFile> asyncTask;
    public static HashMap<String,SoundFile> mSoundFileCache = new HashMap<>();

    public MediaViewHolder(final View itemView, Context context) {
        super(itemView);

        mView= itemView;
        mContext = context;

        ivIcon = itemView.findViewById(R.id.ivIcon);
        tvTitle = itemView.findViewById(R.id.tvTitle);
        tvCreateDate = itemView.findViewById(R.id.tvCreateDate);
        tvWave = itemView.findViewById(R.id.event_item_sound);
        ivStatus = itemView.findViewById(R.id.ivStatus);

        if (mPicasso == null) {
            VideoRequestHandler videoRequestHandler = new VideoRequestHandler(mContext);

            mPicasso = new Picasso.Builder(mContext)
                    .addRequestHandler(videoRequestHandler)
                    .build();
        }

    }

    public void bindData(final Media currentMedia) {

        final String mediaPath = currentMedia.getOriginalFilePath();

        mView.setTag(currentMedia.getId());

        if (currentMedia.getMimeType().startsWith("image")) {

            mPicasso.load(Uri.parse(currentMedia.getOriginalFilePath())).fit().centerCrop().into(ivIcon);
            ivIcon.setVisibility(View.VISIBLE);
            tvWave.setVisibility(View.GONE);

        }
        else  if (currentMedia.getMimeType().startsWith("video")) {

            mPicasso.load(VideoRequestHandler.SCHEME_VIDEO + ":" + currentMedia.getOriginalFilePath()).fit().centerCrop().into(ivIcon);
            ivIcon.setVisibility(View.VISIBLE);
            tvWave.setVisibility(View.GONE);

        }
        else if (currentMedia.getMimeType().startsWith("audio")) {

            ivIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.no_thumbnail));

            if (mSoundFileCache.get(mediaPath)==null) {

                if (asyncTask == null) {
                    asyncTask = new AsyncTask<Void, Void, SoundFile>() {
                        protected void onPreExecute() {
                            // Pre Code
                        }

                        protected SoundFile doInBackground(Void... unused) {
                            File fileSound = FileUtils.getFile(mContext, Uri.parse(mediaPath));
                            try {
                                if (fileSound != null) {
                                    final SoundFile soundFile = SoundFile.create(fileSound.getPath(), new SoundFile.ProgressListener() {
                                        int lastProgress = 0;

                                        @Override
                                        public boolean reportProgress(double fractionComplete) {
                                            final int progress = (int) (fractionComplete * 100);
                                            if (lastProgress == progress) {
                                                return true;
                                            }
                                            lastProgress = progress;
                                            return true;
                                        }
                                    });

                                    mSoundFileCache.put(mediaPath, soundFile);
                                    return soundFile;
                                }
                                else
                                    return null;

                            } catch (Exception e) {
                                Log.e(getClass().getName(), "error loading sound file", e);
                            }

                            return null;
                        }

                        protected void onPostExecute(SoundFile soundFile) {
                            // Post Code

                            if (soundFile != null) {
                                tvWave.setAudioFile(soundFile);
                                tvWave.setVisibility(View.VISIBLE);
                                ivIcon.setVisibility(View.GONE);
                            }

                            asyncTask = null;
                        }
                    };

                    asyncTask.execute();
                }
            }
            else
            {
                tvWave.setAudioFile(mSoundFileCache.get(mediaPath));
                tvWave.setVisibility(View.VISIBLE);
                ivIcon.setVisibility(View.GONE);
            }


        }
        else
            ivIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.no_thumbnail));

        StringBuffer sbTitle = new StringBuffer();

        if (ivStatus != null) {
            ivStatus.setVisibility(View.VISIBLE);
            ivStatus.setImageResource(R.drawable.ic_info_black_24dp);
        }

        if (currentMedia.status == Media.STATUS_QUEUED) {
            sbTitle.append(mContext.getString(R.string.status_waiting));
            if (ivStatus != null) {
                ivStatus.setVisibility(View.VISIBLE);
                ivStatus.setImageResource(R.drawable.ic_cloud_upload_white_36dp);
            }
        } else if (currentMedia.status == Media.STATUS_UPLOADED||currentMedia.status == Media.STATUS_PUBLISHED) {
            sbTitle.append(mContext.getString(R.string.status_public));
            if (ivStatus != null) {
                ivStatus.setVisibility(View.VISIBLE);
                ivStatus.setImageResource(R.drawable.baseline_check_circle_outline_white_48);
            }
        } else if (currentMedia.status == Media.STATUS_UPLOADING) {
            sbTitle.append(mContext.getString(R.string.status_uploading));

             float perc = 0;

             if (currentMedia.contentLength > 0)
                perc = (int)(((float)currentMedia.progress) / ((float)currentMedia.contentLength) * 100f);

             sbTitle.append(" ").append(perc + "%");

            if (ivStatus != null) {
                ivStatus.setVisibility(View.VISIBLE);
                ivStatus.setImageResource(R.drawable.ic_cloud_upload_white_36dp);
            }
        }

        if (sbTitle.length() > 0)
            sbTitle.append(": ");

        sbTitle.append(currentMedia.getTitle());
        tvTitle.setText(sbTitle.toString());

        File fileMedia = new File(Uri.parse(currentMedia.getOriginalFilePath()).getPath());

        if (fileMedia.exists())
        {
            tvCreateDate.setText(readableFileSize(fileMedia.length()));
        }
        else {

            if (currentMedia.contentLength == -1)
            {
                try {
                    InputStream is = mContext.getContentResolver().openInputStream(Uri.parse(currentMedia.getOriginalFilePath()));
                    currentMedia.contentLength = is.available();
                    currentMedia.save();
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            if (currentMedia.contentLength > 0)
                tvCreateDate.setText(readableFileSize(currentMedia.contentLength));
            else
                tvCreateDate.setText(currentMedia.getFormattedCreateDate());
        }
    }

    public static String readableFileSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
