package net.opendasharchive.openarchive.db;

import android.os.Bundle;

import net.opendasharchive.openarchive.fragments.MediaGridFragment;
import net.opendasharchive.openarchive.fragments.MediaListFragment;
import net.opendasharchive.openarchive.util.SmartFragmentStatePagerAdapter;

import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

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
        MediaListFragment fragment = new MediaGridFragment();
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