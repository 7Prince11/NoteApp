package com.kelo.noteapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainTabsAdapter extends FragmentStateAdapter {

    private final PrimaryNotesFragment primaryFragment = PrimaryNotesFragment.newInstance();
    private final SecondaryNotesFragment secondaryFragment = SecondaryNotesFragment.newInstance();

    public MainTabsAdapter(@NonNull FragmentActivity fa) { super(fa); }

    @NonNull @Override
    public Fragment createFragment(int position) {
        return position == 0 ? primaryFragment : secondaryFragment;
    }

    @Override public int getItemCount() { return 2; }

    public PrimaryNotesFragment getPrimaryFragment() { return primaryFragment; }
    public SecondaryNotesFragment getSecondaryFragment() { return secondaryFragment; }
}
