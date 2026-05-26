package com.craftstudio.launcher.ui.subassembly.account;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.craftstudio.launcher.R;
import com.craftstudio.launcher.databinding.ItemAccountManagerBinding;
import com.craftstudio.launcher.feature.accounts.AccountUtils;
import com.craftstudio.launcher.feature.log.Logging;
import com.craftstudio.launcher.utils.skin.SkinLoader;

import com.craftstudio.launcher.Tools;
import com.craftstudio.launcher.value.MinecraftAccount;

import java.util.List;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.Holder> {
    private final List<MinecraftAccount> mData;
    private AccountUpdateListener accountUpdateListener;

    public AccountAdapter(List<MinecraftAccount> mData) {
        this.mData = mData;
    }

    @NonNull
    @Override
    public AccountAdapter.Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemAccountManagerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AccountAdapter.Holder holder, int position) {
        holder.setData(mData.get(position));
    }

    @Override
    public int getItemCount() {
        if (mData != null) {
            return mData.size();
        }
        return 0;
    }

    public void setAccountUpdateListener(AccountUpdateListener accountUpdateListener) {
        this.accountUpdateListener = accountUpdateListener;
    }

    public interface AccountUpdateListener {
        void onViewClick(MinecraftAccount account);

        void onRefresh(MinecraftAccount account);

        void onDelete(MinecraftAccount account);
    }

    public class Holder extends RecyclerView.ViewHolder {
        private final Context mContext;
        private final ItemAccountManagerBinding binding;

        public Holder(@NonNull ItemAccountManagerBinding binding) {
            super(binding.getRoot());
            this.mContext = binding.getRoot().getContext();
            this.binding = binding;
        }

        public void setData(MinecraftAccount account) {
            if (accountUpdateListener != null) {
                itemView.setOnClickListener(v -> accountUpdateListener.onViewClick(account));
                binding.refresh.setOnClickListener(v -> accountUpdateListener.onRefresh(account));
                binding.delete.setOnClickListener(v -> accountUpdateListener.onDelete(account));
            }

            binding.name.setText(account.username);

            String loginType;
            if (AccountUtils.isMicrosoftAccount(account)) {
                setButtonClickable(binding.refresh, true);
                loginType = mContext.getString(R.string.account_microsoft_account);
            } else if (AccountUtils.isOtherLoginAccount(account)) {
                setButtonClickable(binding.refresh, true);
                loginType = account.accountType;
            } else {
                setButtonClickable(binding.refresh, false);
                loginType = mContext.getString(R.string.account_local_account);
            }

            try {
                binding.icon.setImageDrawable(SkinLoader.getAvatarDrawable(mContext, account, (int) Tools.dpToPx(mContext.getResources().getDimensionPixelSize(R.dimen._38sdp))));
            } catch (Exception e) {
                Logging.e("AccountAdapter", "Failed to load avatar.", e);
            }

            binding.loginType.setText(loginType);
        }

        private void setButtonClickable(View button, boolean clickable) {
            button.setAlpha(clickable ? 1.0f : 0.5f);
            button.setEnabled(clickable);
        }
    }
}
