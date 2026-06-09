package net.kdt.pojavlaunch.prefs;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import net.kdt.pojavlaunch.FastClientHelper;
import net.kdt.pojavlaunch.LauncherActivity;

public class FastClientPreference extends Preference {

    public FastClientPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        if (getContext() instanceof LauncherActivity) {
            LauncherActivity activity = (LauncherActivity) getContext();
            FastClientHelper.setup(holder.itemView, activity, activity.getSupportFragmentManager());
        }
    }
}
