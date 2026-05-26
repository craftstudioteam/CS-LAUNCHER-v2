package com.craftstudio.launcher.ui.dialog;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.craftstudio.launcher.R;
import com.craftstudio.launcher.databinding.DialogKeyboardBinding;
import com.craftstudio.launcher.ui.view.AnimButton;

import com.craftstudio.launcher.Tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class KeyboardDialog extends FullScreenDialog implements View.OnClickListener {
    private final DialogKeyboardBinding binding = DialogKeyboardBinding.inflate(getLayoutInflater());
    private final List<View> mSelectedViews = new ArrayList<>(1);
    private final List<Integer> mSelectedKeycodes = new ArrayList<>(1);
    private OnKeycodeSelectListener mOnKeycodeSelectListener;
    private OnMultiKeycodeSelectListener mOnMultiKeycodeSelectListener;
    private boolean showSpecialButtons = true;
    private boolean isGamepadMapper = false;

    public KeyboardDialog(@NonNull Context context) {
        super(context);
    }

    public KeyboardDialog(@NonNull Context context, boolean isGamepadMapper) {
        this(context);
        this.isGamepadMapper = isGamepadMapper;
    }

    public KeyboardDialog setShowSpecialButtons(boolean show) {
        showSpecialButtons = show;
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(binding.getRoot());

        Window window = getWindow();
        if (window != null) {
            int dimension = (int) Tools.dpToPx(getContext().getResources().getDimension(R.dimen._12sdp));

            WindowManager.LayoutParams params = window.getAttributes();
            params.width = Tools.currentDisplayMetrics.widthPixels - 2 * dimension;
            params.height = Tools.currentDisplayMetrics.heightPixels - 2 * dimension;
            window.setAttributes(params);

            window.setGravity(Gravity.CENTER);
        }

        init(showSpecialButtons);
    }

    private void init(boolean showSpecialButtons) {
        binding.close.setOnClickListener(this);
        binding.close1.setOnClickListener(this);
        binding.send.setOnClickListener(this);

        List<View> specialButtons = new ArrayList<>();

        if (isGamepadMapper) {
            specialButtons.add(getKey(getString(R.string.keycode_unspecified)));
            specialButtons.add(getKey(getString(R.string.keycode_mouse_right)));
            specialButtons.add(getKey(getString(R.string.keycode_mouse_middle)));
            specialButtons.add(getKey(getString(R.string.keycode_mouse_left)));
            specialButtons.add(getKey(getString(R.string.keycode_scroll_up)));
            specialButtons.add(getKey(getString(R.string.keycode_scroll_down)));
        } else {
            specialButtons.add(getKey(getString(R.string.keycode_special_keyboard)));
            specialButtons.add(getKey("GUI"));
            specialButtons.add(getKey(getString(R.string.keycode_special_pri)));
            specialButtons.add(getKey(getString(R.string.keycode_special_sec)));
            specialButtons.add(getKey(getString(R.string.keycode_special_mouse)));
            specialButtons.add(getKey(getString(R.string.keycode_special_mid)));
            specialButtons.add(getKey(getString(R.string.keycode_special_scrollup)));
            specialButtons.add(getKey(getString(R.string.keycode_special_scrolldown)));
            specialButtons.add(getKey(getString(R.string.keycode_special_menu)));
        }

        List<View> buttons = new ArrayList<>(List.of(
                binding.keyboardHome, binding.keyboardEsc,
                binding.keyboard0, binding.keyboard1, binding.keyboard2,
                binding.keyboard3, binding.keyboard4, binding.keyboard5,
                binding.keyboard6, binding.keyboard7, binding.keyboard8,
                binding.keyboard9, binding.keyboardUp, binding.keyboardDown,
                binding.keyboardLeft, binding.keyboardRight,
                binding.keyboardA, binding.keyboardB, binding.keyboardC,
                binding.keyboardD, binding.keyboardE, binding.keyboardF,
                binding.keyboardG, binding.keyboardH, binding.keyboardI,
                binding.keyboardJ, binding.keyboardK, binding.keyboardL,
                binding.keyboardM, binding.keyboardN, binding.keyboardO,
                binding.keyboardP, binding.keyboardQ, binding.keyboardR,
                binding.keyboardS, binding.keyboardT, binding.keyboardU,
                binding.keyboardV, binding.keyboardW, binding.keyboardX,
                binding.keyboardY, binding.keyboardZ, binding.keyboardComma,
                binding.keyboardPeriod, binding.keyboardLeftAlt,
                binding.keyboardRightAlt, binding.keyboardLeftShift,
                binding.keyboardRightShift, binding.keyboardTab,
                binding.keyboardSpace, binding.keyboardEnter,
                binding.keyboardBackspace, binding.keyboardGrave,
                binding.keyboardMinus, binding.keyboardEquals,
                binding.keyboardLeftBracket, binding.keyboardRightBracket,
                binding.keyboardBackslash, binding.keyboardSemicolon,
                binding.keyboardApostrophe, binding.keyboardSlash,
                binding.keyboardKpAdd, binding.keyboardPageUp,
                binding.keyboardPageDown, binding.keyboardLeftCtrl,
                binding.keyboardRightCtrl, binding.keyboardCapslock,
                binding.keyboardPause, binding.keyboardEnd,
                binding.keyboardInsert, binding.keyboardF1, binding.keyboardF2,
                binding.keyboardF3, binding.keyboardF4, binding.keyboardF5,
                binding.keyboardF6, binding.keyboardF7, binding.keyboardF8,
                binding.keyboardF9, binding.keyboardF10, binding.keyboardF11,
                binding.keyboardF12, binding.keyboardNumLock,
                binding.keyboardKp0, binding.keyboardKp1, binding.keyboardKp2,
                binding.keyboardKp3, binding.keyboardKp4, binding.keyboardKp5,
                binding.keyboardKp6, binding.keyboardKp7, binding.keyboardKp8,
                binding.keyboardKp9, binding.keyboardKpDivide,
                binding.keyboardKpMultiply, binding.keyboardKpSubract,
                binding.keyboardKpDecimal, binding.keyboardKpEnter));

        if (!isGamepadMapper) buttons.add(0, getKey(getString(R.string.keycode_unspecified)));

        if (showSpecialButtons) {
            //此处如果不是手柄映射模式，那么将反着加入
            int specialCount = isGamepadMapper ? 0 : specialButtons.size() - 1;
            for (View specialButton : specialButtons) {
                int finalSpecialCount = specialCount;
                specialButton.setTag(finalSpecialCount);
                specialButton.setOnClickListener(this);
                if (isGamepadMapper) specialCount += 1;
                else specialCount -= 1;
            }
        }

        int buttonCount = showSpecialButtons ? (specialButtons.size() - 1) : -1;
        if (isGamepadMapper) buttonCount++;

        for (View button : buttons) {
            buttonCount += 1;
            int finalButtonCount = buttonCount;
            button.setTag(finalButtonCount);
            button.setOnClickListener(this);

            if (    //保证顺序正确
                    Objects.equals(button, binding.keyboard9) ||
                    Objects.equals(button, binding.keyboardSlash) ||
                    Objects.equals(button, binding.keyboardPageDown) ||
                    Objects.equals(button, binding.keyboardPause) ||
                    Objects.equals(button, binding.keyboardKpSubract) ||
                    Objects.equals(button, binding.keyboardKpDecimal) ||
                    Objects.equals(button, binding.keyboardKpEnter)
            ) buttonCount += 1;
        }

        if (!showSpecialButtons) {
            binding.specialKey.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == binding.close || v == binding.close1) {
            closeDialog();
        } else if (v == binding.send) {
            if (!mSelectedKeycodes.isEmpty() && mOnMultiKeycodeSelectListener != null) {
                mOnMultiKeycodeSelectListener.onSelect(new ArrayList<>(mSelectedKeycodes));
            }
            closeDialog();
        } else if (v.getTag() != null) {
            int index = (int) v.getTag();
            onKeycodeSelect(v, index);
        }
    }

    private void closeDialog() {
        mSelectedViews.forEach(sv -> sv.setSelected(false));
        mSelectedViews.clear();
        mSelectedKeycodes.clear();
        this.dismiss();
    }

    private AnimButton getKey(String text) {
        AnimButton key = new AnimButton(getContext());
        key.setText(text);
        key.setTextSize(TypedValue.COMPLEX_UNIT_PX, Tools.dpToPx(9.6F));
        binding.specialKey.addView(key);
        return key;
    }

    private String getString(int resId) {
        return getContext().getString(resId);
    }

    private void onKeycodeSelect(View view, int index) {
        if (this.mOnKeycodeSelectListener != null) {
            this.mOnKeycodeSelectListener.onSelect(index);
            dismiss();
        } else if (this.mOnMultiKeycodeSelectListener != null) {
            if (mSelectedViews.contains(view)) {
                mSelectedViews.remove(view);
                mSelectedKeycodes.remove((Object) index);
                view.setSelected(false);
            } else {
                mSelectedViews.add(view);
                mSelectedKeycodes.add(index);
                view.setSelected(true);
            }
        }
    }

    public KeyboardDialog setOnKeycodeSelectListener(OnKeycodeSelectListener listener) {
        if (this.mOnMultiKeycodeSelectListener != null) {
            throw new IllegalStateException("Two listeners should not be initialized at the same time");
        }
        this.mOnKeycodeSelectListener = listener;
        binding.operateLayout.setVisibility(View.GONE);
        binding.close.setVisibility(View.VISIBLE);
        return this;
    }

    public KeyboardDialog setOnMultiKeycodeSelectListener(OnMultiKeycodeSelectListener listener) {
        if (this.mOnKeycodeSelectListener != null) {
            throw new IllegalStateException("Two listeners should not be initialized at the same time");
        }
        this.mOnMultiKeycodeSelectListener = listener;
        binding.operateLayout.setVisibility(View.VISIBLE);
        binding.close.setVisibility(View.GONE);
        return this;
    }

    public interface OnKeycodeSelectListener {
        void onSelect(int index);
    }

    public interface OnMultiKeycodeSelectListener {
        void onSelect(List<Integer> index);
    }
}
