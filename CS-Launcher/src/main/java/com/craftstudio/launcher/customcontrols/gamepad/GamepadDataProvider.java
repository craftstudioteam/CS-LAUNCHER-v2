package com.craftstudio.launcher.customcontrols.gamepad;

import com.craftstudio.launcher.GrabListener;

public interface GamepadDataProvider {
    GamepadMap getMenuMap();
    GamepadMap getGameMap();
    boolean isGrabbing();
    void attachGrabListener(GrabListener grabListener);
}
