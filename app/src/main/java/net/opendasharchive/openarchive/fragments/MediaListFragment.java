package net.opendasharchive.openarchive.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.MediaAdapter;

import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MediaListFragment extends Fragment {

    protected RecyclerView mRecyclerView;
    MediaAdapter mMediaAdapter;
    protected static final String TAG = "RecyclerViewFragment";

    protected long mProjectId = -1;
    protected long mStatus = Media.STATUS_UPLOADING;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MediaListFragment() {
    }

    public void setStatus (long status) {
        mStatus = status;
    }

    public void setProjectId (long projectId)
    {
        mProjectId = projectId;
    }

    public void refresh ()
    {
        if (mMediaAdapter != null)
        {
            List<Media> listMedia = null;

            if (mProjectId == -1)
            {
                listMedia = Media.getMediaByStatus(mStatus);

            }
            else
            {
                listMedia = Media.getMediaByProject(mProjectId);
            }

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

        List<Media> listMedia = null;

        if (mProjectId == -1)
        {
            listMedia = Media.getMediaByStatus(mStatus);

        }
        else
        {
            listMedia = Media.getMediaByProject(mProjectId);
        }

        mMediaAdapter = new MediaAdapter(getActivity(), R.layout.activity_media_list_row_short,listMedia, mRecyclerView );
        mRecyclerView.setAdapter(mMediaAdapter);

        return rootView;
    }


}