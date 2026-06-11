import re

def process_file(filepath, callback):
    with open(filepath, 'r') as f:
        content = f.read()
    new_content = callback(content)
    with open(filepath, 'w') as f:
        f.write(new_content)

def update_fabric_xml(content):
    # Header background = settings background
    content = content.replace('android:background="?attr/colorBgBottomBar"', 'android:background="#1A1A1A"')
    # Background color = settings background
    content = content.replace('android:background="?attr/colorBgApp"', 'android:background="#000000"')
    
    # Progress bar accent
    content = content.replace('android:visibility="gone" />', 'android:visibility="gone" android:indeterminateTint="#FF8C00" />')
    
    # Checkbox tint
    content = content.replace('app:buttonTint="?attr/colorAccent"', 'app:buttonTint="#FF8C00"')
    return content

def update_mod_xml(content):
    content = content.replace('android:background="?attr/colorBgBottomBar"', 'android:background="#1A1A1A"')
    content = content.replace('android:background="?attr/colorBgApp"', 'android:background="#000000"')
    content = content.replace('android:indeterminate="true"', 'android:indeterminate="true" android:indeterminateTint="#FF8C00"')
    return content

def update_list_item_xml(content):
    # Padding, Text colors
    content = content.replace('android:textColor="?attr/colorTextPrimary"', 'android:textColor="#FFFFFF"')
    # Card style
    # Min touch target height 56dp
    content = content.replace('android:minHeight="72dp"', 'android:minHeight="56dp"')
    content = content.replace('android:layout_width="48dp"', 'android:layout_width="28dp"')
    content = content.replace('android:layout_height="48dp"', 'android:layout_height="28dp"')
    content = content.replace('android:padding="10dp"', 'android:padding="0dp"')
    
    return content

process_file('app_pojavlauncher/src/main/res/layout/fragment_fabric_install.xml', update_fabric_xml)
process_file('app_pojavlauncher/src/main/res/layout/fragment_mod_version_list.xml', update_mod_xml)
process_file('app_pojavlauncher/src/main/res/layout/list_item_fabric_version.xml', update_list_item_xml)
process_file('app_pojavlauncher/src/main/res/layout/list_item_mod_version.xml', update_list_item_xml)

print("XMLs updated")
