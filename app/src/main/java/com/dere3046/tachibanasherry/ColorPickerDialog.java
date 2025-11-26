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

public class ColorPickerDialog extends Dialog {

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    private OnColorSelectedListener listener;
    private int currentColor = Color.WHITE;

    // 预定义颜色
    private final int[] presetColors = {
            Color.WHITE, Color.BLACK, Color.RED, Color.GREEN, Color.BLUE,
            Color.YELLOW, Color.CYAN, Color.MAGENTA, 0xFFFFA500, 0xFF800080,
            0xFFA52A2A, 0xFF008080, 0xFF800000, 0xFF008000, 0xFF000080,
            0xFF808080, 0xFFC0C0C0, 0xFFFFD700, 0xFFDA70D6, 0xFFEEE8AA
    };

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
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_color_picker);

        setupPresetColors();
        setupCustomColorPicker();
        setupCurrentColorPreview();
    }

    private void setupPresetColors() {
        GridLayout gridLayout = findViewById(R.id.grid_preset_colors);
        if (gridLayout == null) return;

        gridLayout.removeAllViews();

        int padding = 8;
        int size = getContext().getResources().getDimensionPixelSize(R.dimen.color_item_size);

        for (int i = 0; i < presetColors.length; i++) {
            final int color = presetColors[i];

            LinearLayout colorItem = new LinearLayout(getContext());
            colorItem.setOrientation(LinearLayout.VERTICAL);
            colorItem.setPadding(padding, padding, padding, padding);

            View colorView = new View(getContext());
            colorView.setBackgroundColor(color);
            colorView.setLayoutParams(new LinearLayout.LayoutParams(size, size));

            TextView colorName = new TextView(getContext());
            colorName.setText(colorNames[i]);
            colorName.setTextSize(10);
            colorName.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
            colorName.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            colorItem.addView(colorView);
            colorItem.addView(colorName);

            colorItem.setOnClickListener(v -> {
                currentColor = color;
                updateCurrentColorPreview();
                if (listener != null) {
                    listener.onColorSelected(color);
                    dismiss();
                }
            });

            gridLayout.addView(colorItem);
        }
    }

    private void setupCustomColorPicker() {
        AppCompatSeekBar seekBarRed = findViewById(R.id.seekbar_red);
        AppCompatSeekBar seekBarGreen = findViewById(R.id.seekbar_green);
        AppCompatSeekBar seekBarBlue = findViewById(R.id.seekbar_blue);

        if (seekBarRed == null || seekBarGreen == null || seekBarBlue == null) return;

        // 设置初始值
        seekBarRed.setProgress(Color.red(currentColor));
        seekBarGreen.setProgress(Color.green(currentColor));
        seekBarBlue.setProgress(Color.blue(currentColor));

        SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int red = seekBarRed.getProgress();
                    int green = seekBarGreen.getProgress();
                    int blue = seekBarBlue.getProgress();
                    currentColor = Color.rgb(red, green, blue);
                    updateCurrentColorPreview();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        seekBarRed.setOnSeekBarChangeListener(seekBarChangeListener);
        seekBarGreen.setOnSeekBarChangeListener(seekBarChangeListener);
        seekBarBlue.setOnSeekBarChangeListener(seekBarChangeListener);
    }

    private void setupCurrentColorPreview() {
        updateCurrentColorPreview();
        
        View confirmButton = findViewById(R.id.btn_confirm);
        if (confirmButton != null) {
            confirmButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onColorSelected(currentColor);
                }
                dismiss();
            });
        }
    }

    private void updateCurrentColorPreview() {
        View colorPreview = findViewById(R.id.view_current_color);
        if (colorPreview != null) {
            colorPreview.setBackgroundColor(currentColor);
        }

        TextView rgbText = findViewById(R.id.tv_rgb_value);
        if (rgbText != null) {
            int red = Color.red(currentColor);
            int green = Color.green(currentColor);
            int blue = Color.blue(currentColor);
            rgbText.setText(String.format("RGB: %d, %d, %d", red, green, blue));
        }
    }
}