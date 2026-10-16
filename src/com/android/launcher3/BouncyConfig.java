package com.android.launcher3;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class BouncyConfig {
    private static final String PREF_NAME = "bouncy_settings";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_MUSIC = "music_enabled";
    private static final String KEY_BOUNCY = "bouncy_enabled";

    // 默认时长 460ms (约 130 BPM)
    public static long sDuration = 460L;
    public static boolean sMusicEnabled = true;
    public static boolean sBouncyEnabled = true;

    private static MediaPlayer sMediaPlayer;

    private static final String KEY_VIDEO = "video_enabled";
    public static boolean sVideoEnabled = true; // 默认开启视频
    
    private static final List<Runnable> sListeners = new ArrayList<>();

    // 初始化读取配置
    public static void init(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sDuration = sp.getLong(KEY_DURATION, 460L);
        sMusicEnabled = sp.getBoolean(KEY_MUSIC, true);
        sBouncyEnabled = sp.getBoolean(KEY_BOUNCY, true);
        sVideoEnabled = sp.getBoolean(KEY_VIDEO, true); // 【加这行】
    }

    public static void addListener(Runnable listener) {
        if (!sListeners.contains(listener)) {
            sListeners.add(listener);
        }
    }

    private static void notifyListeners() {
        for (Runnable r : sListeners) {
            r.run();
        }
    }

    // ================= 音乐播放管理 =================
    public static void onResume(Context context) {
        if (!sMusicEnabled) return;
        try {
            if (sMediaPlayer == null) {
                // 读取刚才放进去的 pengyou_de_jiu.mp3
                int resId = context.getResources().getIdentifier("pengyoudejiu", "raw", context.getPackageName());
                if (resId != 0) {
                    sMediaPlayer = MediaPlayer.create(context.getApplicationContext(), resId);
                    sMediaPlayer.setLooping(true); // 单曲循环！
                }
            }
            if (sMediaPlayer != null && !sMediaPlayer.isPlaying()) {
                sMediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onPause() {
        if (sMediaPlayer != null && sMediaPlayer.isPlaying()) {
            sMediaPlayer.pause();
        }
    }

    // ================= 弹窗设置面板（纯代码生成，零报错风险） =================
    public static void showSettingsDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("⚡ 所有人听令：保持Q弹控制台");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 20);

        // 1. Q弹开关
        CheckBox cbBouncy = new CheckBox(context);
        cbBouncy.setText("开启全员保持Q弹");
        cbBouncy.setChecked(sBouncyEnabled);
        layout.addView(cbBouncy);

        // 2. 音乐开关
        CheckBox cbMusic = new CheckBox(context);
        cbMusic.setText("自动播放《朋友的酒》");
        cbMusic.setChecked(sMusicEnabled);
        layout.addView(cbMusic);

        // 3. 频率/卡点调节滑块 (范围：300ms ~ 650ms)
        final TextView tvSpeed = new TextView(context);
        tvSpeed.setPadding(0, 30, 0, 10);
        float currentBpm = 60000f / sDuration;
        tvSpeed.setText(String.format("节拍周期: %d ms (约 %.1f BPM)", sDuration, currentBpm));
        layout.addView(tvSpeed);

        SeekBar sbSpeed = new SeekBar(context);
        sbSpeed.setMax(350); // 300 + 350 = 650ms
        sbSpeed.setProgress((int) (sDuration - 300));
        sbSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sDuration = 300 + progress;
                float bpm = 60000f / sDuration;
                tvSpeed.setText(String.format("节拍周期: %d ms (约 %.1f BPM)", sDuration, bpm));
                // 实时生效！拖动时桌面图标瞬间随之变频！
                notifyListeners();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        layout.addView(sbSpeed);

        builder.setView(layout);
        builder.setPositiveButton("保存并生效", (dialog, which) -> {
            sBouncyEnabled = cbBouncy.isChecked();
            sMusicEnabled = cbMusic.isChecked();

            // 保存到磁盘
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            sp.edit().putLong(KEY_DURATION, sDuration)
                .putBoolean(KEY_MUSIC, sMusicEnabled)
                .putBoolean(KEY_BOUNCY, sBouncyEnabled)
                .apply();

            if (sMusicEnabled) {
                onResume(context);
            } else {
                onPause();
            }
            notifyListeners();
        });
        // 3. 背景视频开关
        CheckBox cbVideo = new CheckBox(context);
        cbVideo.setText("开启背景循环视频");
        cbVideo.setChecked(sVideoEnabled);
        layout.addView(cbVideo);

        builder.setPositiveButton("保存并生效", (dialog, which) -> {
            sBouncyEnabled = cbBouncy.isChecked();
            sMusicEnabled = cbMusic.isChecked();
            sVideoEnabled = cbVideo.isChecked(); // 【加这行】

            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            sp.edit().putLong(KEY_DURATION, sDuration)
                .putBoolean(KEY_MUSIC, sMusicEnabled)
                .putBoolean(KEY_BOUNCY, sBouncyEnabled)
                .putBoolean(KEY_VIDEO, sVideoEnabled) // 【加这行】
                .apply();

            if (sMusicEnabled) {
                onResume(context);
            } else {
                onPause();
            }
            notifyListeners(); // 通知刷新
        });
        
        builder.setNegativeButton("取消", null);
        builder.show();
    }
}
