@echo off
chcp 65001 > nul
title 橘雪莉表情包生成器 - EXE打包工具
color 0A

echo.
echo ============================================
echo       橘雪莉表情包生成器 EXE打包工具
echo ============================================
echo.
echo 注意：本程序完全依赖C扩展模块
echo 如果C扩展编译失败，则无法打包
echo ============================================
echo.

echo [1/4] 正在安装必要的Python包...
pip install Pillow pyperclip psutil pywin32 keyboard > nul 2>&1
if errorlevel 1 (
    echo 警告：部分包可能安装失败
    echo 继续执行可能影响功能...
) else (
    echo 依赖包安装成功
)

echo.
echo [2/4] 正在编译C扩展模块（键盘钩子）...
if not exist keyboard_hook.c (
    echo.
    echo ❌ 错误：未找到 keyboard_hook.c 文件！
    echo 无法编译C扩展，打包终止。
    pause
    exit /b 1
)

rem 清理旧的构建文件
echo 清理旧的构建文件...
if exist build_ext rmdir /s /q build_ext > nul 2>&1
if exist build rmdir /s /q build > nul 2>&1

rem 删除可能存在的旧.pyd文件
for %%f in (*.pyd) do (
    echo 删除旧C扩展文件: %%f
    del "%%f" > nul 2>&1
)

echo 正在编译C扩展...
python setup.py build_ext --inplace

if errorlevel 1 (
    echo.
    echo ❌ 错误：C扩展编译失败！
    echo.
    rem 清理编译失败的残留文件
    echo 清理编译失败的残留文件...
    if exist build_ext rmdir /s /q build_ext > nul 2>&1
    if exist build rmdir /s /q build > nul 2>&1
    for %%f in (*.pyd) do del "%%f" > nul 2>&1
    
    echo 程序完全依赖C扩展模块，编译失败无法继续打包。
    echo.
    echo 请检查：
    echo 1. 是否安装了Microsoft Visual C++ Build Tools？
    echo 2. Python版本是否正确（需要Python 3.8+）？
    echo 3. 是否有编译错误提示？
    echo.
    echo 解决建议：
    echo 1. 安装Visual Studio Build Tools（包含C++编译器）
    echo 2. 安装Python开发包：pip install setuptools wheel
    echo.
    pause
    exit /b 1
)

rem 检查是否生成了.pyd文件
set C_EXT=
for %%f in (*.pyd) do (
    if "%%f" NEQ "" set C_EXT=%%f
)

if not defined C_EXT (
    echo.
    rem 清理编译失败的残留文件
    if exist build_ext rmdir /s /q build_ext > nul 2>&1
    if exist build rmdir /s /q build > nul 2>&1
    
    echo ❌ 错误：未找到编译好的C扩展文件（.pyd）！
    echo 尽管编译命令成功，但未生成有效的扩展模块。
    echo.
    pause
    exit /b 1
)

rem 清理编译产生的build目录（保留.pyd文件）
echo 清理编译产生的临时文件...
if exist build_ext rmdir /s /q build_ext > nul 2>&1
if exist build rmdir /s /q build > nul 2>&1

echo C扩展编译成功：%C_EXT%

echo.
echo [3/4] 正在安装PyInstaller打包工具...
pip install pyinstaller > nul 2>&1
if errorlevel 1 (
    echo 警告：PyInstaller安装可能失败
    echo 正在尝试重新安装...
    pip install pyinstaller
) else (
    echo PyInstaller安装完成
)

echo.
echo [4/4] 正在构建EXE文件...
echo 这个过程可能需要2-5分钟，请稍候...

rem 清理之前的打包文件
if exist build rmdir /s /q build > nul 2>&1
if exist dist rmdir /s /q dist > nul 2>&1

rem 构建PyInstaller命令
echo 正在打包，包含C扩展：%C_EXT%
pyinstaller --onefile --windowed --name "橘雪莉表情包生成器" --clean ^
  --hidden-import=win32clipboard --hidden-import=win32gui ^
  --hidden-import=win32process --hidden-import=win32con ^
  --hidden-import=PIL._tkinter_finder --hidden-import=pyperclip ^
  --hidden-import=psutil --hidden-import=keyboard ^
  --add-data "*.py;." ^
  --add-binary "%C_EXT%;." ^
  main.py

if errorlevel 1 (
    echo.
    echo ❌ 错误：EXE构建失败！
    echo 请检查上方的错误信息。
    
    rem 清理失败的打包文件
    if exist build rmdir /s /q build > nul 2>&1
    if exist dist rmdir /s /q dist > nul 2>&1
    
    pause
    exit /b 1
)

echo.
echo ============================================
echo ✅ 构建成功！
echo ============================================
echo.
echo 生成的文件：dist\橘雪莉表情包生成器.exe
echo.
echo  程序功能说明：
echo 1. 经典模式：手动创建表情包
echo    - 输入文字、选择样式、保存图片
echo.
echo 2. 监听模式（依赖C扩展）：
echo    - 在微信、QQ聊天框中自动工作
echo    - 输入文字后按热键自动生成并发送表情包
echo    - 热键：Enter 或 Ctrl+Enter
echo.
echo  重要注意事项：
echo 1. 必须使用管理员权限运行程序
echo 2. 程序会自动创建必要文件夹：
echo    - background_images (存放背景图片)
echo    - Font (存放字体文件)
echo    - output_images (保存生成的图片)
echo 3. 请添加您的资源文件到相应文件夹
echo 4. 首次运行可能被安全软件拦截
echo.
echo  C扩展验证：
echo   ✅ C扩展已成功编译并打包：%C_EXT%
echo   监听模式功能可用
echo ============================================
echo.
echo 正在打开输出目录...
timeout /t 2 > nul
if exist dist explorer dist
pause