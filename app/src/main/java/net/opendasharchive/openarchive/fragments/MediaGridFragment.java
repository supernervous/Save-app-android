package net.opendasharchive.openarchive.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.opendasharchive.openarchive.BatchMediaReviewActivity;
import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.MediaAdapter;
import net.opendasharchive.openarchive.db.MediaSection;

import java.util.List;

import androidx.recyclerview.widget.GridLayoutManager;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

public class MediaGridFragment extends MediaListFragment {

    private int numberOfColumns = 3;

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
        View rootView = inflater.inflate(R.layout.fragment_media_list, container, false);
        rootView.setTag(TAG);

        GridLayoutManager lMan = new GridLayoutManager(getActivity(), numberOfColumns);

        mRecyclerView = rootView.findViewById(R.id.recyclerview);
        mRecyclerView.setLayoutManager(lMan);

        mActionUpload = rootView.findViewById(R.id.action_upload);

        List<Media> listMedia = Media.getMediaByProject(mProjectId);

        for (Media media : listMedia)
        {
            if (media.status == Media.STATUS_LOCAL)
            {
                mActionUpload.setVisibility(View.VISIBLE);
                mActionUpload.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(getActivity(), BatchMediaReviewActivity.class));
                    }
                });
                break;
            }
        }

        mMediaAdapter = new MediaAdapter(getActivity(), R.layout.activity_media_list_square,listMedia, mRecyclerView );
        mRecyclerView.setAdapter(mMediaAdapter);

        return rootView;
    }

    public void refresh ()
    {
        if (mMediaAdapter != null)
        {
            List<Media> listMedia = null;

            if (mProjectId == -1)
            {
                listMedia = Media.getMediaByStatus(mStatuses);

            }
            else
            {
                listMedia = Media.getMediaByProject(mProjectId);
            }

            mActionUpload.setVisibility(View.GONE);

            for (Media media : listMedia)
            {
                if (media.status == Media.STATUS_LOCAL)
                {
                    mActionUpload.setVisibility(View.VISIBLE);
                    mActionUpload.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(getActivity(), BatchMediaReviewActivity.class));
                        }
                    });
                    break;
                }
            }

            mMediaAdapter.updateData(listMedia);

        }

    }

}