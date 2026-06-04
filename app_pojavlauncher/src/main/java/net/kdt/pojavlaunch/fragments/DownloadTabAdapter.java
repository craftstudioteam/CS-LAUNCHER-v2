package net.kdt.pojavlaunch.fragments;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class DownloadTabAdapter extends FragmentStateAdapter {

    private final String[] mTypes;

    public DownloadTabAdapter(Fragment fragment, String[] types) {
        super(fragment);
        mTypes = types;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        String type = mTypes[position];
        if (type.equals("world")) {
            return new CurseForgeWorldsFragment();
        }
        return DownloadListFragment.newInstance(type);
    }

    @Override
    public int getItemCount() {
        return mTypes.length;
    }
}
