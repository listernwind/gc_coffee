#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
GC Coffee 图标库生成器（PNG 版）
设计稿：24x24 线性描边 SVG，渲染为 96px PNG（2x 高清），输出 base64 data-URI 样式库：
  miniprogram/assets/icons.wxss  （微信小程序，rpx 单位；WXSS 官方支持 PNG base64）
  preview/icons.css              （浏览器预览页，px 单位）
依赖：macOS 自带 qlmanage（SVG 渲染）。缓存于 tools/icon-cache/，重复运行秒级。
用法：python3 tools/gen_icons.py
"""
import base64
import os
import subprocess
import sys

ICONS = {
    "home": 'M4 10.5 L12 4 L20 10.5 M5.5 9.5 V20 H18.5 V9.5 M9.5 20 V13.5 H14.5 V20',
    "cup": 'M4.5 7.5 H17 V13 A5 5 0 0 1 12 18 H9.5 A5 5 0 0 1 4.5 13 Z M17 9.5 H18.8 A2.7 2.7 0 0 1 21.5 12.2 V13 M8.7 2.8 V5.2 M12 2.2 V5.2 M15.3 2.8 V5.2',
    "bottle": 'M10 2.5 H14 M10 4.5 H14 V9 M9.5 9 H14.5 V16.5 A3.5 3.5 0 0 1 11 20 H13 A3.5 3.5 0 0 1 9.5 16.5 Z M10.8 12.5 H13.2',
    "gem": 'M7 3.5 H17 L20.5 9 L12 20.5 L3.5 9 Z M3.5 9 H20.5 M8.8 9 L12 20.5 L15.2 9',
    "user": 'M12 11 A3.6 3.6 0 1 0 12 3.8 A3.6 3.6 0 0 0 12 11 Z M5 20 C5 16.2 8.1 13.8 12 13.8 C15.9 13.8 19 16.2 19 20',
    "users": 'M9.5 10.5 A3.2 3.2 0 1 0 9.5 4.1 A3.2 3.2 0 0 0 9.5 10.5 Z M3.8 19.5 C3.8 16 6.3 13.8 9.5 13.8 C12.7 13.8 15.2 16 15.2 19.5 M16.5 4.5 A3.2 3.2 0 0 1 16.5 10.9 M17.8 13.9 C19.9 14.2 20.2 16.4 20.2 19.5',
    "chart": 'M3.5 20.5 H20.5 M6.5 20.5 V12.5 M12 20.5 V6.5 M17.5 20.5 V9.5',
    "box": 'M4 7.5 L12 3.5 L20 7.5 L12 11.5 Z M4 7.5 V16.8 L12 20.5 L20 16.8 V7.5 M12 11.5 V20.5',
    "gift": 'M3.5 8.5 H20.5 V12.5 H3.5 Z M5 12.5 V19.5 H19 V12.5 M12 8.5 V19.5 M12 8.5 C10 6.5 6.8 7.8 5.2 8.5 C8.6 8.2 12 8.5 12 8.5 C12 8.5 15.4 8.2 18.8 8.5 C17.2 7.8 14 6.5 12 8.5',
    "ticket": 'M3.5 6.5 H20.5 V17.5 H3.5 Z M3.5 12 A1.8 1.8 0 0 0 3.5 12 M14.5 6.5 V17.5 M20.5 12 A1.8 1.8 0 0 1 20.5 12',
    "receipt": 'M6 3.5 H18 V20.5 H6 Z M9 7.5 H15 M9 11 H15 M9 14.5 H13',
    "truck": 'M2.5 6.5 H13.5 V16.5 H2.5 Z M13.5 10 H17.5 L20.5 13.2 V16.5 H13.5 M6 18.8 A2.2 2.2 0 1 0 6 19 M16.8 18.8 A2.2 2.2 0 1 0 16.8 19',
    "wallet": 'M2.5 6.5 H19.5 A2 2 0 0 1 21.5 8.5 V17.5 A2 2 0 0 1 19.5 19.5 H2.5 Z M2.5 9.8 H21.5 M16 14 H18.5',
    "card": 'M2.5 6 H21.5 A1.5 1.5 0 0 1 21.5 9 V18 A1.5 1.5 0 0 1 20 19.5 H4 A1.5 1.5 0 0 1 2.5 18 Z M2.5 9.5 H21.5 M6 15 H9.5',
    "star": 'M12 3.8 L14.6 8.9 L20.3 9.7 L16.2 13.6 L17.2 19.2 L12 16.6 L6.8 19.2 L7.8 13.6 L3.7 9.7 L9.4 8.9 Z',
    "sliders": 'M3.5 7 H20.5 M10 7 A2 2 0 1 0 10 6.9 M3.5 12 H20.5 M15 12 A2 2 0 1 0 15 11.9 M3.5 17 H20.5 M8 17 A2 2 0 1 0 8 16.9',
    "book": 'M4 4.5 H9.8 C11 4.5 12 5.4 12 6.8 V19.5 C12 18.1 11 17.2 9.8 17.2 H4 Z M20 4.5 H14.2 C13 4.5 12 5.4 12 6.8 V19.5 C12 18.1 13 17.2 14.2 17.2 H20 Z',
    "cart": 'M2.5 4 H5.2 L7.4 12.5 H17.2 L20 6.5 H6.6 M9 16.5 A1.8 1.8 0 1 0 9 16.6 M16.6 16.5 A1.8 1.8 0 1 0 16.6 16.6',
    "megaphone": 'M3.5 10 V14.5 L13.5 18 V6 L3.5 10 Z M13.5 8.5 C16.5 9 18.5 10.5 18.5 12.2 C18.5 13.9 16.5 15.4 13.5 15.9 M7 14.5 V17.5 M13.5 15.9 V18.5',
    "crown": 'M4 17.5 H20 M5 17.5 L6.8 8 L11.8 12.5 L16.8 8 L18.6 17.5 M5.8 8 L3.5 11 L6.8 12.5 M18 8 L20.3 11 L17 12.5',
    "clock": 'M12 20.5 A8.5 8.5 0 1 0 12 3.5 A8.5 8.5 0 0 0 12 20.5 Z M12 7.5 V12.2 L15.2 14',
    "bell": 'M12 4 C9.2 4 6.8 6.2 6.8 9.3 C6.8 12.8 5.8 14.3 5.8 15.8 H18.2 C18.2 14.3 17.2 12.8 17.2 9.3 C17.2 6.2 14.8 4 12 4 Z M10 18.6 A2.1 2.1 0 0 0 14 18.6',
    "phone": 'M6.5 3.5 H9.2 L10.6 8.2 L8.4 9.7 A13.5 13.5 0 0 0 14.3 15.6 L15.8 13.4 L20.5 14.8 V17.5 A2 2 0 0 1 18.5 19.5 C10 19.5 4.5 14 4.5 5.5 A2 2 0 0 1 6.5 3.5 Z',
    "wechat": 'M12 3.5 C6.8 3.5 2.5 7 2.5 11.5 C2.5 13.6 3.8 15.4 5.7 16.6 L5.2 19.5 L8.6 18.1 C9.6 18.4 10.8 18.6 12 18.6 C17.2 18.6 21.5 15.1 21.5 11 C21.5 7 17.2 3.5 12 3.5 Z M8.5 10.2 A0.9 0.9 0 1 0 8.5 10.3 M15.5 10.2 A0.9 0.9 0 1 0 15.5 10.3',
    "image": 'M3.5 5.5 H20.5 A1.5 1.5 0 0 1 20.5 8.5 V18.5 A1.5 1.5 0 0 1 19 20 H5 A1.5 1.5 0 0 1 3.5 18.5 Z M8.8 10 A1.6 1.6 0 1 0 8.8 10.1 M6.5 17.5 L11 13 L13.8 15.5 L16.6 12.8 L20 15.8',
    "check": 'M5 12.5 L10 17.5 L19 6.5',
    "lightbulb": 'M12 3.8 A6.2 6.2 0 0 0 5.8 10 C5.8 12.6 7.7 13.9 8.2 16 H15.8 C16.3 13.9 18.2 12.6 18.2 10 A6.2 6.2 0 0 0 12 3.8 Z M10.2 19.5 H13.8 M11 21.5 H13',
}

COLORS = {
    "": "#6F4E37",   # 默认：咖啡棕
    "-w": "#FFF7EC", # 白色（深色/渐变底）
    "-m": "#C9B8A5", # 弱化（tab 未选中）
    "-g": "#C08A4E", # 金色（强调）
}

# 必须用 <path d="..."/> 包裹路径数据（裸写路径是非法 SVG，任何渲染器都不会绘制）
TPL = ('<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 24 24" fill="none" '
       'stroke="{c}" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="{p}"/></svg>')

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
CACHE = os.path.join(HERE, "icon-cache-v2")


def render_batch(items):
    """每个 SVG 作为顶层文档交给 Chrome 无头导航截图（页面内 SVG 渲染不可靠）"""
    os.makedirs(CACHE, exist_ok=True)
    missing = [it for it in items if not os.path.exists(os.path.join(CACHE, it[0] + ".png"))]
    if missing:
        import json as _json
        list_path = os.path.join(CACHE, "_batch.json")
        with open(list_path, 'w', encoding='utf-8') as f:
            _json.dump([{'name': n, 'svg': s} for n, s in missing], f, ensure_ascii=False)
        node_script = os.path.join(ROOT, 'miniprogram', 'test', 'tools', 'render-icons.js')
        subprocess.run(['node', node_script, list_path, CACHE], check=True)
    result = {}
    for name, _ in items:
        p = os.path.join(CACHE, name + '.png')
        if not os.path.exists(p):
            raise RuntimeError(f'渲染失败: {name}')
        result[name] = open(p, 'rb').read()
    return result


def b64_png(png: bytes) -> str:
    return "data:image/png;base64," + base64.b64encode(png).decode()


def gen_css(png_map) -> str:
    lines = [
        "/* GC Coffee 线性图标库（PNG，自动生成，勿手改） */",
        ".ic{display:inline-block;width:40rpx;height:40rpx;background-repeat:no-repeat;background-position:center;background-size:contain;}",
    ]
    for name in png_map:
        base = name[:-2] if name.endswith('-d') else name
        suffix = '' if name.endswith('-d') else name[len(base):]
        cls = f"ic-{base}{suffix}"
        lines.append(f".{cls}{{background-image:url('{b64_png(png_map[name])}');}}")
    return "\n".join(lines) + "\n"


def main():
    # 1. Chrome 渲染全部图标 PNG
    items = []
    for name, path in ICONS.items():
        for suffix, color in COLORS.items():
            items.append((f"{name}{suffix or '-d'}", TPL.format(c=color, p=path)))
    png_map = render_batch(items)

    # 2. PNG 文件：小程序 <image src> 引用
    icon_dir = os.path.join(ROOT, "miniprogram", "assets", "icons")
    os.makedirs(icon_dir, exist_ok=True)
    for key, png in png_map.items():
        base = key[:-2] if key.endswith('-d') else key
        suffix = '' if key.endswith('-d') else key[len(base):]
        with open(os.path.join(icon_dir, f"{base}{suffix}.png"), "wb") as f:
            f.write(png)

    # 3. 小程序 icons.wxss：仅 .ic 基础类（图标由 image 文件承载）
    with open(os.path.join(ROOT, "miniprogram", "assets", "icons.wxss"), "w", encoding="utf-8") as f:
        f.write(".ic{display:inline-block;width:40rpx;height:40rpx;}\n")

    # 4. 浏览器预览：data-URI CSS（Chrome 渲染的正确图标）
    css = gen_css(png_map).replace("40rpx", "18px")
    os.makedirs(os.path.join(ROOT, "preview"), exist_ok=True)
    with open(os.path.join(ROOT, "preview", "icons.css"), "w", encoding="utf-8") as f:
        f.write(css)

    print(f"generated: {len(png_map)} PNG 图标 + icons.wxss + preview/icons.css")


if __name__ == "__main__":
    main()
