#!/usr/bin/env python3
"""Fetch vanilla MC 1.21.8 client.jar + assets and pack them into dist/mc-vfs.bin.

The blob format (binary, little-endian) matches what
top.steve3184.webmc.vfs.WebFs.parseAndApply() expects:

    magic    "MCVF"     (4 bytes)
    version  uint32     (1)
    count    uint32     number of entries
    entries:
      pathLen  uint16   length of the UTF-8 path bytes
      path     UTF-8    leading slash optional
      dataLen  uint32
      data     raw bytes of the file

The browser fetches the blob synchronously at startup and unpacks it into
TeaVM's InMemoryVirtualFileSystem. From there every {@code File}/{@code Path}
operation in MC sees a real, pre-populated filesystem rooted at /.

What we ship:
  * /assets/.mcassetsroot, /data/.mcassetsroot — marker files for
    VanillaPackResourcesBuilder.
  * Everything under /assets/ and /data/ inside client.jar (jar internal
    layout already maps to those paths).
  * Optionally: external assets indexed by the version's asset-index
    (sounds, lang). Only downloaded if --with-objects is passed; off by
    default to keep dist/mc-vfs.bin small for iteration.

Usage:
    scripts/fetch-assets.py                 # client.jar contents only
    scripts/fetch-assets.py --with-objects  # plus indexed objects
    scripts/fetch-assets.py --version 1.21.8 --out dist/mc-vfs.bin
"""
from __future__ import annotations
import argparse
import io
import json
import os
import struct
import sys
import urllib.request
import zipfile
from pathlib import Path

VERSION_MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"
DEFAULT_VERSION = "1.21.8"
PROJECT_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_OUT = PROJECT_ROOT / "dist" / "mc-vfs.bin"
CACHE_DIR = PROJECT_ROOT / "work" / "mc-vfs-cache"


def fetch(url: str, dest: Path | None = None) -> bytes:
    """GET a URL, optionally caching to disk. Returns bytes."""
    if dest is not None and dest.exists():
        return dest.read_bytes()
    print(f"  fetch {url}")
    max_retries = 5
    for i in range(max_retries):
        try:
            with urllib.request.urlopen(url, timeout=60) as r:
                body = r.read()
            if dest is not None:
                dest.parent.mkdir(parents=True, exist_ok=True)
                dest.write_bytes(body)
            return body
        except Exception as e:
            if i < max_retries - 1:
                print(f"    retry {i+1}/{max_retries} after error: {e}")
            else:
                raise


def find_version_url(version_id: str) -> str:
    manifest = json.loads(fetch(VERSION_MANIFEST).decode("utf-8"))
    for v in manifest["versions"]:
        if v["id"] == version_id:
            return v["url"]
    raise SystemExit(f"version {version_id} not in manifest")


def collect_jar_entries(jar_bytes: bytes) -> dict[str, bytes]:
    """Return /assets/** and /data/** entries from client.jar as path -> bytes."""
    out: dict[str, bytes] = {}
    with zipfile.ZipFile(io.BytesIO(jar_bytes)) as zf:
        for name in zf.namelist():
            if name.endswith("/"):
                continue
            if name.startswith("assets/") or name.startswith("data/"):
                out["/" + name] = zf.read(name)
    # synthesise marker files in case they aren't there
    out.setdefault("/assets/.mcassetsroot", b"")
    out.setdefault("/data/.mcassetsroot", b"")
    return out


def collect_indexed_objects(version_json: dict) -> dict[str, bytes]:
    """Download the asset index referenced by the version json and add files
    under /assets/objects/<hash[:2]>/<hash> plus a manifest."""
    asset_index = version_json["assetIndex"]
    index_url = asset_index["url"]
    index_id = asset_index["id"]
    index_bytes = fetch(index_url, CACHE_DIR / "indexes" / f"{index_id}.json")
    out: dict[str, bytes] = {f"/assets/indexes/{index_id}.json": index_bytes}
    objects = json.loads(index_bytes.decode("utf-8"))["objects"]
    print(f"  asset-index {index_id}: {len(objects)} objects")
    for i, (logical, info) in enumerate(objects.items()):
        h = info["hash"]
        rel = f"{h[:2]}/{h}"
        url = f"https://resources.download.minecraft.net/{rel}"
        cache = CACHE_DIR / "objects" / rel
        body = fetch(url, cache)
        out[f"/assets/objects/{rel}"] = body
        if (i + 1) % 100 == 0:
            print(f"    [{i + 1}/{len(objects)}] {logical}")
    return out


def pack_blob(entries: dict[str, bytes]) -> bytes:
    buf = io.BytesIO()
    buf.write(b"MCVF")
    buf.write(struct.pack("<I", 1))                   # version
    buf.write(struct.pack("<I", len(entries)))        # count
    for path, data in entries.items():
        path_b = path.encode("utf-8")
        if len(path_b) > 0xFFFF:
            raise ValueError(f"path too long: {path}")
        buf.write(struct.pack("<H", len(path_b)))
        buf.write(path_b)
        buf.write(struct.pack("<I", len(data)))
        buf.write(data)
    return buf.getvalue()


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--version", default=DEFAULT_VERSION,
                    help=f"MC version id (default {DEFAULT_VERSION})")
    ap.add_argument("--with-objects", action="store_true",
                    help="also download indexed asset objects (sounds/lang)")
    ap.add_argument("--out", type=Path, default=DEFAULT_OUT,
                    help=f"output path (default {DEFAULT_OUT.relative_to(PROJECT_ROOT)})")
    args = ap.parse_args()

    print(f"finding version metadata for {args.version}...")
    vurl = find_version_url(args.version)
    vbytes = fetch(vurl, CACHE_DIR / "versions" / f"{args.version}.json")
    vjson = json.loads(vbytes.decode("utf-8"))

    print("downloading client.jar...")
    client_url = vjson["downloads"]["client"]["url"]
    client_bytes = fetch(client_url, CACHE_DIR / "jars" / f"{args.version}-client.jar")

    print("extracting /assets and /data from client.jar...")
    entries = collect_jar_entries(client_bytes)
    print(f"  {len(entries)} entries from jar")

    if args.with_objects:
        print("downloading indexed objects (this may take a while on first run)...")
        entries.update(collect_indexed_objects(vjson))
        print(f"  total entries: {len(entries)}")

    print(f"packing into {args.out}...")
    args.out.parent.mkdir(parents=True, exist_ok=True)
    blob = pack_blob(entries)
    args.out.write_bytes(blob)
    size_mib = len(blob) / (1024 * 1024)
    print(f"done. wrote {args.out} ({size_mib:.1f} MiB, {len(entries)} files)")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(130)
