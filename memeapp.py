import os  
import time  
import threading  
import tkinter as tk  
from tkinter import ttk, filedialog, messagebox, colorchooser  
from PIL import Image, ImageTk  
from generator import ImageGenerator

class MemeApp:
    def __init__(self):
        self.root = tk.Tk()
        self.root.title("橘雪莉表情包生成器 (Refactored)")
        self.root.geometry("1000x700") # 宽一点，左右布局
        
        # 初始化逻辑处理器
        self.generator = ImageGenerator()
        
        # 状态变量
        self.var_text_color = (255, 255, 255) # RGB
        self.var_font_size = tk.IntVar(value=100)
        self.var_use_outline = tk.BooleanVar(value=True)
        self.var_outline_width = tk.IntVar(value=3)
        self.var_bg_file = tk.StringVar()
        self.var_font_file = tk.StringVar()
        
        # 预览图缓存
        self.current_image_obj = None 
        self._preview_job = None # 用于防抖动

        self._setup_ui()
        self._load_resources()
        
        # 初始刷新
        self._trigger_preview_update()

    def _load_resources(self):
        """加载资源文件列表"""
        # 背景
        bgs = self.generator.get_files(self.generator.bg_folder, ('.png', '.jpg', '.jpeg'))
        self.combo_bg['values'] = bgs
        if bgs:
            self.combo_bg.current(0)
        
        # 字体
        fonts = self.generator.get_files(self.generator.font_folder, ('.ttf', '.otf'))
        self.combo_font['values'] = fonts
        if fonts:
            self.combo_font.current(0)

    def _setup_ui(self):
        """搭建左右分栏的界面"""
        # === 主容器 ===
        main_paned = tk.PanedWindow(self.root, orient=tk.HORIZONTAL)
        main_paned.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)

        # === 左侧控制面板 ===
        left_frame = ttk.Frame(main_paned)
        main_paned.add(left_frame, width=400)

        # 1. 文本输入区
        group_text = ttk.LabelFrame(left_frame, text="1. 输入文字")
        group_text.pack(fill=tk.X, pady=5)
        
        self.text_input = tk.Text(group_text, height=5, width=30)
        self.text_input.pack(fill=tk.X, padx=5, pady=5)
        # 绑定键盘弹起事件，实现打字即预览
        self.text_input.bind('<KeyRelease>', self._on_input_change)

        # 2. 外观设置区
        group_style = ttk.LabelFrame(left_frame, text="2. 样式设置")
        group_style.pack(fill=tk.X, pady=5)

        # 颜色选择
        self.btn_color = tk.Button(group_style, text="点击修改文字颜色", bg="white", command=self._choose_color)
        self.btn_color.pack(fill=tk.X, padx=5, pady=5)

        # 字体大小 & 描边
        frame_sliders = ttk.Frame(group_style)
        frame_sliders.pack(fill=tk.X, padx=5)
        
        ttk.Label(frame_sliders, text="最大字号:").grid(row=0, column=0, sticky='w')
        s1 = ttk.Scale(frame_sliders, from_=20, to=200, variable=self.var_font_size, command=self._on_input_change)
        s1.grid(row=0, column=1, sticky='ew')
        
        ttk.Checkbutton(frame_sliders, text="启用描边", variable=self.var_use_outline, command=self._on_input_change).grid(row=1, column=0, sticky='w')
        s2 = ttk.Scale(frame_sliders, from_=0, to=10, variable=self.var_outline_width, command=self._on_input_change)
        s2.grid(row=1, column=1, sticky='ew')

        # 3. 资源选择区
        group_res = ttk.LabelFrame(left_frame, text="3. 资源选择")
        group_res.pack(fill=tk.X, pady=5)

        ttk.Label(group_res, text="背景图片:").pack(anchor='w', padx=5)
        self.combo_bg = ttk.Combobox(group_res, textvariable=self.var_bg_file, state="readonly")
        self.combo_bg.pack(fill=tk.X, padx=5, pady=2)
        self.combo_bg.bind("<<ComboboxSelected>>", self._on_input_change)
        
        # 添加背景按钮
        btn_add_bg = ttk.Button(group_res, text="+ 添加新背景", command=self._add_background)
        btn_add_bg.pack(anchor='e', padx=5, pady=2)

        ttk.Label(group_res, text="字体文件:").pack(anchor='w', padx=5, pady=(10, 0))
        self.combo_font = ttk.Combobox(group_res, textvariable=self.var_font_file, state="readonly")
        self.combo_font.pack(fill=tk.X, padx=5, pady=2)
        self.combo_font.bind("<<ComboboxSelected>>", self._on_input_change)

        # 4. 保存按钮
        self.btn_save = ttk.Button(left_frame, text="保存图片 (Save)", command=self._save_image)
        self.btn_save.pack(fill=tk.X, pady=20, ipady=10)

        # === 右侧预览面板 ===
        right_frame = ttk.Frame(main_paned)
        main_paned.add(right_frame)

        self.lbl_preview = ttk.Label(right_frame, text="预览区域", anchor="center", background="#e0e0e0")
        self.lbl_preview.pack(fill=tk.BOTH, expand=True)
        # 监听窗口大小变化，调整预览图大小
        self.lbl_preview.bind('<Configure>', self._on_resize_preview)

        # --- 使用说明 ---
        instruction_text = """
        シェリーちゃん可愛い大好き！
        如果发现有些操作按钮没有出来，可以试试看把应用程序框拖大点！

        你是谁？请支持《魔法少女的魔女裁判》喵！"""
        instruction_label = ttk.Label(self.root, text=instruction_text, justify=tk.LEFT, wraplength=450)
        instruction_label.pack(side=tk.BOTTOM, fill=tk.X, padx=20, pady=10)



    # --- 交互回调函数 ---

    def _choose_color(self):
        """弹出颜色选择器"""
        colors = colorchooser.askcolor(initialcolor='#%02x%02x%02x' % self.var_text_color)
        if colors[0]:
            self.var_text_color = tuple(map(int, colors[0])) # 转成 (r,g,b)
            self.btn_color.config(bg=colors[1]) # 更新按钮颜色
            self._trigger_preview_update()

    def _add_background(self):
        """添加背景图"""
        path = filedialog.askopenfilename(filetypes=[("Images", "*.png;*.jpg;*.jpeg")])
        if path:
            try:
                img = Image.open(path)
                filename = os.path.basename(path)
                target = os.path.join(self.renderer.bg_folder, filename)
                img.save(target)
                self._load_resources() # 刷新列表
                self.combo_bg.set(filename)
                self._trigger_preview_update()
                messagebox.showinfo("成功", "背景已添加")
            except Exception as e:
                messagebox.showerror("错误", f"无法添加图片: {e}")

    def _on_input_change(self, event=None):
        """
        统一的事件处理入口。
        为了防止打字时频繁渲染导致卡顿，这里使用了简单的防抖动 (Debounce) 机制。
        """
        if self._preview_job:
            self.root.after_cancel(self._preview_job)
        # 延迟 300ms 后执行渲染，如果期间又有输入，则重置计时
        self._preview_job = self.root.after(300, self._trigger_preview_update)

    def _trigger_preview_update(self):
        """收集当前所有设置，并在后台线程生成预览图"""
        # 1. 收集参数
        settings = {
            'text': self.text_input.get("1.0", tk.END).strip(),
            'text_color': self.var_text_color,
            'font_size': self.var_font_size.get(),
            'use_outline': self.var_use_outline.get(),
            'outline_width': self.var_outline_width.get(),
            'bg_path': os.path.join(self.generator.bg_folder, self.var_bg_file.get()) if self.var_bg_file.get() else None,
            'font_file': self.var_font_file.get()
        }

        # 2. 线程生成 (避免卡死UI)
        thread = threading.Thread(target=self._generate_task, args=(settings,))
        thread.daemon = True
        thread.start()

    def _generate_task(self, settings):
        """[线程内部] 调用渲染器"""
        image = self.generator.render_image(settings)
        # 渲染完后，回到主线程更新UI
        self.root.after(0, self._update_preview_ui, image)

    def _update_preview_ui(self, pil_image):
        """[主线程] 更新显示的图片"""
        self.current_image_obj = pil_image # 保存一份原始高清图用于保存
        
        # 计算缩放以适应预览窗口
        win_w = self.lbl_preview.winfo_width()
        win_h = self.lbl_preview.winfo_height()
        
        if win_w < 10 or win_h < 10: return # 窗口太小时不渲染

        # 保持比例缩放
        ratio = min(win_w / 900, win_h / 900)
        new_size = (int(900 * ratio), int(900 * ratio))
        
        try:
            preview_img = pil_image.resize(new_size, Image.Resampling.LANCZOS)
            tk_img = ImageTk.PhotoImage(preview_img)
            self.lbl_preview.config(image=tk_img, text="") # 清除文字，显示图片
            self.lbl_preview.image = tk_img # 保持引用防止被垃圾回收
        except Exception as e:
            print(f"预览更新失败: {e}")

    def _on_resize_preview(self, event):
        """当窗口大小改变时，重新调整预览图大小"""
        if self.current_image_obj:
            # 这里不用重新渲染文字，只需要重设尺寸
            self._update_preview_ui(self.current_image_obj)

    def _save_image(self):
        """保存图片到本地"""
        if not self.current_image_obj:
            return
        
        timestamp = time.strftime("%Y%m%d%H%M%S")
        filename = f"{timestamp}.png"
        save_path = os.path.join("output_images", filename)
        
        try:
            self.current_image_obj.save(save_path)
            messagebox.showinfo("保存成功", f"图片已保存至:\n{save_path}")
        except Exception as e:
            messagebox.showerror("保存失败", str(e))

    def run(self):
        self.root.mainloop()

if __name__ == "__main__":
    app = MemeApp()
    app.run()
