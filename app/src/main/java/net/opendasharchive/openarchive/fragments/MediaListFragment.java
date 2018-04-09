package net.opendasharchive.openarchive.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import net.opendasharchive.openarchive.Globals;
import net.opendasharchive.openarchive.db.MediaAdapter;
import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.ReviewMediaActivity;
import net.opendasharchive.openarchive.db.Media;

import java.util.List;

import io.cleaninsights.sdk.piwik.CleanInsightsApplication;
import io.cleaninsights.sdk.piwik.MeasureHelper;
import io.cleaninsights.sdk.piwik.Measurer;

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

    /**
    ListView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent reviewMediaIntent = new Intent(getActivity(), ReviewMediaActivity.class);
            reviewMediaIntent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, getMediaIdByPosition(position));
            reviewMediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(reviewMediaIntent);
        }
    };

    ListView.OnItemLongClickListener onItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.alert_lbl_delete_media)
                    .setCancelable(true)
                    .setMessage(R.string.alert_delete_media)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Media.deleteMediaById(getMediaIdByPosition(position));
                            initMediaAdapter();
                            mMediaAdapter.notifyDataSetChanged();
                            Toast.makeText(getActivity(), R.string.alert_media_deleted, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {}
                    })
                    .setIcon(R.drawable.ic_dialog_alert_holo_light)
                    .show();

            return true;
        }
    };**/





}