package com.dere3046.tachibanasherry;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.core.content.ContextCompat;

/**
 * 自定义颜色选择对话框
 * 提供预设颜色选择和RGB自定义调色功能
 */
public class ColorPickerDialog extends Dialog {

    // 颜色选择回调接口
    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    private OnColorSelectedListener listener;
    private int currentColor = Color.WHITE;

    // 预设颜色数组
    private final int[] presetColors = {
            Color.WHITE, Color.BLACK, Color.RED, Color.GREEN, Color.BLUE,
            Color.YELLOW, Color.CYAN, Color.MAGENTA, 0xFFFFA500, 0xFF800080,
            0xFFA52A2A, 0xFF008080, 0xFF800000, 0xFF008000, 0xFF000080,
            0xFF808080, 0xFFC0C0C0, 0xFFFFD700, 0xFFDA70D6, 0xFFEEE8AA
    };

    // 预设颜色名称
    private final String[] colorNames = {
            "白色", "黑色", "红色", "绿色", "蓝色", "黄色", "青色", "紫色", "橙色", "深紫",
            "棕色", "蓝绿", "深红", "深绿", "深蓝", "灰色", "银色", "金色", "兰花紫", "淡黄"
    };

    public ColorPickerDialog(@NonNull Context context, OnColorSelectedListener listener, int currentColor) {
        super(context);
        this.listener = listener;
        this.currentColor = currentColor;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // 隐藏标题栏
        setContentView(R.layout.dialog_color_picker);

        // 初始化各个组件
        setupPresetColors();
        setupCustomColorPicker();
        setupCurrentColorPreview();
    }

    /**
     * 设置预设颜色网格
     */
    private void setupPresetColors() {
        GridLayout gridLayout = findViewById(R.id.grid_preset_colors);
        if (gridLayout == null) return;

        gridLayout.removeAllViews(); // 清空现有视图

        int padding = 8;
        int size = getContext().getResources().getDimensionPixelSize(R.dimen.color_item_size);

        // 为每个预设颜色创建视图
        for (int i = 0; i < presetColors.length; i++) {
            final int color = presetColors[i];

            // 创建颜色项容器
            LinearLayout colorItem = new LinearLayout(getContext());
            colorItem.setOrientation(LinearLayout.VERTICAL);
            colorItem.setPadding(padding, padding, padding, padding);

            // 颜色预览方块
            View colorView = new View(getContext());
            colorView.setBackgroundColor(color);
            colorView.setLayoutParams(new LinearLayout.LayoutParams(size, size));

            // 颜色名称标签
            TextView colorName = new TextView(getContext());
            colorName.setText(colorNames[i]);
            colorName.setTextSize(10);
            colorName.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
            colorName.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            // 组装视图
            colorItem.addView(colorView);
            colorItem.addView(colorName);

            // 设置点击事件
            colorItem.setOnClickListener(v -> {
                currentColor = color;
                updateCurrentColorPreview();
                if (listener != null) {
                    listener.onColorSelected(color);
                    dismiss(); // 选择后关闭对话框
                }
            });

            gridLayout.addView(colorItem);
        }
    }

    /**
     * 设置自定义颜色选择器（RGB滑块）
     */
    private void setupCustomColorPicker() {
        AppCompatSeekBar seekBarRed = findViewById(R.id.seekbar_red);
        AppCompatSeekBar seekBarGreen = findViewById(R.id.seekbar_green);
        AppCompatSeekBar seekBarBlue = findViewById(R.id.seekbar_blue);

        if (seekBarRed == null || seekBarGreen == null || seekBarBlue == null) return;

        // 设置滑块初始值为当前颜色分量
        seekBarRed.setProgress(Color.red(currentColor));
        seekBarGreen.setProgress(Color.green(currentColor));
        seekBarBlue.setProgress(Color.blue(currentColor));

        // 更新RGB值显示
        updateRGBText();

        // 滑块变化监听器
        SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // 根据RGB值合成新颜色
                    int red = seekBarRed.getProgress();
                    int green = seekBarGreen.getProgress();
                    int blue = seekBarBlue.getProgress();
                    currentColor = Color.rgb(red, green, blue);
                    updateCurrentColorPreview();
                    updateRGBText(); // 更新RGB数值显示
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        // 为三个滑块设置监听器
        seekBarRed.setOnSeekBarChangeListener(seekBarChangeListener);
        seekBarGreen.setOnSeekBarChangeListener(seekBarChangeListener);
        seekBarBlue.setOnSeekBarChangeListener(seekBarChangeListener);
    }

    /**
     * 更新RGB数值显示
     */
    private void updateRGBText() {
        TextView tvRed = findViewById(R.id.tv_red_value);
        TextView tvGreen = findViewById(R.id.tv_green_value);
        TextView tvBlue = findViewById(R.id.tv_blue_value);
        
        if (tvRed != null) tvRed.setText(String.valueOf(Color.red(currentColor)));
        if (tvGreen != null) tvGreen.setText(String.valueOf(Color.green(currentColor)));
        if (tvBlue != null) tvBlue.setText(String.valueOf(Color.blue(currentColor)));
    }

    /**
     * 设置当前颜色预览区域
     */
    private void setupCurrentColorPreview() {
        updateCurrentColorPreview();
        
        // 确认按钮点击事件
        View confirmButton = findViewById(R.id.btn_confirm);
        if (confirmButton != null) {
            confirmButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onColorSelected(currentColor);
                }
                dismiss(); // 关闭对话框
            });
        }
    }

    /**
     * 更新当前颜色预览
     */
    private void updateCurrentColorPreview() {
        // 更新颜色预览方块
        View colorPreview = findViewById(R.id.view_current_color);
        if (colorPreview != null) {
            colorPreview.setBackgroundColor(currentColor);
        }

        // 更新RGB文本显示
        TextView rgbText = findViewById(R.id.tv_rgb_value);
        if (rgbText != null) {
            int red = Color.red(currentColor);
            int green = Color.green(currentColor);
            int blue = Color.blue(currentColor);
            rgbText.setText(String.format("RGB: %d, %d, %d", red, green, blue));
        }
        
        // 更新RGB滑块数值显示
        updateRGBText();
    }
}