import os

def process_file(filepath, callback):
    with open(filepath, 'r') as f:
        content = f.read()
    new_content = callback(content)
    with open(filepath, 'w') as f:
        f.write(new_content)

def update_fabric_xml(content):
    content = content.replace('?attr/colorBgApp', '#000000')
    content = content.replace('?attr/colorBgBottomBar', '#1A1A1A')
    content = content.replace('?attr/colorTextPrimary', '#FFFFFF')
    content = content.replace('?attr/colorTextSecondary', '#AAAAAA')
    content = content.replace('?attr/colorAccent', '#FF8C00')
    return content

def update_mod_xml(content):
    content = content.replace('?attr/colorBgApp', '#000000')
    content = content.replace('?attr/colorBgBottomBar', '#1A1A1A')
    content = content.replace('?attr/colorTextPrimary', '#FFFFFF')
    content = content.replace('?attr/colorTextSecondary', '#AAAAAA')
    content = content.replace('?attr/colorAccent', '#FF8C00')
    return content

def update_list_item_fabric_xml(content):
    content = content.replace('?attr/colorTextPrimary', '#FFFFFF')
    content = content.replace('android:minHeight="72dp"', 'android:minHeight="56dp"')
    content = content.replace('android:layout_width="48dp"', 'android:layout_width="36dp"')
    content = content.replace('android:layout_height="48dp"', 'android:layout_height="36dp"')
    return content

def update_list_item_mod_xml(content):
    content = content.replace('?attr/colorTextPrimary', '#FFFFFF')
    content = content.replace('android:minHeight="72dp"', 'android:minHeight="56dp"')
    # OptiFine 28dp without container
    content = content.replace('''<FrameLayout
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:background="@drawable/bg_icon_container"
            android:padding="10dp">
            <ImageView
                android:id="@+id/mod_icon"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:scaleType="fitCenter" />
        </FrameLayout>''', '''<ImageView
            android:id="@+id/mod_icon"
            android:layout_width="28dp"
            android:layout_height="28dp"
            android:scaleType="fitCenter" />''')
    return content

process_file('app_pojavlauncher/src/main/res/layout/fragment_fabric_install.xml', update_fabric_xml)
process_file('app_pojavlauncher/src/main/res/layout/fragment_mod_version_list.xml', update_mod_xml)
process_file('app_pojavlauncher/src/main/res/layout/list_item_fabric_version.xml', update_list_item_fabric_xml)
process_file('app_pojavlauncher/src/main/res/layout/list_item_mod_version.xml', update_list_item_mod_xml)

print("XMLs updated")
