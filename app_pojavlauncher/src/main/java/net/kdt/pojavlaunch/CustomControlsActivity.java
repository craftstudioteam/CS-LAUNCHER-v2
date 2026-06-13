package net.kdt.pojavlaunch;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.drawerlayout.widget.DrawerLayout;

import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData;
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.EditorExitable;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.io.IOException;


public class CustomControlsActivity extends BaseActivity implements EditorExitable {
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerNavigationView;
	private ControlLayout mControlLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_custom_controls);

		mControlLayout = findViewById(R.id.customctrl_controllayout);
		mDrawerLayout = findViewById(R.id.customctrl_drawerlayout);
		mDrawerNavigationView = findViewById(R.id.customctrl_navigation_view);
		View mPullDrawerButton = findViewById(R.id.drawer_button);

		mPullDrawerButton.setOnClickListener(v -> mDrawerLayout.openDrawer(mDrawerNavigationView));
		mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

		String[] menuItems = getResources().getStringArray(R.array.menu_customcontrol_customactivity);
		int[] menuIcons = new int[]{
				android.R.drawable.ic_menu_add, // Add Button
				android.R.drawable.ic_menu_gallery, // Add Drawer
				android.R.drawable.ic_menu_compass, // Add Joystick
				android.R.drawable.ic_menu_upload, // Load
				android.R.drawable.ic_menu_save, // Save
				android.R.drawable.ic_menu_myplaces, // Default
				android.R.drawable.ic_menu_share // Export
		};
		mDrawerNavigationView.setAdapter(new ArrayAdapter<String>(this, R.layout.item_custom_control_menu, R.id.menu_item_text, menuItems) {
			@androidx.annotation.NonNull
			@Override
			public View getView(int position, @androidx.annotation.Nullable View convertView, @androidx.annotation.NonNull ViewGroup parent) {
				View view = super.getView(position, convertView, parent);
				ImageView icon = view.findViewById(R.id.menu_item_icon);
				if (icon != null && position < menuIcons.length) {
					icon.setImageResource(menuIcons[position]);
				}
				return view;
			}
		});
		mDrawerNavigationView.setOnItemClickListener((parent, view, position, id) -> {
			android.util.Log.i("CustomControlsActivity", "Menu item clicked: position=" + position);
			switch(position) {
				case 0: android.util.Log.i("CustomControlsActivity", "Action: Add Button"); mControlLayout.addControlButton(new ControlData("New")); break;
				case 1: android.util.Log.i("CustomControlsActivity", "Action: Add Button Drawer"); mControlLayout.addDrawer(new ControlDrawerData()); break;
				case 2: android.util.Log.i("CustomControlsActivity", "Action: Add Joystick"); mControlLayout.addJoystickButton(new ControlJoystickData()); break;
				case 3: android.util.Log.i("CustomControlsActivity", "Action: Load"); mControlLayout.openLoadDialog(); break;
				case 4: android.util.Log.i("CustomControlsActivity", "Action: Save"); mControlLayout.openSaveDialog(this); break;
				case 5: android.util.Log.i("CustomControlsActivity", "Action: Select Default"); mControlLayout.openSetDefaultDialog(); break;
				case 6: // Saving the currently shown control
					android.util.Log.i("CustomControlsActivity", "Action: Share layout");
					try {
						Uri contentUri = DocumentsContract.buildDocumentUri(getString(R.string.storageProviderAuthorities), mControlLayout.saveToDirectory(mControlLayout.mLayoutFileName));

						Intent shareIntent = new Intent();
						shareIntent.setAction(Intent.ACTION_SEND);
						shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
						shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						shareIntent.setType("application/json");
						startActivity(shareIntent);

						Intent sendIntent = Intent.createChooser(shareIntent, mControlLayout.mLayoutFileName);
						startActivity(sendIntent);
					}catch (Exception e) {
						Tools.showError(this, e);
					}
					break;
			}
			mDrawerLayout.closeDrawers();
		});
		mControlLayout.setModifiable(true);
		try {
			mControlLayout.loadLayout(LauncherPreferences.PREF_DEFAULTCTRL_PATH);
		}catch (IOException e) {
			Tools.showError(this, e);
		}
	}

	@Override
	public void onBackPressed() {
		mControlLayout.askToExit(this);
	}

	@Override
	public void exitEditor() {
		super.onBackPressed();
	}
}
