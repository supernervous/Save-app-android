package net.opendasharchive.openarchive.fragments;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
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

import java.io.File;

public class MediaViewHolder extends RecyclerView.ViewHolder {

    private ImageView ivIcon;
    private TextView tvTitle;
    private TextView tvCreateDate;
    private SimpleWaveformView tvWave;

    private Context mContext;

    private Picasso mPicasso;

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

        String mediaPath = currentMedia.getOriginalFilePath();

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

            /**
            final File fileSound = new File(currentMedia.getOriginalFilePath());
            try {
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

                tvWave.setAudioFile(soundFile);
                tvWave.setVisibility(View.VISIBLE);
                ivIcon.setVisibility(View.GONE);


            } catch (Exception e) {
                Log.e(getClass().getName(),"error loading sound file",e);
            }**/

            ivIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.audio_waveform));

        }
        else
            ivIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.no_thumbnail));

        if (currentMedia.status == Media.STATUS_QUEUED)
            tvTitle.setText("Waiting to upload...");
        else
            tvTitle.setText(currentMedia.getTitle());


        tvCreateDate.setText(currentMedia.getFormattedCreateDate());
    }
}
