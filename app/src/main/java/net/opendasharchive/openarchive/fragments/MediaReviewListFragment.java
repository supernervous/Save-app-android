package net.opendasharchive.openarchive.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.MediaAdapter;

import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MediaReviewListFragment extends MediaListFragment {

    protected TextView mActionUpload;

    protected long mStatus = Media.STATUS_LOCAL;
    protected long[] mStatuses = {Media.STATUS_LOCAL};

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MediaReviewListFragment() {
    }

    public void setStatus (long status) {
        mStatus = status;
    }

    public void refresh ()
    {
        if (mMediaAdapter != null)
        {
            List<Media> listMedia = Media.getMediaByStatus(mStatuses);


            mMediaAdapter.updateData(listMedia);

        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_media_list, container, false);
        rootView.setTag(TAG);

        mRecyclerView = rootView.findViewById(R.id.recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setHasFixedSize(true);

        mActionUpload = rootView.findViewById(R.id.action_upload);

        List<Media> listMedia = Media.getMediaByStatus(mStatuses);

        mMediaAdapter = new MediaAdapter(getActivity(), R.layout.activity_media_list_row,listMedia, mRecyclerView );
        mMediaAdapter.setDoImageFade(false);
        mRecyclerView.setAdapter(mMediaAdapter);

        return rootView;
    }


}