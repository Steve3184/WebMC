#!/usr/bin/env python3
"""Fast parallel patch generator"""
import os
import subprocess
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed
import json
import sys

# Paths
SCRIPT_DIR = Path(__file__).parent
ROOT_DIR = SCRIPT_DIR.parent
WORK_DIR = ROOT_DIR / "work"
UPSTREAM_DIR = ROOT_DIR / "upstream"
PATCHES_DIR = ROOT_DIR / "patches"
MC_SOURCE_REL = "src/main/java"

# Addon prefixes to skip
ADDON_PREFIXES = (
    "com/mojang/blaze3d/",
    "org/lwjgl/",
    "top/steve3184/webmc/",
)

def should_skip(rel_path: str) -> bool:
    """Check if file is addon-owned and should be skipped"""
    return any(rel_path.startswith(p) for p in ADDON_PREFIXES)

def process_modified_file(args):
    """Process a single modified file"""
    f, upstream_f, rel = args
    try:
        if not should_skip(rel):
            patch_path = PATCHES_DIR / f"{rel}.patch"
            patch_path.parent.mkdir(parents=True, exist_ok=True)
            result = subprocess.run(
                ["diff", "-u", f"--label=a/{rel}", f"--label=b/{rel}", str(upstream_f), str(f)],
                capture_output=True, text=True
            )
            if result.stdout:
                patch_path.write_text(result.stdout)
                return "modified", rel
    except Exception as e:
        pass
    return None

def process_new_file(args):
    """Process a new file (not in upstream)"""
    f, rel = args
    try:
        if not should_skip(rel):
            patch_path = PATCHES_DIR / f"{rel}.patch"
            patch_path.parent.mkdir(parents=True, exist_ok=True)
            result = subprocess.run(
                ["diff", "-u", f"--label=a/{rel}", f"--label=b/{rel}", "/dev/null", str(f)],
                capture_output=True, text=True
            )
            if result.stdout:
                patch_path.write_text(result.stdout)
                return "new", rel
    except Exception as e:
        pass
    return None

def process_deleted_file(args):
    """Process a deleted file"""
    f, rel = args
    try:
        if not should_skip(rel):
            patch_path = PATCHES_DIR / f"{rel}.patch"
            patch_path.parent.mkdir(parents=True, exist_ok=True)
            result = subprocess.run(
                ["diff", "-u", f"--label=a/{rel}", f"--label=b/{rel}", str(f), "/dev/null"],
                capture_output=True, text=True
            )
            if result.stdout:
                patch_path.write_text(result.stdout)
                return "deleted", rel
    except Exception as e:
        pass
    return None

def main():
    work_src = WORK_DIR / MC_SOURCE_REL
    upstream_src = UPSTREAM_DIR / MC_SOURCE_REL

    if not work_src.exists():
        print(f"ERROR: {work_src} not found. Run setup first.")
        sys.exit(1)

    # Clean patches dir
    print("[36m[wiping patches/][0m")
    import shutil
    if PATCHES_DIR.exists():
        shutil.rmtree(PATCHES_DIR)
    PATCHES_DIR.mkdir(parents=True)

    modified_tasks = []
    new_tasks = []
    deleted_tasks = []

    print("[36m[scanning work/src/][0m")
    for f in work_src.rglob("*.java"):
        rel = str(f.relative_to(work_src))
        upstream_f = upstream_src / rel
        if upstream_f.exists():
            modified_tasks.append((f, upstream_f, rel))
        else:
            new_tasks.append((f, rel))

    print("[36m[scanning upstream/src/][0m")
    for f in upstream_src.rglob("*.java"):
        rel = str(f.relative_to(upstream_src))
        work_f = work_src / rel
        if not work_f.exists():
            deleted_tasks.append((f, rel))

    total = len(modified_tasks) + len(new_tasks) + len(deleted_tasks)
    print(f"Found: {len(modified_tasks)} modified, {len(new_tasks)} new, {len(deleted_tasks)} deleted")

    modified_count = 0
    new_count = 0
    deleted_count = 0
    processed = 0

    # Process with thread pool
    all_tasks = (
        [(t, "modified") for t in modified_tasks] +
        [(t, "new") for t in new_tasks] +
        [(t, "deleted") for t in deleted_tasks]
    )

    with ThreadPoolExecutor(max_workers=8) as executor:
        futures = []
        for task, task_type in all_tasks:
            if task_type == "modified":
                futures.append(executor.submit(process_modified_file, task))
            elif task_type == "new":
                futures.append(executor.submit(process_new_file, task))
            else:
                futures.append(executor.submit(process_deleted_file, task))

        for future in as_completed(futures):
            result = future.result()
            if result:
                _, rel = result
                if "modified" in str(result):
                    modified_count += 1
                elif "new" in str(result):
                    new_count += 1
                else:
                    deleted_count += 1
            processed += 1
            if processed % 200 == 0:
                print(f"  Processed {processed}/{total} files...")

    total_generated = modified_count + new_count
    print(f"[32m[OK: generated {total_generated} patch(es), {deleted_count} deletion(s)][0m")

if __name__ == "__main__":
    main()
