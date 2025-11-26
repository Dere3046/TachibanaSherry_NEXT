package com.dere3046.tachibanasherry;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // UI 组件
    private ImageView ivPreview;
    private EditText etInputText, etMaxFontSize, etOutlineWidth;
    private Spinner spinnerBackground, spinnerFont;
    private Button btnColorPicker, btnGenerate;
    private CheckBox cbBold, cbOutline;
    private TextView tvStatus;
    private View viewColorPreview;

    // 状态数据
    private int currentTextColor = Color.WHITE;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 启用夜间模式支持 - 跟随系统设置
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadAssetsData();
        setupListeners();
    }

    private void initViews() {
        Log.d(TAG, "Initializing views");
        
        try {
            ivPreview = findViewById(R.id.iv_preview);
            etInputText = findViewById(R.id.et_input_text);
            etMaxFontSize = findViewById(R.id.et_max_font_size);
            etOutlineWidth = findViewById(R.id.et_outline_width);
            spinnerBackground = findViewById(R.id.spinner_background);
            spinnerFont = findViewById(R.id.spinner_font);
            btnColorPicker = findViewById(R.id.btn_color_picker);
            btnGenerate = findViewById(R.id.btn_generate);
            cbBold = findViewById(R.id.cb_bold);
            cbOutline = findViewById(R.id.cb_outline);
            tvStatus = findViewById(R.id.tv_status);
            viewColorPreview = findViewById(R.id.view_color_preview);
            
            if (viewColorPreview == null) {
                Log.e(TAG, "viewColorPreview is null!");
            } else {
                viewColorPreview.setBackgroundColor(currentTextColor);
                updateButtonTextColor();
            }
            
            Log.d(TAG, "All views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw new RuntimeException("View initialization failed", e);
        }
    }

    private void setupListeners() {
        Log.d(TAG, "Setting up listeners");
        
        // 简单的颜色选择器
        btnColorPicker.setOnClickListener(v -> {
            try {
                showColorPickerDialog();
            } catch (Exception e) {
                Log.e(TAG, "Error in color picker", e);
                Toast.makeText(MainActivity.this, "颜色选择失败", Toast.LENGTH_SHORT).show();
            }
        });

        // 生成按钮
        btnGenerate.setOnClickListener(v -> {
            try {
                String text = etInputText.getText().toString();
                if (text.isEmpty()) {
                    Toast.makeText(this, "请输入文字", Toast.LENGTH_SHORT).show();
                    return;
                }
                startGeneration(text);
            } catch (Exception e) {
                Log.e(TAG, "Error in generate button", e);
                Toast.makeText(MainActivity.this, "生成失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 从 assets 文件夹读取文件列表
    private void loadAssetsData() {
        Log.d(TAG, "Loading assets data");
        
        AssetManager assets = getAssets();
        List<String> bgList = new ArrayList<>();
        List<String> fontList = new ArrayList<>();

        try {
            // 读取 background_images 文件夹
            String[] images = assets.list("background_images");
            if (images != null) {
                bgList.addAll(Arrays.asList(images));
                Log.d(TAG, "Found " + images.length + " background images");
            } else {
                Log.w(TAG, "No background images found");
            }

            // 读取 fonts 文件夹
            String[] fonts = assets.list("fonts");
            if (fonts != null) {
                fontList.addAll(Arrays.asList(fonts));
                Log.d(TAG, "Found " + fonts.length + " fonts");
            } else {
                Log.w(TAG, "No fonts found");
            }

        } catch (IOException e) {
            Log.e(TAG, "Error loading assets", e);
            tvStatus.setText("资源加载失败: " + e.getMessage());
        }

        // 设置 Spinner 适配器
        try {
            ArrayAdapter<String> bgAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, bgList);
            spinnerBackground.setAdapter(bgAdapter);

            ArrayAdapter<String> fontAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, fontList);
            spinnerFont.setAdapter(fontAdapter);
            
            if (bgList.isEmpty()) {
                tvStatus.setText("警告: assets/background_images 为空");
                Log.w(TAG, "No background images available");
            }
            if (fontList.isEmpty()) {
                tvStatus.setText("警告: assets/fonts 为空");
                Log.w(TAG, "No fonts available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up spinners", e);
        }
    }

    private void showColorPickerDialog() {
        ColorPickerDialog colorPickerDialog = new ColorPickerDialog(
            this,
            new ColorPickerDialog.OnColorSelectedListener() {
                @Override
                public void onColorSelected(int color) {
                    currentTextColor = color;
                    if (viewColorPreview != null) {
                        viewColorPreview.setBackgroundColor(currentTextColor);
                    }
                    updateButtonTextColor();
                }
            },
            currentTextColor
        );
        
        colorPickerDialog.show();
    }
    
    private void updateButtonTextColor() {
        if (btnColorPicker != null) {
            btnColorPicker.setTextColor(getContrastColor(currentTextColor));
        }
    }
    
    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }
    
    private int getContrastColor(int backgroundColor) {
        return isColorDark(backgroundColor) ? Color.WHITE : Color.BLACK;
    }

    // === 核心逻辑 ===
    private void startGeneration(String text) {
        tvStatus.setText("正在生成...");
        btnGenerate.setEnabled(false);

        // 获取当前UI配置参数
        final String bgFileName = (String) spinnerBackground.getSelectedItem();
        final String fontFileName = (String) spinnerFont.getSelectedItem();
        final boolean isBold = cbBold.isChecked();
        final boolean isOutline = cbOutline.isChecked();
        
        // 解析数值参数 - 使用临时变量
        int tempOutlineWidth;
        try {
            tempOutlineWidth = Integer.parseInt(etOutlineWidth.getText().toString());
        } catch (NumberFormatException e) {
            tempOutlineWidth = 2;
        }
        final int outlineWidth = tempOutlineWidth;
        
        int tempMaxFontSize;
        try {
            tempMaxFontSize = Integer.parseInt(etMaxFontSize.getText().toString());
        } catch (NumberFormatException e) {
            tempMaxFontSize = 120;
        }
        final int maxFontSize = tempMaxFontSize;

        executor.execute(() -> {
            try {
                Log.d(TAG, "Background task started");
                
                // 1. 加载图片
                if (bgFileName == null) {
                    throw new Exception("未选择背景图");
                }
                
                InputStream is = getAssets().open("background_images/" + bgFileName);
                Bitmap originalBitmap = BitmapFactory.decodeStream(is);
                if (originalBitmap == null) {
                    throw new Exception("背景图加载失败");
                }
                
                Log.d(TAG, "Background image loaded: " + bgFileName);
                
                // 2. 强制调整尺寸为 900x900
                Bitmap canvasBitmap = Bitmap.createScaledBitmap(originalBitmap, 900, 900, true);
                
                
                Bitmap mutableBitmap = canvasBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(mutableBitmap);
                
                // 3. 准备字体
                Typeface typeface = Typeface.DEFAULT;
                if (fontFileName != null && !fontFileName.isEmpty()) {
                    typeface = Typeface.createFromAsset(getAssets(), "fonts/" + fontFileName);
                    Log.d(TAG, "Font loaded: " + fontFileName);
                }
                // 应用加粗
                if (isBold) {
                    typeface = Typeface.create(typeface, Typeface.BOLD);
                }

                // 4. 计算最佳字号和行布局
                TextLayoutResult layoutResult = calculateTextLayout(text, typeface, maxFontSize);
                
                // 5. 绘制文字
                drawTextOnCanvas(canvas, layoutResult, currentTextColor, isOutline, outlineWidth);

                // 6. 保存到相册
                String savedPath = saveImageToGallery(mutableBitmap);
                Log.d(TAG, "Image saved to: " + savedPath);

                // 7. 更新 UI
                handler.post(() -> {
                    ivPreview.setImageBitmap(mutableBitmap);
                    tvStatus.setText("成功! 已保存至相册");
                    Toast.makeText(MainActivity.this, "保存成功", Toast.LENGTH_LONG).show();
                    btnGenerate.setEnabled(true);
                    Log.d(TAG, "Generation completed successfully");
                });

            } catch (Exception e) {
                Log.e(TAG, "Error in background task", e);
                handler.post(() -> {
                    tvStatus.setText("错误: " + e.getMessage());
                    btnGenerate.setEnabled(true);
                    Toast.makeText(MainActivity.this, "生成失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // 辅助类：存储计算好的文字布局结果
    private static class TextLayoutResult {
        List<String> lines;
        float fontSize;
        float lineHeight;
        float totalHeight;
        Paint paint;
        
        TextLayoutResult(List<String> lines, float fontSize, float lineHeight, float totalHeight, Paint paint) {
            this.lines = lines;
            this.fontSize = fontSize;
            this.lineHeight = lineHeight;
            this.totalHeight = totalHeight;
            this.paint = paint;
        }
    }

    // 自适应字号逻辑
    private TextLayoutResult calculateTextLayout(String text, Typeface typeface, int startFontSize) {
        int maxWidth = 800; // 900 - margins
        int maxTotalHeight = 300; // 底部区域高度
        int minFontSize = 20;

        Paint paint = new Paint();
        paint.setTypeface(typeface);
        paint.setAntiAlias(true);

        float currentSize = startFontSize;

        while (currentSize >= minFontSize) {
            paint.setTextSize(currentSize);
            Paint.FontMetrics metrics = paint.getFontMetrics();
            float lineHeight = metrics.descent - metrics.ascent; // 行高
            
            // 尝试分行
            List<String> lines = breakTextIntoLines(text, paint, maxWidth);
            float totalHeight = lines.size() * lineHeight;

            if (totalHeight <= maxTotalHeight) {
                // 找到合适的大小
                return new TextLayoutResult(lines, currentSize, lineHeight, totalHeight, paint);
            }
            
            currentSize -= 5;
        }

        // 如果都失败，使用最小字号
        paint.setTextSize(minFontSize);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float lineHeight = metrics.descent - metrics.ascent;
        List<String> lines = breakTextIntoLines(text, paint, maxWidth);
        float totalHeight = lines.size() * lineHeight;
        return new TextLayoutResult(lines, minFontSize, lineHeight, totalHeight, paint);
    }

    // 将文本按宽度分行
    private List<String> breakTextIntoLines(String text, Paint paint, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        
        // 处理硬回车
        String[] paragraphs = text.split("\n");
        
        for (String paragraph : paragraphs) {
            int start = 0;
            while (start < paragraph.length()) {
                int count = paint.breakText(paragraph, start, paragraph.length(), true, maxWidth, null);
                if (count <= 0) break;
                lines.add(paragraph.substring(start, start + count));
                start += count;
            }
        }
        
        if (lines.isEmpty()) {
            lines.add(text);
        }
        return lines;
    }

    private void drawTextOnCanvas(Canvas canvas, TextLayoutResult result, int color, boolean useOutline, int outlineWidth) {
        int canvasHeight = 900;
        int textAreaBottomY = 900;
        int textAreaTopY = 600;

        float startY = textAreaBottomY - result.totalHeight - 30; // 底部留白 30px
        if (startY < textAreaTopY) startY = textAreaTopY;

        Paint textPaint = result.paint;
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        
        // 用于描边的 Paint
        Paint outlinePaint = null;
        if (useOutline && outlineWidth > 0) {
            outlinePaint = new Paint(textPaint);
            outlinePaint.setStyle(Paint.Style.STROKE);
            outlinePaint.setStrokeWidth(outlineWidth * 2); // Android stroke 是居中的，所以要乘2
            outlinePaint.setColor(Color.BLACK);
        }
        
        textPaint.setColor(color);
        textPaint.setStyle(Paint.Style.FILL);

        float y = startY - metrics.ascent; // drawText 的 y 是 baseline，所以要减去 ascent (ascent 是负数)

        for (String line : result.lines) {
            float textWidth = textPaint.measureText(line);
            float x = (900 - textWidth) / 2; // 居中

            if (useOutline && outlineWidth > 0 && outlinePaint != null) {
                canvas.drawText(line, x, y, outlinePaint);
            }
            canvas.drawText(line, x, y, textPaint);

            y += result.lineHeight;
        }
    }

    // Android 11 (API 30) 保存图片的标准方式
    private String saveImageToGallery(Bitmap bitmap) throws IOException {
        String filename = "Sherry_" + System.currentTimeMillis() + ".png";
        
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TachibanaSherry");
        }

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("无法创建文件");

        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) throw new IOException("无法写入文件");
            boolean success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            if (!success) throw new IOException("图片压缩失败");
        }
        
        return uri.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}