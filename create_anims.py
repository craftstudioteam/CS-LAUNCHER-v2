import os

anim_dir = "app_pojavlauncher/src/main/res/anim"
os.makedirs(anim_dir, exist_ok=True)

files = {
    "slide_in_up.xml": """<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android"
    android:interpolator="@android:anim/decelerate_interpolator">
    <translate android:fromYDelta="100%" android:toYDelta="0%" android:duration="300" />
    <alpha android:fromAlpha="0.0" android:toAlpha="1.0" android:duration="300" />
</set>""",
    "slide_out_down.xml": """<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android"
    android:interpolator="@android:anim/accelerate_interpolator">
    <translate android:fromYDelta="0%" android:toYDelta="100%" android:duration="300" />
    <alpha android:fromAlpha="1.0" android:toAlpha="0.0" android:duration="300" />
</set>""",
    "slide_in_right.xml": """<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android"
    android:interpolator="@android:anim/decelerate_interpolator">
    <translate android:fromXDelta="100%" android:toXDelta="0%" android:duration="250" />
    <alpha android:fromAlpha="0.0" android:toAlpha="1.0" android:duration="250" />
</set>""",
    "slide_out_left.xml": """<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android"
    android:interpolator="@android:anim/accelerate_interpolator">
    <translate android:fromXDelta="0%" android:toXDelta="-100%" android:duration="250" />
    <alpha android:fromAlpha="1.0" android:toAlpha="0.0" android:duration="250" />
</set>""",
    "slide_in_left.xml": """<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android"
    android:interpolator="@android:anim/decelerate_interpolator">
    <translate android:fromXDelta="-100%" android:toXDelta="0%" android:duration="250" />
    <alpha android:fromAlpha="0.0" android:toAlpha="1.0" android:duration="250" />
</set>""",
    "slide_out_right.xml": """<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android"
    android:interpolator="@android:anim/accelerate_interpolator">
    <translate android:fromXDelta="0%" android:toXDelta="100%" android:duration="250" />
    <alpha android:fromAlpha="1.0" android:toAlpha="0.0" android:duration="250" />
</set>""",
    "fade_in.xml": """<?xml version="1.0" encoding="utf-8"?>
<alpha xmlns:android="http://schemas.android.com/apk/res/android"
    android:interpolator="@android:anim/decelerate_interpolator"
    android:fromAlpha="0.0" android:toAlpha="1.0"
    android:duration="250" />""",
    "fade_out.xml": """<?xml version="1.0" encoding="utf-8"?>
<alpha xmlns:android="http://schemas.android.com/apk/res/android"
    android:interpolator="@android:anim/accelerate_interpolator"
    android:fromAlpha="1.0" android:toAlpha="0.0"
    android:duration="250" />""",
    "scale_pulse.xml": """<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <scale
        android:fromXScale="1.0" android:toXScale="1.05"
        android:fromYScale="1.0" android:toYScale="1.05"
        android:pivotX="50%" android:pivotY="50%"
        android:duration="800"
        android:repeatMode="reverse"
        android:repeatCount="infinite" />
</set>""",
    "item_stagger_fade.xml": """<?xml version="1.0" encoding="utf-8"?>
<layoutAnimation xmlns:android="http://schemas.android.com/apk/res/android"
    android:animation="@anim/slide_in_up"
    android:delay="15%"
    android:animationOrder="normal" />"""
}

for name, content in files.items():
    with open(os.path.join(anim_dir, name), "w") as f:
        f.write(content)

print("Done")
