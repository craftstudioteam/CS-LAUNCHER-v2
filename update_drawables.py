import os

res_dir = "app_pojavlauncher/src/main/res/drawable"

files = {
    "bg_version_list_item.xml": """<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <shape>
            <solid android:color="#2A2A2A"/>
            <corners android:radius="12dp"/>
            <stroke android:width="1dp" android:color="#FF8C00"/>
        </shape>
    </item>
    <item>
        <shape>
            <solid android:color="#1A1A1A"/>
            <corners android:radius="12dp"/>
            <stroke android:width="1dp" android:color="#333333"/>
        </shape>
    </item>
</selector>""",
    "bg_install_button_primary.xml": """<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <shape>
            <solid android:color="#CC7000"/>
            <corners android:radius="12dp"/>
        </shape>
    </item>
    <item>
        <shape>
            <solid android:color="#FF8C00"/>
            <corners android:radius="12dp"/>
        </shape>
    </item>
</selector>""",
    "bg_selected_version_badge.xml": """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#FF8C00"/>
    <corners android:radius="16dp"/>
</shape>""",
    "bg_icon_container.xml": """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@android:color/transparent"/>
</shape>"""
}

for name, content in files.items():
    with open(os.path.join(res_dir, name), "w") as f:
        f.write(content)

print("Drawables updated")
