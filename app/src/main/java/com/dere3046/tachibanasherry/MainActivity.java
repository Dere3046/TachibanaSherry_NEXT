package com.dere3046.tachibanasherry;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    // UI 组件
    private ImageView ivPreview;
    private EditText etInputText, etMaxFontSize, etOutlineWidth;
    private Spinner spinnerBackground, spinnerFont;
    private Button btnColorPicker, btnGenerate;
    private CheckBox cbBold, cbOutline;
    private TextView tvStatus, tvStatusBottom;
    private View viewColorPreview;

    // 状态数据
    private int currentTextColor = Color.WHITE;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadAssetsData(); // 加载 Assets 中的背景和字体列表
        setupListeners();
    }

    private void initViews() {
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
        
        viewColorPreview.setBackgroundColor(currentTextColor);
    }

    private void setupListeners() {
        // 简单的颜色选择器
        btnColorPicker.setOnClickListener(v -> showColorPickerDialog());

        // 生成按钮
        btnGenerate.setOnClickListener(v -> {
            String text = etInputText.getText().toString();
            if (text.isEmpty()) {
                Toast.makeText(this, "请输入文字", Toast.LENGTH_SHORT).show();
                return;
            }
            startGeneration(text);
        });
    }

    // 从 assets 文件夹读取文件列表
    private void loadAssetsData() {
        AssetManager assets = getAssets();
        List<String> bgList = new ArrayList<>();
        List<String> fontList = new ArrayList<>();

        try {
            // 读取 background_images 文件夹
            String[] images = assets.list("background_images");
            if (images != null) bgList.addAll(Arrays.asList(images));

            // 读取 fonts 文件夹
            String[] fonts = assets.list("fonts");
            if (fonts != null) fontList.addAll(Arrays.asList(fonts));

        } catch (IOException e) {
            e.printStackTrace();
            tvStatus.setText("资源加载失败: " + e.getMessage());
        }

        // 设置 Spinner 适配器
        ArrayAdapter<String> bgAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, bgList);
        spinnerBackground.setAdapter(bgAdapter);

        ArrayAdapter<String> fontAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, fontList);
        spinnerFont.setAdapter(fontAdapter);
        
        if (bgList.isEmpty()) tvStatus.setText("警告: assets/background_images 为空");
        if (fontList.isEmpty()) tvStatus.setText("警告: assets/fonts 为空");
    }

    private void showColorPickerDialog() {
        final int[] colors = {Color.WHITE, Color.BLACK, Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, 0xFFFFA500, 0xFF800080};
        final String[] colorNames = {"白色", "黑色", "红色", "绿色", "蓝色", "黄色", "青色", "橙色", "紫色"};

        new AlertDialog.Builder(this)
                .setTitle("选择文字颜色")
                .setItems(colorNames, (dialog, which) -> {
                    currentTextColor = colors[which];
                    viewColorPreview.setBackgroundColor(currentTextColor);
                    // 动态修改按钮文字颜色以保持可见性
                    btnColorPicker.setTextColor(isColorDark(currentTextColor) ? Color.WHITE : Color.BLACK);
                })
                .show();
    }
    
    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    // === 核心逻辑 ===
    private void startGeneration(String text) {
        tvStatus.setText("正在生成...");
        btnGenerate.setEnabled(false);

        // 获取当前UI配置参数
        String bgFileName = (String) spinnerBackground.getSelectedItem();
        String fontFileName = (String) spinnerFont.getSelectedItem();
        boolean isBold = cbBold.isChecked();
        boolean isOutline = cbOutline.isChecked();
        int outlineWidth = Integer.parseInt(etOutlineWidth.getText().toString());
        int maxFontSize = Integer.parseInt(etMaxFontSize.getText().toString());

        executor.execute(() -> {
            try {
                // 1. 加载图片
                if (bgFileName == null) throw new Exception("未选择背景图");
                InputStream is = getAssets().open("background_images/" + bgFileName);
                Bitmap originalBitmap = BitmapFactory.decodeStream(is);
                
                // 2. 强制调整尺寸为 900x900
                Bitmap canvasBitmap = Bitmap.createScaledBitmap(originalBitmap, 900, 900, true);
                
                // 我们需要一个可变的 Bitmap
                Bitmap mutableBitmap = canvasBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(mutableBitmap);
                
                // 3. 准备字体
                Typeface typeface = Typeface.DEFAULT;
                if (fontFileName != null) {
                    typeface = Typeface.createFromAsset(getAssets(), "fonts/" + fontFileName);
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

                // 7. 更新 UI
                handler.post(() -> {
                    ivPreview.setImageBitmap(mutableBitmap);
                    tvStatus.setText("成功! 已保存至相册");
                    Toast.makeText(MainActivity.this, "保存成功", Toast.LENGTH_LONG).show();
                    btnGenerate.setEnabled(true);
                });

            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> {
                    tvStatus.setText("错误: " + e.getMessage());
                    btnGenerate.setEnabled(true);
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
        List<String> bestLines = new ArrayList<>();
        float bestLineHeight = 0;
        float bestTotalHeight = 0;

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
        return new TextLayoutResult(breakTextIntoLines(text, paint, maxWidth), minFontSize, lineHeight, 0, paint);
    }

    // 将文本按宽度分行
    private List<String> breakTextIntoLines(String text, Paint paint, int maxWidth) {
        List<String> lines = new ArrayList<>();
        // 处理硬回车
        String[] paragraphs = text.split("\n");
        
        for (String paragraph : paragraphs) {
            int start = 0;
            while (start < paragraph.length()) {
                // measureText returns the number of chars that fit
                int count = paint.breakText(paragraph, start, paragraph.length(), true, maxWidth, null);
                lines.add(paragraph.substring(start, start + count));
                start += count;
            }
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
        Paint outlinePaint = new Paint(textPaint);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(outlineWidth * 2); // Android stroke 是居中的，所以要乘2
        outlinePaint.setColor(Color.BLACK);
        
        textPaint.setColor(color);
        textPaint.setStyle(Paint.Style.FILL);

        float y = startY - metrics.ascent; // drawText 的 y 是 baseline，所以要减去 ascent (ascent 是负数)

        for (String line : result.lines) {
            float textWidth = textPaint.measureText(line);
            float x = (900 - textWidth) / 2; // 居中

            if (useOutline && outlineWidth > 0) {
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
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TachibanaSherry"); // 保存到 Pictures/TachibanaSherry

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("无法创建文件");

        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) throw new IOException("无法写入文件");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        }
        
        return uri.toString();
    }
}