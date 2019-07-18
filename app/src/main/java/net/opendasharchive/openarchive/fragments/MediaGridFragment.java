package net.opendasharchive.openarchive.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.opendasharchive.openarchive.BatchMediaReviewActivity;
import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Collection;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.MediaAdapter;
import net.opendasharchive.openarchive.db.MediaSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

public class MediaGridFragment extends MediaListFragment {

    private int numberOfColumns = 4;
    private HashMap<Long,MediaAdapter> mAdapters;
    private HashMap<Long,SectionViewHolder> mSection;

    private View mMainView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MediaGridFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mAdapters = new HashMap<>();
        mSection = new HashMap<>();

        mMainView = inflater.inflate(R.layout.fragment_media_list, container, false);
        mMainView.setTag(TAG);

        LinearLayout mainContainer = mMainView.findViewById(R.id.mediacontainer);

        List<Collection> listCollections = Collection.getAllAsList();

        for (Collection coll : listCollections) {

            List<Media> listMedia = Media.getMediaByProjectAndCollection(mProjectId, coll.getId());
            if (listMedia.size() > 0)
            {
                View view = createMediaList(mainContainer, coll, listMedia);
                mainContainer.addView(view);
            }
        }

        return mMainView;
    }

    private class SectionViewHolder {
        View mediaSection;
        RecyclerView.Recycler mediaGrid;
        TextView sectionStatus;
        TextView sectionTimestamp;
        View action;
    }

    private View createMediaList (LinearLayout mainContainer, Collection coll, List<Media> listMedia)
    {
        GridLayoutManager lMan = new GridLayoutManager(getActivity(), numberOfColumns) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };

        SectionViewHolder holder = new SectionViewHolder();

        holder.mediaSection = getLayoutInflater().inflate(R.layout.fragment_media_list_section, null);
        RecyclerView rView = holder.mediaSection.findViewById(R.id.recyclerview);
        rView.setHasFixedSize(true);
        rView.setLayoutManager(lMan);

        holder.sectionStatus = holder.mediaSection.findViewById(R.id.sectionstatus);
        holder.sectionTimestamp = holder.mediaSection.findViewById(R.id.sectiontimestamp);
        holder.action = holder.mediaSection.findViewById(R.id.action_next);

        setSectionHeaders(coll, listMedia, holder);

        MediaAdapter mediaAdapter = new MediaAdapter(getActivity(), R.layout.activity_media_list_square,listMedia, rView );
        rView.setAdapter(mediaAdapter);
        mAdapters.put(coll.getId(),mediaAdapter);
        mSection.put(coll.getId(),holder);

        return holder.mediaSection;
    }

    public void updateItem (long mediaId, long progress)
    {
        for (MediaAdapter adapter : mAdapters.values())
            adapter.updateItem(mediaId, progress);

    }

    public void refresh ()
    {

        LinearLayout mainContainer = mMainView.findViewById(R.id.mediacontainer);

        List<Collection> listCollections = Collection.getAllAsList();

        for (Collection coll : listCollections) {

            List<Media> listMedia = Media.getMediaByProjectAndCollection(mProjectId, coll.getId());

            MediaAdapter adapter = mAdapters.get(coll.getId());
            SectionViewHolder holder = mSection.get(coll.getId());

            if (adapter != null)
            {
                adapter.updateData(listMedia);

                setSectionHeaders(coll, listMedia, holder);

            }
            else if (listMedia.size() > 0)
            {
                View view = createMediaList(mainContainer, coll,listMedia);
                mainContainer.addView(view,0);
            }
        }

    }

    private void setSectionHeaders (Collection coll, List<Media> listMedia, SectionViewHolder holder)
    {
        for (Media media : listMedia)
        {
            if (media.status == Media.STATUS_LOCAL)
            {
                holder.sectionStatus.setText("READY TO UPLOAD");
                holder.sectionTimestamp.setText(listMedia.size() + " item(s)");
                holder.action.setVisibility(View.VISIBLE);
                holder.action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(getActivity(), BatchMediaReviewActivity.class));
                    }
                });

            }
            else if (media.status == Media.STATUS_QUEUED || media.status == Media.STATUS_UPLOADING)
            {
                holder.sectionStatus.setText("UPLOADING");
                if (coll.getUploadDate() != null)
                    holder.sectionTimestamp.setText(coll.getUploadDate().toLocaleString());
                else if (listMedia.size() > 0 && listMedia.get(0).uploadDate != null)
                    holder.sectionTimestamp.setText(listMedia.get(0).uploadDate.toString());

                holder.action.setVisibility(View.GONE);
            }
            else
            {
                holder.sectionStatus.setText(listMedia.size() + " items uploaded");
                if (coll.getUploadDate() != null)
                    holder.sectionTimestamp.setText(coll.getUploadDate().toLocaleString());
                else if (listMedia.size() > 0 && listMedia.get(0).uploadDate != null)
                    holder.sectionTimestamp.setText(listMedia.get(0).uploadDate.toString());

                holder.action.setVisibility(View.GONE);
            }
        }
    }
}