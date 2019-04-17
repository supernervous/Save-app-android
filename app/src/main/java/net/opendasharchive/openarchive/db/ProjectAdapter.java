package net.opendasharchive.openarchive.db;

import android.content.Context;
import android.os.Bundle;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.fragments.MediaGridFragment;
import net.opendasharchive.openarchive.fragments.MediaListFragment;
import net.opendasharchive.openarchive.fragments.NewProjectFragment;
import net.opendasharchive.openarchive.util.SmartFragmentStatePagerAdapter;

import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

/**
 * Created by micahjlucas on 1/20/15.
 */
public class ProjectAdapter extends SmartFragmentStatePagerAdapter {

    private List<Project> data;
    private Context context;

    public ProjectAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.context = context;

    }

    @Override
    public Fragment getItem(int i) {

        if (i == 0)
        {
            NewProjectFragment frag = new NewProjectFragment();
            return frag;
        }
        else {
            MediaListFragment fragment = new MediaGridFragment();
            fragment.setProjectId(getProject(i).getId());
            Bundle args = new Bundle();
            fragment.setArguments(args);
            return fragment;
        }
    }

    public Project getProject (int i)
    {
        return data.get(i-1);
    }

    public void updateData (List<Project> data)
    {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return data.size()+1;
    }

    @Override
    public CharSequence getPageTitle(int position) {

        if (position == 0)
        {
            return context.getString(R.string.action_plus);
        }
        else
            return data.get(position-1).description;
    }

}