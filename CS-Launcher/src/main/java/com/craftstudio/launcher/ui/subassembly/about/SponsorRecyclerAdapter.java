package com.craftstudio.launcher.ui.subassembly.about;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.craftstudio.launcher.R;
import com.craftstudio.launcher.databinding.ItemSponsorViewBinding;

public class SponsorRecyclerAdapter extends RecyclerView.Adapter<SponsorRecyclerAdapter.Holder> {
    private final SponsorMeta mMeta;

    public SponsorRecyclerAdapter(SponsorMeta meta) {
        this.mMeta = meta;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemSponsorViewBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(mMeta.sponsors[position]);
    }

    @Override
    public int getItemCount() {
        return mMeta.sponsors.length;
    }

    public static class Holder extends RecyclerView.ViewHolder {
        private final ItemSponsorViewBinding binding;

        public Holder(@NonNull ItemSponsorViewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        public void bind(SponsorMeta.Sponsor sponsor) {
            float amount = sponsor.getAmount();

            binding.nameView.setText(sponsor.getName());
            binding.timeView.setText(sponsor.getTime());
            binding.amountView.setText(String.format("￥%s", amount));

            Glide.with(binding.avatarView)
                    .load(sponsor.getAvatar())
                    .into(binding.avatarView);

            Drawable background = itemView.getBackground();
            if (amount >= 12.0f) {
                int color;
                if (amount >= 18.0f) {
                    color = ContextCompat.getColor(itemView.getContext(), R.color.background_sponsor_advanced);
                } else {
                    color = ContextCompat.getColor(itemView.getContext(), R.color.background_sponsor_intermediate);
                }
                background.setTint(color);
            } else {
                background.setTintList(null);
            }
        }
    }
}
