package net.opendasharchive.openarchive.db;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;

import com.github.derlio.waveform.SimpleWaveformView;
import com.github.derlio.waveform.soundfile.SoundFile;

import java.io.File;
import java.util.List;

/**
 * Created by micahjlucas on 1/20/15.
 */
public class MediaAdapter extends ArrayAdapter<Media> {

    Context mContext;
    int layoutResourceId;
    List<Media> data;

    public MediaAdapter(Context context, int layoutResourceId, List<Media> data) {
        super(context, layoutResourceId, data);

        this.layoutResourceId = layoutResourceId;
        this.mContext = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View rowView, ViewGroup parent) {
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        rowView = inflater.inflate(layoutResourceId, parent, false);

        Media currentMedia = data.get(position);

        ImageView ivIcon = (ImageView)rowView.findViewById(R.id.ivIcon);
        TextView tvTitle = (TextView)rowView.findViewById(R.id.tvTitle);
        TextView tvCreateDate = (TextView)rowView.findViewById(R.id.tvCreateDate);
        SimpleWaveformView tvWave = (SimpleWaveformView)rowView.findViewById(R.id.event_item_sound);

        Bitmap bThumb = currentMedia.getThumbnail(mContext);

        if (bThumb != null) {
            ivIcon.setImageBitmap(bThumb);
            ivIcon.setVisibility(View.VISIBLE);
            tvWave.setVisibility(View.GONE);
        }
        else if (currentMedia.getMimeType().startsWith("audio")) {

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
                tvWave.invalidate();
            } catch (Exception e) {
            }
            //ivIcon.setImageDrawable(getContext().getResources().getDrawable(R.drawable.audio_waveform));

        }
        else
            ivIcon.setImageDrawable(getContext().getResources().getDrawable(R.drawable.no_thumbnail));

        tvTitle.setText(currentMedia.getTitle());
        tvCreateDate.setText(currentMedia.getFormattedCreateDate());

        return rowView;
    }
}