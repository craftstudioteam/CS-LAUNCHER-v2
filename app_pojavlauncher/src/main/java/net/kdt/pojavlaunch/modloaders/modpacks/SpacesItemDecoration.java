package net.kdt.pojavlaunch.modloaders.modpacks;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** Adds vertical spacing between RecyclerView items. */
public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
    private final int mVerticalSpaceDp;

    public SpacesItemDecoration(int verticalSpaceDp) {
        this.mVerticalSpaceDp = verticalSpaceDp;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        if (pos == RecyclerView.NO_POSITION) return;
        // Apply spacing except after the last item
        if (pos < state.getItemCount() - 1) {
            outRect.bottom = mVerticalSpaceDp;
        }
    }
}
