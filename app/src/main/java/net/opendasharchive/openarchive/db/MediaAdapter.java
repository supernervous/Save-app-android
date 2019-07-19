package net.opendasharchive.openarchive.db;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.opendasharchive.openarchive.util.Globals;
import net.opendasharchive.openarchive.ReviewMediaActivity;
import net.opendasharchive.openarchive.fragments.MediaViewHolder;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by micahjlucas on 1/20/15.
 */
public class MediaAdapter extends RecyclerView.Adapter {

    private Context mContext;
    private int layoutResourceId;
    private List<Media> data;
    private RecyclerView recyclerview;
    private boolean doImageFade = true;

    public MediaAdapter(Context context, int layoutResourceId, List<Media> data, RecyclerView recyclerView) {
        super();
        this.layoutResourceId = layoutResourceId;
        this.mContext = context;
        this.data = data;
        this.recyclerview = recyclerView;
    }

    public void setDoImageFade (boolean doImageFade)
    {
        this.doImageFade = doImageFade;
    }

    public List<Media> getMediaList ()
    {
        return data;
    }

    public boolean updateItem (long mediaId, long progress)
    {
        for (int i = 0; i < data.size(); i++)
        {
            Media item = data.get(i);
            if (item.getId() == mediaId)
            {
                item.progress = progress;
                notifyItemChanged(i);

               return true;
            }
        }

        return false;
    }

    public void updateData (List<Media> data)
    {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int itemPosition = recyclerview.getChildLayoutPosition(view);

                Intent reviewMediaIntent = new Intent(mContext, ReviewMediaActivity.class);
                reviewMediaIntent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, data.get(itemPosition).getId());
                reviewMediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mContext.startActivity(reviewMediaIntent);
            }
        });
        MediaViewHolder mvh = new MediaViewHolder(view, mContext);
        mvh.doImageFade = doImageFade;

        return mvh;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        ((MediaViewHolder) holder).bindData(data.get(position));
    }

    @Override
    public int getItemViewType(final int position) {
        return layoutResourceId;
    }

}