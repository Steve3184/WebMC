"""
SD 1.5 模型下载脚本 - 支持断点续传
用法: python download_sd15.py
可随时 Ctrl+C 中断，再次运行会从断点继续
"""
import os
import sys
import time

# ============ 配置 ============
# 下载地址（多个镜像源，按顺序尝试）
DOWNLOAD_URLS = [
    # ModelScope 直链（国内首选）
    "https://modelscope.cn/models/AI-ModelScope/stable-diffusion-v1-5/resolve/master/v1-5-pruned-emaonly.safetensors",
    # HuggingFace 镜像
    "https://hf-mirror.com/runwayml/stable-diffusion-v1-5/resolve/main/v1-5-pruned-emaonly.safetensors",
    # HuggingFace 官方（需翻墙）
    "https://huggingface.co/runwayml/stable-diffusion-v1-5/resolve/main/v1-5-pruned-emaonly.safetensors",
]

# 保存路径
SAVE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                        "stable-diffusion-webui", "models", "Stable-diffusion")
SAVE_NAME = "v1-5-pruned-emaonly.safetensors"
# ==============================


def download_with_progress(url, save_path):
    """支持断点续传的下载"""
    import urllib.request

    # 检查已下载的大小
    downloaded = 0
    if os.path.exists(save_path):
        downloaded = os.path.getsize(save_path)
        print(f"发现已有文件 {downloaded / (1024**3):.2f} GB, 尝试断点续传...")

    # 先获取文件总大小
    total_size = 0
    try:
        req = urllib.request.Request(url, method='HEAD')
        with urllib.request.urlopen(req, timeout=15) as resp:
            total_size = int(resp.headers.get('content-length', 0))
            print(f"文件总大小: {total_size / (1024**3):.2f} GB")
    except Exception as e:
        print(f"无法获取文件大小: {e}")

    if downloaded > 0 and total_size > 0 and downloaded >= total_size:
        print("[OK] 文件已下载完成!")
        return True

    # 断点续传
    headers = {}
    if downloaded > 0:
        headers['Range'] = f'bytes={downloaded}-'

    req = urllib.request.Request(url, headers=headers)

    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            mode = 'ab' if downloaded > 0 else 'wb'
            with open(save_path, mode) as f:
                chunk_size = 1024 * 512  # 512KB
                last_print = 0
                start_time = time.time()
                last_downloaded = downloaded
                while True:
                    chunk = resp.read(chunk_size)
                    if not chunk:
                        break
                    f.write(chunk)
                    downloaded += len(chunk)

                    # 每秒打印一次进度
                    now = time.time()
                    if now - last_print >= 2.0:
                        elapsed = now - last_print
                        speed = (downloaded - last_downloaded) / elapsed / (1024**2)
                        last_print = now
                        last_downloaded = downloaded
                        if total_size > 0:
                            pct = downloaded / total_size * 100
                            eta = (total_size - downloaded) / (speed * 1024**2) if speed > 0 else 0
                            print(f"\r进度: {downloaded/(1024**3):.2f}/{total_size/(1024**3):.2f} GB ({pct:.1f}%) 速度: {speed:.1f} MB/s 剩余: {eta:.0f}s   ", end="", flush=True)
                        else:
                            print(f"\r已下载: {downloaded/(1024**3):.2f} GB 速度: {speed:.1f} MB/s   ", end="", flush=True)

        print(f"\n[OK] 下载完成: {save_path}")
        final_size = os.path.getsize(save_path)
        print(f"   文件大小: {final_size / (1024**3):.2f} GB")
        return True

    except KeyboardInterrupt:
        print(f"\n[PAUSE] 下载已暂停, 已下载 {downloaded/(1024**3):.2f} GB")
        print("   再次运行此脚本将从断点继续下载")
        return False
    except Exception as e:
        print(f"\n[ERROR] 下载出错: {e}")
        if downloaded > 0:
            print(f"   已下载 {downloaded/(1024**3):.2f} GB, 再次运行将尝试续传或换源")
        return False


def main():
    save_path = os.path.join(SAVE_DIR, SAVE_NAME)
    os.makedirs(SAVE_DIR, exist_ok=True)

    # 如果已经下载完成，直接退出
    if os.path.exists(save_path):
        size = os.path.getsize(save_path)
        if size > 4 * 1024**3:  # > 4GB 认为完整
            print(f"[OK] SD 1.5 模型已存在: {save_path}")
            print(f"   文件大小: {size/(1024**3):.2f} GB")
            return

    print("=" * 60)
    print("  Stable Diffusion 1.5 模型下载")
    print("  支持断点续传 - 可随时 Ctrl+C 暂停")
    print("=" * 60)
    print(f"\n保存路径: {save_path}\n")

    for i, url in enumerate(DOWNLOAD_URLS):
        source_name = ["ModelScope (国内首选)", "HuggingFace 镜像", "HuggingFace 官方"][i]
        print(f"\n>> 尝试源 {i+1}/{len(DOWNLOAD_URLS)}: {source_name}")
        print(f"   URL: {url}\n")

        success = download_with_progress(url, save_path)
        if success:
            # 验证文件
            size = os.path.getsize(save_path)
            if size > 4 * 1024**3:
                print(f"\n[OK] SD 1.5 模型下载成功!")
                print(f"   路径: {save_path}")
                print(f"   大小: {size/(1024**3):.2f} GB")
                print(f"\n现在可以启动 SD-WebUI 了!")
                return
            else:
                print(f"[WARN] 文件可能不完整 ({size/(1024**3):.2f} GB), 尝试下一个源...")
                os.remove(save_path)
        else:
            print(f"   源 {i+1} 失败, 尝试下一个...")

    print("\n[FAIL] 所有下载源均失败。请手动下载:")
    print(f"   1. 浏览器打开: {DOWNLOAD_URLS[0]}")
    print(f"   2. 保存到: {save_path}")
    print(f"   或使用迅雷/IDM等下载工具加速")


if __name__ == "__main__":
    main()
