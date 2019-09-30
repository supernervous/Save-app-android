package net.opendasharchive.openarchive.db;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import net.opendasharchive.openarchive.fragments.MediaListFragment;
import net.opendasharchive.openarchive.util.Globals;
import net.opendasharchive.openarchive.ReviewMediaActivity;
import net.opendasharchive.openarchive.fragments.MediaViewHolder;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.view.MotionEventCompat;
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
    private final MediaListFragment.OnStartDragListener mDragStartListener;
    private boolean isEditMode = false;

    public MediaAdapter(Context context, int layoutResourceId, List<Media> data, RecyclerView recyclerView, MediaListFragment.OnStartDragListener dragStartListener) {
        super();
        this.layoutResourceId = layoutResourceId;
        this.mContext = context;
        this.data = data;
        this.recyclerview = recyclerView;
        this.mDragStartListener = dragStartListener;
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
                item.status = Media.STATUS_UPLOADING;
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

        int priority = data.size();

        for (Media media: data)
        {
            media.setPriority(priority--);
            media.save();
        }

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
        MediaViewHolder mvh = (MediaViewHolder)holder;

        mvh.bindData(data.get(position));

        if (mvh.handleView != null) {
            if (isEditMode)
                mvh.handleView.setVisibility(View.VISIBLE);
            else
                mvh.handleView.setVisibility(View.GONE);

            mvh.handleView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (MotionEventCompat.getActionMasked(event) ==
                            MotionEvent.ACTION_DOWN) {
                        if (mDragStartListener != null)
                            mDragStartListener.onStartDrag(holder);
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public int getItemViewType(final int position) {
        return layoutResourceId;
    }

    public void setEditMode (boolean isEditMode)
    {
        this.isEditMode = isEditMode;
    }

    public void onItemMove (int oldPos, int newPos)
    {
        Media mediaToMov = data.get(oldPos);

        data.remove(oldPos);
        data.add(newPos, mediaToMov);

        int priority = data.size();

        for (Media media: data)
        {
            media.setPriority(priority--);
            media.save();
        }

        notifyDataSetChanged();
    }

    public void onItemDismiss (int pos)
    {
        Media mediaToDismiss = data.get(pos);
        data.remove(mediaToDismiss);
        mediaToDismiss.status = Media.STATUS_LOCAL;
        mediaToDismiss.save();
        
        notifyDataSetChanged();


    }
}