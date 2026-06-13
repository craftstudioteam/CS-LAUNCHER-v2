package net.kdt.pojavlaunch.prefs;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.AttributeSet;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;

public class BackButtonPreference extends Preference {
    public BackButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressWarnings("unused") public BackButtonPreference(Context context) {
        this(context, null);
    }

    private void init(){
        if(getTitle() == null){
            setTitle(R.string.preference_back_title);
        }
        if(getIcon() == null){
            setIcon(R.drawable.ic_arrow_back_white);
        }
    }


    @Override
    public void onBindViewHolder(androidx.preference.PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        android.view.View backBtn = holder.findViewById(R.id.back_button);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> onClick());
        }
    }

    @Override
    protected void onClick() {
        FragmentActivity activity = findFragmentActivity(getContext());
        if (activity != null) {
            activity.getOnBackPressedDispatcher().onBackPressed();
            return;
        }
        ExtraCore.setValue(ExtraConstants.BACK_PREFERENCE, "true");
    }

    private FragmentActivity findFragmentActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof FragmentActivity) return (FragmentActivity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
