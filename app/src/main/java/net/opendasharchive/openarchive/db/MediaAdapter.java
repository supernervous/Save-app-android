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

        Bitmap bThumb = currentMedia.getThumbnail(mContext);

        if (bThumb != null)
            ivIcon.setImageBitmap(bThumb);
        else if (currentMedia.getMimeType().startsWith("audio"))
            ivIcon.setImageDrawable(getContext().getResources().getDrawable(R.drawable.audio_waveform));
        else
            ivIcon.setImageDrawable(getContext().getResources().getDrawable(R.drawable.no_thumbnail));

        tvTitle.setText(currentMedia.getTitle());
        tvCreateDate.setText(currentMedia.getFormattedCreateDate());

        return rowView;
    }
}