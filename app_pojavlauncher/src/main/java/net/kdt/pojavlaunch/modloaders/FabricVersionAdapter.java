package net.kdt.pojavlaunch.modloaders;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.kdt.pojavlaunch.R;

import java.util.List;

public class FabricVersionAdapter extends ArrayAdapter<FabricVersion> {
    private final int mResource;
    private final boolean mShowBadges;
    private int mSelectedPosition = -1;

    public FabricVersionAdapter(Context context, int resource, List<FabricVersion> objects, boolean showBadges) {
        super(context, resource, objects);
        this.mResource = resource;
        this.mShowBadges = showBadges;
    }

    public void setSelectedPosition(int position) {
        mSelectedPosition = position;
        notifyDataSetChanged();
    }

    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(mResource, parent, false);
        }

        FabricVersion item = getItem(position);
        if (item == null) return view;

        TextView text1 = view.findViewById(android.R.id.text1);
        if (text1 != null) {
            text1.setText(item.version);
        }

        if (mShowBadges) {
            TextView badge = view.findViewById(R.id.fabric_stable_badge);
            if (badge != null) {
                badge.setVisibility(item.stable ? View.VISIBLE : View.GONE);
            }
            TextView subtitle = view.findViewById(R.id.fabric_version_subtitle);
            if (subtitle != null) {
                subtitle.setText(item.stable ? "Recommended" : "Preview");
                subtitle.setTextColor(item.stable ? 0xFF8BFF8B : 0xFF9CA3AF);
            }
        }

        view.setSelected(position == mSelectedPosition);

        return view;
    }
}
