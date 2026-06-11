package net.kdt.pojavlaunch.modloaders;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.TextView;

public class OptiFineVersionListAdapter extends BaseExpandableListAdapter implements ExpandableListAdapter {

    private final OptiFineUtils.OptiFineVersions mOptiFineVersions;
    private final LayoutInflater mLayoutInflater;

    public OptiFineVersionListAdapter(OptiFineUtils.OptiFineVersions optiFineVersions, LayoutInflater mLayoutInflater) {
        mOptiFineVersions = optiFineVersions;
        this.mLayoutInflater = mLayoutInflater;
    }

    @Override
    public int getGroupCount() {
        return mOptiFineVersions.minecraftVersions.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return mOptiFineVersions.optifineVersions.get(i).size();
    }

    @Override
    public Object getGroup(int i) {
        return mOptiFineVersions.minecraftVersions.get(i);
    }

    @Override
    public Object getChild(int i, int i1) {
        return mOptiFineVersions.optifineVersions.get(i).get(i1);
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i1) {
        return i1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int i, boolean b, View convertView, ViewGroup viewGroup) {
        if(convertView == null)
            convertView = mLayoutInflater.inflate(net.kdt.pojavlaunch.R.layout.list_item_mod_group, viewGroup, false);

        ((TextView) convertView.findViewById(android.R.id.text1)).setText((String)getGroup(i));

        return convertView;
    }

    @Override
    public View getChildView(int i, int i1, boolean b, View convertView, ViewGroup viewGroup) {
        if(convertView == null)
            convertView = mLayoutInflater.inflate(net.kdt.pojavlaunch.R.layout.list_item_mod_version, viewGroup, false);
        
        ((TextView) convertView.findViewById(android.R.id.text1)).setText(((OptiFineUtils.OptiFineVersion)getChild(i,i1)).versionName);
        android.widget.ImageView icon = convertView.findViewById(net.kdt.pojavlaunch.R.id.mod_icon);
        if (icon != null) {
            icon.setImageResource(net.kdt.pojavlaunch.R.drawable.ic_optifine);
        }
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }
}
