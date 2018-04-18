package net.opendasharchive.openarchive.fragments;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
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
import java.util.HashMap;

public class MediaViewHolder extends RecyclerView.ViewHolder {

    private ImageView ivIcon;
    private TextView tvTitle;
    private TextView tvCreateDate;
    private SimpleWaveformView tvWave;

    private Context mContext;

    private Picasso mPicasso;
    private AsyncTask<Void, Void, SoundFile> asyncTask;
    public static HashMap<String,SoundFile> mSoundFileCache = new HashMap<>();

    public MediaViewHolder(final View itemView, Context context) {
        super(itemView);

        mContext = context;

        ivIcon = itemView.findViewById(R.id.ivIcon);
        tvTitle = itemView.findViewById(R.id.tvTitle);
        tvCreateDate = itemView.findViewById(R.id.tvCreateDate);
        tvWave = itemView.findViewById(R.id.event_item_sound);

        if (mPicasso == null) {
            VideoRequestHandler videoRequestHandler = new VideoRequestHandler(mContext);

            mPicasso = new Picasso.Builder(mContext)
                    .addRequestHandler(videoRequestHandler)
                    .build();
        }
    }

    public void bindData(final Media currentMedia) {

        final String mediaPath = currentMedia.getOriginalFilePath();

        if (currentMedia.getMimeType().startsWith("image")) {

            mPicasso.load(Uri.parse(currentMedia.getOriginalFilePath())).fit().centerCrop().into(ivIcon);
            ivIcon.setVisibility(View.VISIBLE);
            tvWave.setVisibility(View.GONE);

        }
        else  if (currentMedia.getMimeType().startsWith("video")) {

            if (currentMedia.getThumbnailUri() == null
                    && mediaPath.startsWith("content")) {

                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = mContext.getContentResolver().query(Uri.parse(mediaPath), filePathColumn, null, null, null);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String thumbPath = cursor.getString(columnIndex);
                    currentMedia.setThumbnailFilePath(thumbPath);
                    currentMedia.save();
                    cursor.close();
            }

            if (currentMedia.getThumbnailUri() != null)
            {
                mPicasso.load(currentMedia.getThumbnailUri()).fit().centerCrop().into(ivIcon);
            }
            else
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

        if (currentMedia.status == Media.STATUS_QUEUED)
            sbTitle.append(mContext.getString(R.string.status_waiting));
        else if (currentMedia.status == Media.STATUS_PUBLISHED)
            sbTitle.append(mContext.getString(R.string.status_public));
        else if (currentMedia.status == Media.STATUS_UPLOADING)
            sbTitle.append(mContext.getString(R.string.status_uploading));

        if (sbTitle.length() > 0)
            sbTitle.append(": ");

        sbTitle.append(currentMedia.getTitle());
        tvTitle.setText(sbTitle.toString());

        tvCreateDate.setText(currentMedia.getFormattedCreateDate());
    }
}
