package net.opendasharchive.openarchive.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.MediaAdapter;

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

        mMediaAdapter = new MediaAdapter(getActivity(), R.layout.activity_media_list_square,Media.getMediaByProject(mProjectId), mRecyclerView );

        mRecyclerView.setAdapter(mMediaAdapter);

        return rootView;
    }


}