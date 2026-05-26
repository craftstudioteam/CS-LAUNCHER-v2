package com.craftstudio.launcher.authenticator.microsoft;

import android.content.Context;

import com.craftstudio.launcher.R;
import com.craftstudio.launcher.utils.stringutils.StringUtils;

public class PresentedException extends RuntimeException {
    final int localizationStringId;
    final boolean suspectedNoMinecraftPurchase;
    final Object[] extraArgs;

    public PresentedException(int localizationStringId, boolean suspectedNoMinecraftPurchase, Object... extraArgs) {
        this.localizationStringId = localizationStringId;
        this.suspectedNoMinecraftPurchase = suspectedNoMinecraftPurchase;
        this.extraArgs = extraArgs;
    }

    public PresentedException(Throwable throwable, int localizationStringId, boolean suspectedNoMinecraftPurchase, Object... extraArgs) {
        super(throwable);
        this.localizationStringId = localizationStringId;
        this.suspectedNoMinecraftPurchase = suspectedNoMinecraftPurchase;
        this.extraArgs = extraArgs;
    }

    public String toString(Context context) {
        String string = context.getString(localizationStringId, extraArgs);
        if (suspectedNoMinecraftPurchase) {
            string = StringUtils.insertNewline(string, context.getString(R.string.no_minecraft_purchase));
        }
        return string;
    }
}
