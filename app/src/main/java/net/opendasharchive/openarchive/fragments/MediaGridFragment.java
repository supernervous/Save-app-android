package net.opendasharchive.openarchive.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

        SectionedRecyclerViewAdapter sectionAdapter = new SectionedRecyclerViewAdapter();

        List<Media> mediaList = Media.getMediaByProjectAndStatus(mProjectId,"=",Media.STATUS_LOCAL);
        String title = "Waiting for upload";
        MediaSection section = new MediaSection(getContext(),mRecyclerView,R.layout.activity_media_list_square,title,mediaList);
        sectionAdapter.addSection(section);

        mediaList = Media.getMediaByProjectAndStatus(mProjectId,"=",Media.STATUS_PUBLISHED);
        title = "Uploaded";
        section = new MediaSection(getContext(),mRecyclerView,R.layout.activity_media_list_square,title,mediaList);
        sectionAdapter.addSection(section);

        mRecyclerView.setAdapter(sectionAdapter);

        return rootView;
    }


}