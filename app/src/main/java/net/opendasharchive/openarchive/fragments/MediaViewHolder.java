package net.opendasharchive.openarchive.fragments;

import android.content.Context;
import android.graphics.Bitmap;
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

    public MediaViewHolder(final View itemView, Context context) {
        super(itemView);

        mContext = context;

        ivIcon = itemView.findViewById(R.id.ivIcon);
        tvTitle = itemView.findViewById(R.id.tvTitle);
        tvCreateDate = itemView.findViewById(R.id.tvCreateDate);
        tvWave = itemView.findViewById(R.id.event_item_sound);

    }

    public void bindData(final Media currentMedia) {


        if (currentMedia.getMimeType().startsWith("image")||currentMedia.getMimeType().startsWith("video")) {

            Picasso.get().load(currentMedia.getThumbnailUri()).fit().centerCrop().into(ivIcon);
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

        tvTitle.setText(currentMedia.getTitle());
        tvCreateDate.setText(currentMedia.getFormattedCreateDate());
    }
}
