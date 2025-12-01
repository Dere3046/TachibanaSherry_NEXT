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

/**
 * 橘雪莉表情包生成器主界面
 * 功能：根据用户输入的文字和设置生成自定义表情包图片
 */
public class MainActivity extends AppCompatActivity {

    // 日志标签
    private static final String TAG = "MainActivity";

    // UI组件变量
    private ImageView ivPreview;
    private EditText etInputText, etMaxFontSize, etOutlineWidth;
    private Spinner spinnerBackground, spinnerFont;
    private Button btnColorPicker, btnGeneratePreview, btnSaveImage;
    private CheckBox cbBold, cbOutline;
    private TextView tvStatus;
    private View viewColorPreview;

    // 状态数据
    private int currentTextColor = Color.WHITE; // 当前选择的文字颜色
    private Bitmap currentPreviewBitmap = null; // 当前预览的图片
    private final ExecutorService executor = Executors.newSingleThreadExecutor(); // 后台线程池
    private final Handler handler = new Handler(Looper.getMainLooper()); // 主线程处理器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 启用夜间模式支持，跟随系统设置
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化界面组件
        initViews();
        // 加载资源数据（背景图片和字体）
        loadAssetsData();
        // 设置事件监听器
        setupListeners();
    }

    /**
     * 初始化所有界面组件
     */
    private void initViews() {
        Log.d(TAG, "开始初始化界面组件");
        
        try {
            // 绑定所有UI组件
            ivPreview = findViewById(R.id.iv_preview);
            etInputText = findViewById(R.id.et_input_text);
            etMaxFontSize = findViewById(R.id.et_max_font_size);
            etOutlineWidth = findViewById(R.id.et_outline_width);
            spinnerBackground = findViewById(R.id.spinner_background);
            spinnerFont = findViewById(R.id.spinner_font);
            btnColorPicker = findViewById(R.id.btn_color_picker);
            btnGeneratePreview = findViewById(R.id.btn_generate_preview);
            btnSaveImage = findViewById(R.id.btn_save_image);
            cbBold = findViewById(R.id.cb_bold);
            cbOutline = findViewById(R.id.cb_outline);
            tvStatus = findViewById(R.id.tv_status);
            viewColorPreview = findViewById(R.id.view_color_preview);
            
            // 设置颜色预览的初始颜色
            if (viewColorPreview == null) {
                Log.e(TAG, "颜色预览视图未找到！");
            } else {
                viewColorPreview.setBackgroundColor(currentTextColor);
                updateButtonTextColor();
            }
            
            Log.d(TAG, "所有界面组件初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "初始化界面组件时发生错误", e);
            throw new RuntimeException("界面初始化失败", e);
        }
    }

    /**
     * 设置各种事件监听器
     */
    private void setupListeners() {
        Log.d(TAG, "正在设置事件监听器");
        
        // 颜色选择按钮点击事件
        btnColorPicker.setOnClickListener(v -> {
            try {
                showColorPickerDialog();
            } catch (Exception e) {
                Log.e(TAG, "颜色选择器出错", e);
                Toast.makeText(MainActivity.this, "颜色选择失败", Toast.LENGTH_SHORT).show();
            }
        });

        // 生成预览按钮点击事件
        btnGeneratePreview.setOnClickListener(v -> {
            try {
                String text = etInputText.getText().toString();
                if (text.isEmpty()) {
                    Toast.makeText(this, "请输入要生成的文字内容", Toast.LENGTH_SHORT).show();
                    return;
                }
                generatePreview(text);
            } catch (Exception e) {
                Log.e(TAG, "生成预览时出错", e);
                Toast.makeText(MainActivity.this, "生成预览失败", Toast.LENGTH_LONG).show();
            }
        });

        // 保存图片按钮点击事件
        btnSaveImage.setOnClickListener(v -> {
            try {
                if (currentPreviewBitmap == null) {
                    Toast.makeText(this, "请先生成预览图片", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveCurrentImage();
            } catch (Exception e) {
                Log.e(TAG, "保存图片时出错", e);
                Toast.makeText(MainActivity.this, "保存图片失败", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 从assets文件夹加载背景图片和字体文件列表
     */
    private void loadAssetsData() {
        Log.d(TAG, "开始加载资源数据");
        
        AssetManager assets = getAssets();
        List<String> bgList = new ArrayList<>();
        List<String> fontList = new ArrayList<>();

        try {
            // 读取background_images文件夹中的图片文件
            String[] images = assets.list("background_images");
            if (images != null) {
                bgList.addAll(Arrays.asList(images));
                Log.d(TAG, "找到 " + images.length + " 张背景图片");
            } else {
                Log.w(TAG, "未找到背景图片");
            }

            // 读取fonts文件夹中的字体文件
            String[] fonts = assets.list("fonts");
            if (fonts != null) {
                fontList.addAll(Arrays.asList(fonts));
                Log.d(TAG, "找到 " + fonts.length + " 种字体");
            } else {
                Log.w(TAG, "未找到字体文件");
            }

        } catch (IOException e) {
            Log.e(TAG, "加载资源时发生错误", e);
            tvStatus.setText("资源加载失败: " + e.getMessage());
        }

        // 设置下拉选择器的适配器
        try {
            ArrayAdapter<String> bgAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, bgList);
            spinnerBackground.setAdapter(bgAdapter);

            ArrayAdapter<String> fontAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, fontList);
            spinnerFont.setAdapter(fontAdapter);
            
            // 检查资源是否为空并给出提示
            if (bgList.isEmpty()) {
                tvStatus.setText("警告: assets/background_images 文件夹为空");
                Log.w(TAG, "没有可用的背景图片");
            }
            if (fontList.isEmpty()) {
                tvStatus.setText("警告: assets/fonts 文件夹为空");
                Log.w(TAG, "没有可用的字体文件");
            }
        } catch (Exception e) {
            Log.e(TAG, "设置下拉选择器时出错", e);
        }
    }

    /**
     * 显示颜色选择对话框
     */
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
    
    /**
     * 根据背景色更新按钮文字颜色，确保可读性
     */
    private void updateButtonTextColor() {
        if (btnColorPicker != null) {
            btnColorPicker.setTextColor(getContrastColor(currentTextColor));
        }
    }
    
    /**
     * 判断颜色是否为深色
     */
    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }
    
    /**
     * 根据背景色获取对比色（白色或黑色）
     */
    private int getContrastColor(int backgroundColor) {
        return isColorDark(backgroundColor) ? Color.WHITE : Color.BLACK;
    }

    // ========================= 核心图片生成逻辑 =========================

    /**
     * 生成图片预览（不保存到相册）
     */
    private void generatePreview(String text) {
        tvStatus.setText("正在生成预览...");
        btnGeneratePreview.setEnabled(false);
        btnSaveImage.setEnabled(false);

        // 获取用户当前选择的参数
        final String bgFileName = (String) spinnerBackground.getSelectedItem();
        final String fontFileName = (String) spinnerFont.getSelectedItem();
        final boolean isBold = cbBold.isChecked();
        final boolean isOutline = cbOutline.isChecked();
        
        // 解析数值参数，使用默认值如果输入无效
        int tempOutlineWidth;
        try {
            tempOutlineWidth = Integer.parseInt(etOutlineWidth.getText().toString());
        } catch (NumberFormatException e) {
            tempOutlineWidth = 2; // 默认描边宽度
        }
        final int outlineWidth = tempOutlineWidth;
        
        int tempMaxFontSize;
        try {
            tempMaxFontSize = Integer.parseInt(etMaxFontSize.getText().toString());
        } catch (NumberFormatException e) {
            tempMaxFontSize = 120; // 默认最大字号
        }
        final int maxFontSize = tempMaxFontSize;

        // 在后台线程执行图片生成任务
        executor.execute(() -> {
            try {
                Log.d(TAG, "开始后台图片生成任务");
                
                // 1. 加载背景图片
                if (bgFileName == null) {
                    throw new Exception("请选择一张背景图片");
                }
                
                InputStream is = getAssets().open("background_images/" + bgFileName);
                Bitmap originalBitmap = BitmapFactory.decodeStream(is);
                if (originalBitmap == null) {
                    throw new Exception("背景图片加载失败");
                }
                
                Log.d(TAG, "背景图片加载成功: " + bgFileName);
                
                // 2. 调整图片尺寸为 900x900 像素
                Bitmap canvasBitmap = Bitmap.createScaledBitmap(originalBitmap, 900, 900, true);
                
                // 3. 创建可编辑的位图副本
                Bitmap mutableBitmap = canvasBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(mutableBitmap);
                
                // 4. 加载并设置字体
                Typeface typeface = Typeface.DEFAULT;
                if (fontFileName != null && !fontFileName.isEmpty()) {
                    typeface = Typeface.createFromAsset(getAssets(), "fonts/" + fontFileName);
                    Log.d(TAG, "字体加载成功: " + fontFileName);
                }
                // 应用加粗效果
                if (isBold) {
                    typeface = Typeface.create(typeface, Typeface.BOLD);
                }

                // 5. 计算文字布局（自动分行和字号调整）
                TextLayoutResult layoutResult = calculateTextLayout(text, typeface, maxFontSize);
                
                // 6. 在画布上绘制文字
                drawTextOnCanvas(canvas, layoutResult, currentTextColor, isOutline, outlineWidth);

                // 7. 更新UI（必须在主线程执行）
                handler.post(() -> {
                    currentPreviewBitmap = mutableBitmap; // 保存当前预览图片
                    ivPreview.setImageBitmap(mutableBitmap); // 显示预览图片
                    tvStatus.setText("预览生成完成！点击保存按钮保存图片");
                    btnGeneratePreview.setEnabled(true);
                    btnSaveImage.setEnabled(true); // 启用保存按钮
                    Log.d(TAG, "预览生成完成");
                });

            } catch (Exception e) {
                Log.e(TAG, "后台图片生成任务出错", e);
                handler.post(() -> {
                    tvStatus.setText("生成失败: " + e.getMessage());
                    btnGeneratePreview.setEnabled(true);
                    Toast.makeText(MainActivity.this, "生成预览失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 保存当前预览的图片到相册
     */
    private void saveCurrentImage() {
        if (currentPreviewBitmap == null) {
            Toast.makeText(this, "没有可保存的图片", Toast.LENGTH_SHORT).show();
            return;
        }

        tvStatus.setText("正在保存图片...");
        btnSaveImage.setEnabled(false);

        executor.execute(() -> {
            try {
                String savedPath = saveImageToGallery(currentPreviewBitmap);
                Log.d(TAG, "图片保存成功: " + savedPath);

                handler.post(() -> {
                    tvStatus.setText("保存成功！图片已保存到相册");
                    btnSaveImage.setEnabled(true);
                    Toast.makeText(MainActivity.this, "图片保存成功", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "图片保存流程完成");
                });

            } catch (Exception e) {
                Log.e(TAG, "保存图片时出错", e);
                handler.post(() -> {
                    tvStatus.setText("保存失败: " + e.getMessage());
                    btnSaveImage.setEnabled(true);
                    Toast.makeText(MainActivity.this, "保存失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 辅助类：存储文字布局计算结果
     * 包含分行信息、字号、行高和绘制工具
     */
    private static class TextLayoutResult {
        List<String> lines; // 分行后的文字列表
        float fontSize;     // 计算出的最佳字号
        float lineHeight;   // 行高
        float totalHeight;  // 文字区域总高度
        Paint paint;        // 绘制工具
        
        TextLayoutResult(List<String> lines, float fontSize, float lineHeight, float totalHeight, Paint paint) {
            this.lines = lines;
            this.fontSize = fontSize;
            this.lineHeight = lineHeight;
            this.totalHeight = totalHeight;
            this.paint = paint;
        }
    }

    /**
     * 计算文字的自适应布局（自动调整字号和分行）
     */
    private TextLayoutResult calculateTextLayout(String text, Typeface typeface, int startFontSize) {
        int maxWidth = 800; // 最大文字宽度（900px画布减去边距）
        int maxTotalHeight = 300; // 底部文字区域最大高度
        int minFontSize = 20; // 最小字号限制

        Paint paint = new Paint();
        paint.setTypeface(typeface);
        paint.setAntiAlias(true); // 开启抗锯齿

        float currentSize = startFontSize;

        // 从最大字号开始尝试，逐步减小直到找到合适的字号
        while (currentSize >= minFontSize) {
            paint.setTextSize(currentSize);
            Paint.FontMetrics metrics = paint.getFontMetrics();
            float lineHeight = metrics.descent - metrics.ascent; // 计算行高
            
            // 将文字按最大宽度分行
            List<String> lines = breakTextIntoLines(text, paint, maxWidth);
            float totalHeight = lines.size() * lineHeight;

            // 如果总高度在允许范围内，使用当前字号
            if (totalHeight <= maxTotalHeight) {
                return new TextLayoutResult(lines, currentSize, lineHeight, totalHeight, paint);
            }
            
            currentSize -= 5; // 每次减小5px继续尝试
        }

        // 如果所有尝试都失败，使用最小字号
        paint.setTextSize(minFontSize);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float lineHeight = metrics.descent - metrics.ascent;
        List<String> lines = breakTextIntoLines(text, paint, maxWidth);
        float totalHeight = lines.size() * lineHeight;
        return new TextLayoutResult(lines, minFontSize, lineHeight, totalHeight, paint);
    }

    /**
     * 将长文本按最大宽度分割成多行
     */
    private List<String> breakTextIntoLines(String text, Paint paint, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        
        // 首先按硬回车分行
        String[] paragraphs = text.split("\n");
        
        // 对每个段落按宽度进一步分行
        for (String paragraph : paragraphs) {
            int start = 0;
            while (start < paragraph.length()) {
                // 计算当前行能容纳的字符数
                int count = paint.breakText(paragraph, start, paragraph.length(), true, maxWidth, null);
                if (count <= 0) break;
                // 提取子字符串作为一行
                lines.add(paragraph.substring(start, start + count));
                start += count;
            }
        }
        
        // 确保至少有一行
        if (lines.isEmpty()) {
            lines.add(text);
        }
        return lines;
    }

    /**
     * 在画布上绘制文字（支持描边效果）
     */
    private void drawTextOnCanvas(Canvas canvas, TextLayoutResult result, int color, boolean useOutline, int outlineWidth) {
        int canvasHeight = 900;
        int textAreaBottomY = 900; // 文字区域底部Y坐标
        int textAreaTopY = 600;    // 文字区域顶部Y坐标

        // 计算文字起始Y坐标，确保在底部区域且不超出范围
        float startY = textAreaBottomY - result.totalHeight - 30; // 底部留白30px
        if (startY < textAreaTopY) startY = textAreaTopY;

        Paint textPaint = result.paint;
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        
        // 如果启用描边，创建描边画笔
        Paint outlinePaint = null;
        if (useOutline && outlineWidth > 0) {
            outlinePaint = new Paint(textPaint);
            outlinePaint.setStyle(Paint.Style.STROKE);
            outlinePaint.setStrokeWidth(outlineWidth * 2); // Android描边是居中的，所以要乘以2
            outlinePaint.setColor(Color.BLACK); // 描边颜色固定为黑色
        }
        
        // 设置文字颜色和填充样式
        textPaint.setColor(color);
        textPaint.setStyle(Paint.Style.FILL);

        // 计算第一行的基线位置
        float y = startY - metrics.ascent; // drawText的y坐标是基线，所以要减去ascent（ascent是负数）

        // 逐行绘制文字
        for (String line : result.lines) {
            float textWidth = textPaint.measureText(line);
            float x = (900 - textWidth) / 2; // 水平居中

            // 如果启用描边，先绘制描边（作为文字背景）
            if (useOutline && outlineWidth > 0 && outlinePaint != null) {
                canvas.drawText(line, x, y, outlinePaint);
            }
            // 绘制填充文字
            canvas.drawText(line, x, y, textPaint);

            // 移动到下一行
            y += result.lineHeight;
        }
    }

    /**
     * 将图片保存到系统相册（Android 11+兼容）
     */
    private String saveImageToGallery(Bitmap bitmap) throws IOException {
        // 生成唯一的文件名
        String filename = "Sherry表情包_" + System.currentTimeMillis() + ".png";
        
        // 创建媒体存储内容值
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        
        // Android 10+ 使用相对路径
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TachibanaSherry");
        }

        // 插入媒体记录并获取URI
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("无法创建文件记录");

        // 将图片数据写入输出流
        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) throw new IOException("无法打开文件输出流");
            boolean success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            if (!success) throw new IOException("图片压缩失败");
        }
        
        return uri.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭线程池，释放资源
        executor.shutdown();
    }
}