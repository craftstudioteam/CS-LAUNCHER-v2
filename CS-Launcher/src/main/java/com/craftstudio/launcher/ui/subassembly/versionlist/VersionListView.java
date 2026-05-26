package com.craftstudio.launcher.ui.subassembly.versionlist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.craftstudio.launcher.R;
import com.craftstudio.launcher.event.sticky.MinecraftVersionValueEvent;
import com.craftstudio.launcher.feature.log.Logging;
import com.craftstudio.launcher.task.TaskExecutors;
import com.craftstudio.launcher.ui.subassembly.filelist.FileItemBean;
import com.craftstudio.launcher.ui.subassembly.filelist.FileRecyclerViewCreator;

import com.craftstudio.launcher.JMinecraftVersionList;
import com.craftstudio.launcher.Tools;
import com.craftstudio.launcher.utils.FilteredSubList;

import org.greenrobot.eventbus.EventBus;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kotlin.Pair;

public class VersionListView extends LinearLayout {
    private Context context;
    private List<JMinecraftVersionList.Version> releaseList, snapshotList, betaList, alphaList;
    private FileRecyclerViewCreator fileRecyclerViewCreator;
    private VersionSelectedListener versionSelectedListener;

    public VersionListView(Context context) {
        this(context, null);
    }

    public VersionListView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VersionListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private void init(Context context) {
        this.context = context;

        LayoutParams layParam = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        setOrientation(VERTICAL);

        RecyclerView mainListView = new RecyclerView(context);

        JMinecraftVersionList.Version[] versionArray;
        MinecraftVersionValueEvent event = EventBus.getDefault().getStickyEvent(MinecraftVersionValueEvent.class);

        if (event != null) {
            JMinecraftVersionList jMinecraftVersionList = event.getList();
            boolean isVersionsNotNull = jMinecraftVersionList != null && jMinecraftVersionList.versions != null;
            versionArray = isVersionsNotNull ? jMinecraftVersionList.versions : new JMinecraftVersionList.Version[0];
        } else {
            versionArray = new JMinecraftVersionList.Version[0];
        }

        releaseList = new FilteredSubList<>(versionArray, item -> item.type.equals("release"));
        snapshotList = new FilteredSubList<>(versionArray, item -> item.type.equals("snapshot"));
        betaList = new FilteredSubList<>(versionArray, item -> item.type.equals("old_beta"));
        alphaList = new FilteredSubList<>(versionArray, item -> item.type.equals("old_alpha"));

        fileRecyclerViewCreator = new FileRecyclerViewCreator(
                context,
                mainListView,
                (position, fileItemBean) -> versionSelectedListener.onVersionSelected(fileItemBean.name),
                null,
                showVersions(VersionType.RELEASE)
        );

        addView(mainListView, layParam);
    }

    private Pair<String, Date>[] getVersionPair(List<JMinecraftVersionList.Version> versions) {
        List<Pair<String, Date>> pairList = new ArrayList<>();
        for (int i = 0; i < versions.size(); i++) {
            JMinecraftVersionList.Version version = versions.get(i);
            Date date;
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(version.releaseTime, formatter);
                date = Date.from(zonedDateTime.toInstant());
            } catch (Exception e) {
                Logging.e("Version List", Tools.printToString(e));
                date = null;
            }
            pairList.add(new Pair<>(version.id, date));
        }
        return pairList.toArray(new Pair[0]);
    }

    public void setVersionSelectedListener(VersionSelectedListener versionSelectedListener) {
        this.versionSelectedListener = versionSelectedListener;
    }

    public void setVersionType(VersionType versionType) {
        showVersions(versionType);
    }

    public void setFilterString(String filterString) {
        this.fileRecyclerViewCreator.setFilterString(filterString);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private List<FileItemBean> showVersions(VersionType versionType) {
        switch (versionType) {
            case SNAPSHOT:
                return getVersion(context.getDrawable(R.drawable.ic_command_block), getVersionPair(snapshotList));
            case BETA:
                return getVersion(context.getDrawable(R.drawable.ic_old_cobblestone), getVersionPair(betaList));
            case ALPHA:
                return getVersion(context.getDrawable(R.drawable.ic_old_grass_block), getVersionPair(alphaList));
            case RELEASE:
            default:
                return getVersion(context.getDrawable(R.drawable.ic_minecraft), getVersionPair(releaseList));
        }
    }

    private List<FileItemBean> getVersion(Drawable icon, Pair<String, Date>[] namesPair) {
        List<FileItemBean> itemBeans = FileRecyclerViewCreator.loadItemBean(icon, namesPair);
        TaskExecutors.runInUIThread(() -> fileRecyclerViewCreator.loadData(itemBeans));
        return itemBeans;
    }
}
