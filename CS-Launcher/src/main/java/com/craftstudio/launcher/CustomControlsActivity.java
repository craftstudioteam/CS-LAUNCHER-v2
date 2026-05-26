package com.craftstudio.launcher;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.drawerlayout.widget.DrawerLayout;

import com.craftstudio.launcher.databinding.ActivityCustomControlsBinding;
import com.craftstudio.launcher.databinding.ViewControlMenuBinding;
import com.craftstudio.launcher.feature.background.BackgroundManager;
import com.craftstudio.launcher.feature.background.BackgroundType;
import com.craftstudio.launcher.setting.AllSettings;
import com.craftstudio.launcher.ui.activity.BaseActivity;
import com.craftstudio.launcher.ui.subassembly.menu.ControlMenu;
import com.craftstudio.launcher.ui.subassembly.view.GameMenuViewWrapper;

import com.craftstudio.launcher.customcontrols.ControlLayout;
import com.craftstudio.launcher.customcontrols.EditorExitable;

import java.io.IOException;

public class CustomControlsActivity extends BaseActivity implements EditorExitable {
	public static final String BUNDLE_CONTROL_PATH = "control_path";
	private ActivityCustomControlsBinding binding;
	private String mControlPath = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		parseBundle();
		binding = ActivityCustomControlsBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		ControlLayout controlLayout = binding.customctrlControllayout;
		DrawerLayout drawerLayout = binding.customctrlDrawerlayout;
		FrameLayout drawerNavigationView = binding.customctrlNavigationView;

		new GameMenuViewWrapper(this, v -> {
			boolean open = drawerLayout.isDrawerOpen(drawerNavigationView);

			if (open) drawerLayout.closeDrawer(drawerNavigationView);
			else drawerLayout.openDrawer(drawerNavigationView);
		}, false).setVisibility(true);

		BackgroundManager.setBackgroundImage(this, BackgroundType.CUSTOM_CONTROLS, binding.backgroundView, null);

		drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
		drawerLayout.setScrimColor(Color.TRANSPARENT);

		ViewControlMenuBinding controlMenuBinding = ViewControlMenuBinding.inflate(getLayoutInflater());
		new ControlMenu(this, this, controlMenuBinding, controlLayout, true);

		drawerNavigationView.addView(controlMenuBinding.getRoot());
		controlLayout.setModifiable(true);
		try {
			if (mControlPath == null) controlLayout.loadLayout((String) null);
			else controlLayout.loadLayout(mControlPath);
		} catch (IOException e) {
			Tools.showError(this, e);
		}

		setupDrawerAnimation(drawerLayout, drawerNavigationView);
		setupEntryAnimations();

		getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				binding.customctrlControllayout.askToExit(CustomControlsActivity.this);
			}
		});
	}

	private void parseBundle() {
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			mControlPath = bundle.getString(BUNDLE_CONTROL_PATH);
		}
	}

	private void setupDrawerAnimation(DrawerLayout drawerLayout, FrameLayout navPanel) {
		drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
			@Override
			public void onDrawerOpened(View drawerView) {
				drawerView.setAlpha(0f);
				drawerView.setTranslationX(80f);
				drawerView.setScaleX(0.94f);
				drawerView.setScaleY(0.94f);
				drawerView.animate()
					.alpha(1f)
					.translationX(0f)
					.scaleX(1f)
					.scaleY(1f)
					.setDuration(300)
					.setInterpolator(new OvershootInterpolator(1.2f))
					.start();
			}

			@Override
			public void onDrawerClosed(View drawerView) {
				drawerView.animate()
					.alpha(0f)
					.translationX(40f)
					.scaleX(0.96f)
					.scaleY(0.96f)
					.setDuration(180)
					.setInterpolator(new AccelerateInterpolator())
					.start();
			}
		});
	}

	private void setupEntryAnimations() {
		binding.editorBadge.setAlpha(0f);
		binding.editorBadge.setTranslationY(-20f);
		binding.editorBadge.animate()
			.alpha(1f)
			.translationY(0f)
			.setDuration(400)
			.setStartDelay(100)
			.setInterpolator(new OvershootInterpolator(1.5f))
			.start();

		binding.hintContainer.setAlpha(0f);
		binding.hintContainer.setTranslationX(30f);
		binding.hintContainer.animate()
			.alpha(1f)
			.translationX(0f)
			.setDuration(400)
			.setStartDelay(180)
			.setInterpolator(new OvershootInterpolator(1.5f))
			.start();
	}

	@Override
	public boolean shouldIgnoreNotch() {
		return AllSettings.getIgnoreNotch().getValue();
	}

	@Override
	public void exitEditor() {
		finish();
	}
}
