package net.opendasharchive.openarchive.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.opendasharchive.openarchive.db.MediaAdapter;
import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;

public class MediaListFragment extends Fragment {

    public MediaAdapter mMediaAdapter;
    protected RecyclerView mRecyclerView;

    private static final String TAG = "RecyclerViewFragment";
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MediaListFragment() {
    }

    public void refresh ()
    {
        mMediaAdapter.updateData(Media.getAllMediaAsList());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_media_list, container, false);
        rootView.setTag(TAG);

        mRecyclerView = rootView.findViewById(R.id.recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setHasFixedSize(true);

        mMediaAdapter = new MediaAdapter(getActivity(), R.layout.activity_media_list_row,Media.getAllMediaAsList(), mRecyclerView );
        mRecyclerView.setAdapter(mMediaAdapter);

        return rootView;
    }


}