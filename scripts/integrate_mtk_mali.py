#!/usr/bin/env python3
"""Apply the MTK integration overlay to the pinned Winlator Mali submodule."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "upstream" / "winlator" / "app"
JAVA = APP / "src/main/java/com/winlator/cmod"
MAIN = JAVA / "MainActivity.java"
MENU = APP / "src/main/res/menu/main_menu.xml"
MANIFEST = APP / "src/main/AndroidManifest.xml"
GRADLE = APP / "build.gradle"

def replace_once(path: Path, old: str, new: str) -> None:
    data = path.read_text(encoding="utf-8")
    count = data.count(old)
    if count != 1:
        raise SystemExit(f"Expected one integration anchor in {path}, found {count}")
    path.write_text(data.replace(old, new, 1), encoding="utf-8")

replace_once(MAIN,
    "        R.id.main_menu_shortcuts, R.id.main_menu_containers, R.id.main_menu_input_controls,\n",
    "        R.id.main_menu_shortcuts, R.id.main_menu_containers, R.id.main_menu_input_controls,\n        R.id.main_menu_mtk_device,\n")
replace_once(MAIN,
    "            case R.id.main_menu_settings:\n                show(new SettingsFragment(), false);  // Forward animation\n                break;\n",
    "            case R.id.main_menu_settings:\n                show(new SettingsFragment(), false);  // Forward animation\n                break;\n            case R.id.main_menu_mtk_device:\n                startActivity(new Intent(this, MTKDeviceActivity.class));\n                drawerLayout.closeDrawer(GravityCompat.START);\n                break;\n")
replace_once(MENU,
    '                <item\n                    android:id="@+id/main_menu_settings"\n',
    '                <item\n                    android:id="@+id/main_menu_mtk_device"\n                    android:icon="@drawable/icon_settings"\n                    android:title="MTK Device &amp; Backend" />\n                <item\n                    android:id="@+id/main_menu_settings"\n')
replace_once(MANIFEST,
    '        <activity\n            android:name="com.winlator.cmod.MainActivity"\n',
    '        <activity android:name="com.winlator.cmod.MTKDeviceActivity" android:exported="false" />\n\n        <activity\n            android:name="com.winlator.cmod.MainActivity"\n')

activity = r'''package com.winlator.cmod;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

public final class MTKDeviceActivity extends Activity {
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void addText(LinearLayout root, String value, float size, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value); text.setTextSize(size); text.setTextColor(0xFFE8EDF2);
        text.setPadding(0, dp(7), 0, dp(7));
        if (bold) text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(text);
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF101820);
        getWindow().setNavigationBarColor(0xFF101820);
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(0xFF101820);
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(24), dp(20), dp(24)); scroll.addView(content);
        addText(content, "MTK Winlator • Device & Backend", 22, true);
        addText(content, "Diagnostik perangkat dan backend grafis.", 14, false);
        ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (manager != null) manager.getMemoryInfo(memory);
        boolean adreno = com.winlator.cmod.core.GPUInformation.isAdrenoGPU(this);
        String hardware = (Build.HARDWARE + " " + Build.BOARD + " " + Build.SOC_MODEL).toLowerCase(Locale.ROOT);
        boolean maliHint = hardware.contains("mali") || hardware.contains("mediatek") || hardware.contains("mtk");
        String gpuFamily = adreno ? "Adreno terdeteksi" : (maliHint ? "MediaTek/Mali terindikasi; verifikasi renderer Vulkan" : "GPU belum dapat dipastikan otomatis");
        addText(content, "INFORMASI PERANGKAT", 16, true);
        addText(content, "Model: " + Build.MANUFACTURER + " " + Build.MODEL, 14, false);
        addText(content, "Hardware: " + Build.HARDWARE + " / " + Build.BOARD, 14, false);
        addText(content, "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")", 14, false);
        addText(content, "ABI: " + String.join(", ", Build.SUPPORTED_ABIS), 14, false);
        addText(content, "RAM total: " + String.format(Locale.US, "%.1f GB", memory.totalMem / 1073741824.0), 14, false);
        addText(content, "Keluarga GPU: " + gpuFamily, 14, false);
        addText(content, "BACKEND GRAFIS", 16, true);
        boolean vulkan = getPackageManager().hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL);
        addText(content, "Vulkan feature: " + (vulkan ? "dilaporkan tersedia Android" : "tidak dilaporkan perangkat"), 14, false);
        addText(content, adreno ? "Saran awal: Turnip untuk Adreno." : "Saran Mali: Graphics Driver = Wrapper; BCn Emulation = ASTC atau ETC2. DXVK memerlukan Vulkan yang kompatibel.", 14, false);
        addText(content, "Catatan: diagnostik ini bukan benchmark dan tidak menjamin semua game kompatibel.", 13, false);
        addText(content, "RUNTIME TERPADU", 16, true);
        addText(content, "Daftar game, container Wine, Box64/FEX, pengaturan driver, dan peluncuran game menggunakan sistem Winlator Mali yang sama. Halaman ini menambahkan diagnostik MTK ke aplikasi tersebut.", 14, false);
        setContentView(scroll);
    }
}
'''
p=JAVA / "MTKDeviceActivity.java"
if p.exists(): raise SystemExit(f"Refusing to overwrite {p}")
p.write_text(activity, encoding="utf-8")
replace_once(GRADLE,
    '            applicationId "com.winlator.mali"\n            resValue "string", "app_name", "Winlator Mali"\n',
    '            applicationId "com.mtk.winlator"\n            resValue "string", "app_name", "MTK Winlator Mali"\n')
print("Applied MTK device/backend integration to Winlator Mali.")
