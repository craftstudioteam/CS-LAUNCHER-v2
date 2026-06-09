package net.kdt.pojavlaunch;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentManager;

import net.kdt.pojavlaunch.fragments.FastClientLoadingFragment;

public class FastClientHelper {

    private static final String PREF_NAME = "fastclient_prefs";
    private static final String KEY_ENABLED = "fc_enabled";
    private static final String KEY_VERSION  = "fc_version";

    // Settings fragment ke onViewCreated() mein call karo
    public static void setup(View rootView, Context ctx, FragmentManager fm) {

        SwitchCompat sw     = rootView.findViewById(R.id.switch_fastclient);
        if (sw == null) return; // Guard against wrong layout

        TextView tvVersion  = rootView.findViewById(R.id.tv_fastclient_version_info);
        SharedPreferences p = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Saved state restore
        boolean saved = p.getBoolean(KEY_ENABLED, false);
        sw.setOnCheckedChangeListener(null);
        sw.setChecked(saved);
        if (saved) {
            tvVersion.setVisibility(View.VISIBLE);
            tvVersion.setText("⚡ FastClient " + p.getString(KEY_VERSION, "v1.6.2") + " — Active");
        }

        sw.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                sw.setChecked(false); // dialog confirm hone tak false raho
                showVersionDialog(ctx, fm, sw, tvVersion, p);
            } else {
                p.edit().putBoolean(KEY_ENABLED, false).apply();
                tvVersion.setVisibility(View.GONE);
            }
        });
    }

    private static void showVersionDialog(Context ctx, FragmentManager fm,
            SwitchCompat sw, TextView tvVersion, SharedPreferences p) {

        Dialog d = new Dialog(ctx);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_fastclient_version);
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        d.setCancelable(false);

        RadioGroup rg        = d.findViewById(R.id.rg_version_selector);
        TextView btnCancel   = d.findViewById(R.id.btn_cancel_fastclient);
        TextView btnInstall  = d.findViewById(R.id.btn_install_fastclient);

        btnCancel.setOnClickListener(v -> d.dismiss());

        btnInstall.setOnClickListener(v -> {
            RadioButton rb = d.findViewById(rg.getCheckedRadioButtonId());
            String ver = versionFromId(rb.getId());
            d.dismiss();
            showLoading(fm, ver, sw, tvVersion, p);
        });

        d.show();
    }

    private static void showLoading(FragmentManager fm, String ver,
            SwitchCompat sw, TextView tvVersion, SharedPreferences p) {

        FastClientLoadingFragment frag = FastClientLoadingFragment.newInstance(ver);
        frag.setOnComplete(() -> {
            p.edit().putBoolean(KEY_ENABLED, true).putString(KEY_VERSION, ver).apply();
            sw.setChecked(true);
            tvVersion.setVisibility(View.VISIBLE);
            tvVersion.setText("⚡ FastClient " + ver + " — Active");
        });

        fm.beginTransaction()
          .replace(R.id.container_fragment, frag, "fc_loading")
          .addToBackStack(null)
          .commit();
    }

    private static String versionFromId(int id) {
        if (id == R.id.rb_version_1) return "v1.6.2";
        if (id == R.id.rb_version_2) return "v1.6.1";
        if (id == R.id.rb_version_3) return "v1.6.0";
        if (id == R.id.rb_version_4) return "v1.5.9";
        return "v1.6.2";
    }
}
