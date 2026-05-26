package com.craftstudio.launcher.contracts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

// Android's OpenDocument contract is the basicmost crap that doesn't allow
// you to specify practically anything. So i made this instead.
public class OpenDocumentWithExtension extends ActivityResultContract<Object, List<Uri>> {
    private final String mimeType;
    private final boolean allowMultiple;

    public OpenDocumentWithExtension(String extension) {
        this(extension, false);
    }

    /**
     * Create a new OpenDocumentWithExtension contract.
     * If the extension provided to the constructor is not available in the device's MIME
     * type database, the filter will default to "all types"
     * @param extension the extension to filter by
     * @param allowMultiple whether or not to allow multiple file selections
     */
    public OpenDocumentWithExtension(String extension, boolean allowMultiple) {
        String extensionMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if(extensionMimeType == null) extensionMimeType = "*/*";
        mimeType = extensionMimeType;
        this.allowMultiple = allowMultiple;
    }

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, @NonNull Object input) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);

        //根据构造函数参数决定是否允许多选
        if (allowMultiple) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }

        return intent;
    }

    @Nullable
    @Override
    public final SynchronousResult<List<Uri>> getSynchronousResult(@NonNull Context context,
                                                                   @NonNull Object input) {
        return null;
    }

    @Nullable
    @Override
    public final List<Uri> parseResult(int resultCode, @Nullable Intent intent) {
        if (intent == null || resultCode != Activity.RESULT_OK) return null;

        List<Uri> uris = new ArrayList<>();
        if (intent.getClipData() != null) {
            //多个项目被选中
            for (int i = 0; i < intent.getClipData().getItemCount(); i++) {
                uris.add(intent.getClipData().getItemAt(i).getUri());
            }
        } else {
            uris.add(intent.getData());
        }

        return uris;
    }
}
