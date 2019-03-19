package net.opendasharchive.openarchive.db;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.opendasharchive.openarchive.ReviewMediaActivity;
import net.opendasharchive.openarchive.fragments.MediaListFragment;
import net.opendasharchive.openarchive.fragments.MediaViewHolder;
import net.opendasharchive.openarchive.util.Globals;
import net.opendasharchive.openarchive.util.SmartFragmentStatePagerAdapter;

import java.util.List;

/**
 * Created by micahjlucas on 1/20/15.
 */
public class ProjectAdapter extends SmartFragmentStatePagerAdapter {

    private List<Project> data;

    public ProjectAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int i) {
        MediaListFragment fragment = new MediaListFragment();
        fragment.setProjectId(getProject(i).getId());
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public Project getProject (int i)
    {
        return data.get(i);
    }

    public void updateData (List<Project> data)
    {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return data.get(position).description;
    }

}