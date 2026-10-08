"""Replace structure entries with Zopfli DEFLATE, preserving other ZIP records.

Uses public zipfile readers and the ZIP32 layout from PKWARE's APPNOTE. The
dependency wheel, pip cache and compressed streams live under this project's
build directory; no global Python installation is modified.
"""
import argparse
import hashlib
import importlib
import importlib.metadata
import io
import os
import platform
import shutil
import struct
import subprocess
import sys
import tempfile
import zipfile
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / "build"
REQUIREMENTS = Path(__file__).with_name("requirements-structure-tools.txt")
STRUCTURE_ROOT = "assets/gtladditions/structures/"
MAX_U32 = 0xFFFFFFFF


def owned_directory(path: Path) -> Path:
    path = path.resolve()
    if path == BUILD.resolve() or not path.is_relative_to(BUILD.resolve()):
        raise ValueError(f"Tool dependencies and pip cache must be inside {BUILD}: {path}")
    return path


def remove_owned_tree(path: Path) -> None:
    # Only clean directories created for this build tool, after checking the path.
    shutil.rmtree(owned_directory(path))


def load_compressor(dependency_dir: Path, pip_cache: Path):
    if sys.version_info < (3, 10):
        raise RuntimeError("Structure compression requires Python 3.10 or newer")
    dependency_dir = owned_directory(dependency_dir)
    pip_cache = owned_directory(pip_cache)
    requirement = REQUIREMENTS.read_text(encoding="ascii").strip()
    name, version = requirement.split("==")
    if name != "zopfli" or "\n" in version:
        raise ValueError("Expected one pinned zopfli requirement")
    fingerprint = hashlib.sha256(
        f"{requirement}/{sys.implementation.cache_tag}/{platform.system()}/{platform.machine()}".encode()
    ).hexdigest()[:16]
    target = dependency_dir / fingerprint
    check_code = (
        "import sys,pathlib,importlib.metadata; sys.path.insert(0,sys.argv[1]); "
        "import zopfli.zlib; "
        "assert pathlib.Path(zopfli.zlib.__file__).resolve().is_relative_to(pathlib.Path(sys.argv[1]).resolve()); "
        "assert importlib.metadata.version('zopfli') == sys.argv[2]"
    )

    def usable(path: Path) -> bool:
        if not path.is_dir():
            return False
        return subprocess.run(
            [sys.executable, "-B", "-c", check_code, str(path), version],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL
        ).returncode == 0

    if not usable(target):
        dependency_dir.mkdir(parents=True, exist_ok=True)
        pip_cache.mkdir(parents=True, exist_ok=True)
        temporary = Path(tempfile.mkdtemp(prefix=".zopfli-install-", dir=dependency_dir))
        try:
            pip_temporary = owned_directory(pip_cache.parent / "pip-temp")
            pip_temporary.mkdir(parents=True, exist_ok=True)
            pip_environment = os.environ.copy()
            for variable in ("TMP", "TEMP", "TMPDIR"):
                pip_environment[variable] = str(pip_temporary)
            subprocess.run([
                sys.executable, "-B", "-m", "pip", "install", "--disable-pip-version-check",
                "--only-binary=:all:", "--no-deps", "--no-compile",
                "--cache-dir", str(pip_cache), "--target", str(temporary), "-r", str(REQUIREMENTS)
            ], check=True, env=pip_environment)
            if not usable(temporary):
                raise RuntimeError("The installed Zopfli wheel could not be loaded by this Python")
            if target.exists():
                remove_owned_tree(target)
            os.replace(temporary, target)
        finally:
            if temporary.exists():
                remove_owned_tree(temporary)
    sys.path.insert(0, str(target))
    module = importlib.import_module("zopfli.zlib")
    if not Path(module.__file__).resolve().is_relative_to(target):
        raise RuntimeError("Zopfli was imported from outside the project dependency directory")
    return module.compress, version


def end_record(data: bytes) -> tuple[int, int, int, int]:
    lower = max(0, len(data) - 22 - 0xFFFF)
    position = data.rfind(b"PK\x05\x06", lower)
    while position >= lower:
        if position + 22 <= len(data):
            fields = struct.unpack_from("<4s4H2LH", data, position)
            if position + 22 + fields[7] == len(data):
                break
        position = data.rfind(b"PK\x05\x06", lower, position)
    else:
        raise ValueError("ZIP end-of-central-directory record is missing")
    _, disk, central_disk, disk_count, count, size, offset, _ = fields
    if disk or central_disk or disk_count != count:
        raise ValueError("Multi-volume JARs are not supported")
    if count == 0xFFFF or size == MAX_U32 or offset == MAX_U32 or data[position - 20:position - 16] == b"PK\x06\x07":
        raise ValueError("ZIP64 JARs are not supported by this build tool")
    central_start = position - size
    adjustment = central_start - offset
    if central_start < 0 or adjustment < 0:
        raise ValueError("Invalid ZIP central-directory position")
    return position, central_start, count, adjustment


def open_zip(data: bytes) -> zipfile.ZipFile:
    end, _, _, _ = end_record(data)
    comment = data[end + 22:]
    # Python 3.10's reader mistakes an EOCD signature inside a comment for the
    # real footer. Use a comment-free reading view and retain the actual metadata.
    view = data[:end + 20] + b"\0\0" if b"PK\x05\x06" in comment else data
    archive = zipfile.ZipFile(io.BytesIO(view))
    archive.comment = comment
    return archive


def parse_archive(data: bytes):
    end, central_start, count, adjustment = end_record(data)
    with open_zip(data) as archive:
        infos = archive.infolist()
    if len(infos) != count or len({info.filename for info in infos}) != count:
        raise ValueError("Invalid entry count or duplicate JAR entry names")
    central = {}
    position = central_start
    for info in infos:
        if data[position:position + 4] != b"PK\x01\x02" or position + 46 > end:
            raise ValueError("Invalid ZIP central-directory entry")
        name_len, extra_len, comment_len, disk = struct.unpack_from("<4H", data, position + 28)
        length = 46 + name_len + extra_len + comment_len
        record = data[position:position + length]
        sizes = struct.unpack_from("<2L", record, 20)
        offset = struct.unpack_from("<L", record, 42)[0]
        if len(record) != length or position + length > end or disk or MAX_U32 in (*sizes, offset):
            raise ValueError("Unsupported or truncated central-directory entry")
        if offset + adjustment != info.header_offset:
            raise ValueError("Inconsistent ZIP local-header position")
        central[info.filename] = record
        position += length
    if position != end:
        raise ValueError("Unsupported records after ZIP central directory")
    physical = sorted(infos, key=lambda info: info.header_offset)
    records = {}
    for index, info in enumerate(physical):
        start = info.header_offset
        stop = physical[index + 1].header_offset if index + 1 < len(physical) else central_start
        if start < adjustment or data[start:start + 4] != b"PK\x03\x04" or start + 30 > stop:
            raise ValueError("Invalid ZIP local header")
        flags, method = struct.unpack_from("<2H", data, start + 6)
        name_len, extra_len = struct.unpack_from("<2H", data, start + 26)
        payload_start = start + 30 + name_len + extra_len
        payload_end = payload_start + info.compress_size
        if flags != info.flag_bits or method != info.compress_type or payload_end > stop:
            raise ValueError("Inconsistent or overlapping ZIP entry")
        records[info.filename] = (start, payload_start, payload_end, stop)
    return infos, physical, central, records, end, adjustment


def is_structure(info: zipfile.ZipInfo) -> bool:
    return info.filename.startswith(STRUCTURE_ROOT) and info.filename.endswith(".bin")


def verify_deflate(expected: bytes, compressed: bytes) -> None:
    decoder = zlib.decompressobj(wbits=-15)
    actual = decoder.decompress(compressed, len(expected) + 1)
    if actual != expected or not decoder.eof or decoder.unused_data or decoder.unconsumed_tail:
        raise ValueError("Zopfli stream failed round-trip verification")


def compress_payload(payload: bytes, cache: Path, iterations: int, compressor, version: str) -> bytes:
    key = hashlib.sha256(f"py-zopfli-{version}/first/{iterations}/cache-v1/".encode() + payload).hexdigest()
    cached = cache / f"{key}.deflate"
    if cached.exists():
        compressed = cached.read_bytes()
        try:
            verify_deflate(payload, compressed)
            return compressed
        except (ValueError, zlib.error):
            cached.unlink()
    # The pinned binding emits RFC 1950 zlib framing: a two-byte header and Adler-32.
    compressed = compressor(payload, numiterations=iterations, blocksplitting=1, blocksplittinglast=0)[2:-4]
    verify_deflate(payload, compressed)
    fd, temporary = tempfile.mkstemp(prefix=".zopfli-", suffix=".tmp", dir=cache)
    try:
        with os.fdopen(fd, "wb") as stream:
            stream.write(compressed)
        os.replace(temporary, cached)
    finally:
        Path(temporary).unlink(missing_ok=True)
    return compressed


def rewrite_entry(data: bytes, info: zipfile.ZipInfo, record, compressed: bytes) -> bytes:
    start, payload_start, payload_end, stop = record
    header = bytearray(data[start:payload_start])
    if MAX_U32 in struct.unpack_from("<2L", header, 18):
        raise ValueError("ZIP64 structure entries are not supported")
    name_length, extra_length = struct.unpack_from("<2H", header, 26)
    extra = header[30 + name_length:30 + name_length + extra_length]
    position = 0
    while position < len(extra):
        if position + 4 > len(extra):
            raise ValueError("Truncated local ZIP extra field")
        tag, length = struct.unpack_from("<2H", extra, position)
        position += 4 + length
        if position > len(extra):
            raise ValueError("Truncated local ZIP extra field")
        if tag == 1:
            raise ValueError("ZIP64 structure entries are not supported")
    struct.pack_into("<H", header, 4, max(20, struct.unpack_from("<H", header, 4)[0]))
    struct.pack_into("<H", header, 8, zipfile.ZIP_DEFLATED)
    tail = data[payload_end:stop]
    if info.flag_bits & 8:
        signature_size = 4 if tail.startswith(b"PK\x07\x08") else 0
        if len(tail) < signature_size + 12 or struct.unpack_from("<3L", tail, signature_size) != (info.CRC, info.compress_size, info.file_size):
            raise ValueError("Invalid ZIP data descriptor")
        tail = tail[:signature_size] + struct.pack("<3L", info.CRC, len(compressed), info.file_size) + tail[signature_size + 12:]
        struct.pack_into("<3L", header, 14, 0, 0, 0)
    else:
        struct.pack_into("<3L", header, 14, info.CRC, len(compressed), info.file_size)
    return bytes(header) + compressed + tail


def verify_archive(source: bytes, candidate: bytes) -> None:
    old_infos, _, old_central, old_records, _, _ = parse_archive(source)
    new_infos, _, new_central, new_records, _, _ = parse_archive(candidate)
    if [info.filename for info in old_infos] != [info.filename for info in new_infos]:
        raise ValueError("JAR entries or their order changed")
    with open_zip(source) as old_zip, open_zip(candidate) as new_zip:
        if old_zip.comment != new_zip.comment:
            raise ValueError("JAR archive comment changed")
        for original, replacement in zip(old_infos, new_infos):
            if (original.CRC, original.file_size, original.date_time, original.comment, original.extra, original.flag_bits) != (
                replacement.CRC, replacement.file_size, replacement.date_time, replacement.comment, replacement.extra, replacement.flag_bits
            ):
                raise ValueError(f"JAR entry metadata changed: {original.filename}")
            if is_structure(original):
                if old_zip.read(original) != new_zip.read(replacement):
                    raise ValueError(f"Structure contents changed: {original.filename}")
            else:
                first = old_records[original.filename]
                second = new_records[replacement.filename]
                if source[first[0]:first[3]] != candidate[second[0]:second[3]]:
                    raise ValueError(f"Non-structure ZIP record changed: {original.filename}")
                old = bytearray(old_central[original.filename])
                new = bytearray(new_central[replacement.filename])
                old[42:46] = new[42:46] = b"\0" * 4
                if old != new:
                    raise ValueError(f"Non-structure central metadata changed: {original.filename}")
        bad = new_zip.testzip()
        if bad:
            raise ValueError(f"Final JAR failed CRC verification: {bad}")


def optimize(jar: Path, cache: Path, iterations: int, compressor, version: str) -> tuple[int, int, int]:
    if iterations < 1:
        raise ValueError("Zopfli iterations must be positive")
    source = jar.read_bytes()
    infos, physical, central, records, end, adjustment = parse_archive(source)
    structures = [info for info in infos if is_structure(info)]
    if not structures:
        raise ValueError(f"No structure resources found in {jar}")
    cache.mkdir(parents=True, exist_ok=True)
    replacements = {}
    before = sum(info.compress_size for info in structures)
    with open_zip(source) as original:
        for info in structures:
            if info.flag_bits & (1 | 64) or info.compress_type not in (zipfile.ZIP_STORED, zipfile.ZIP_DEFLATED):
                raise ValueError(f"Unsupported structure entry: {info.filename}")
            compressed = compress_payload(original.read(info), cache, iterations, compressor, version)
            if len(compressed) < info.compress_size:
                replacements[info.filename] = compressed
    after = sum(len(replacements[info.filename]) if info.filename in replacements else info.compress_size for info in structures)
    if replacements:
        output = bytearray(source[:physical[0].header_offset])
        offsets = {}
        for info in physical:
            offsets[info.filename] = len(output) - adjustment
            record = records[info.filename]
            if info.filename in replacements:
                output += rewrite_entry(source, info, record, replacements[info.filename])
            else:
                output += source[record[0]:record[3]]
        central_start = len(output)
        for info in infos:
            record = bytearray(central[info.filename])
            struct.pack_into("<L", record, 42, offsets[info.filename])
            if info.filename in replacements:
                struct.pack_into("<H", record, 6, max(20, struct.unpack_from("<H", record, 6)[0]))
                struct.pack_into("<H", record, 10, zipfile.ZIP_DEFLATED)
                struct.pack_into("<L", record, 20, len(replacements[info.filename]))
            output += record
        footer = bytearray(source[end:])
        struct.pack_into("<2L", footer, 12, len(output) - central_start, central_start - adjustment)
        output += footer
        candidate = bytes(output)
        verify_archive(source, candidate)
        fd, temporary = tempfile.mkstemp(prefix=".structure-zopfli-", suffix=".jar", dir=jar.resolve().parent)
        try:
            with os.fdopen(fd, "wb") as stream:
                stream.write(candidate)
            if jar.read_bytes() != source:
                raise RuntimeError("The JAR changed during compression; original left untouched")
            os.replace(temporary, jar)
        finally:
            Path(temporary).unlink(missing_ok=True)
    print(f"Structure Zopfli: {len(structures)} resources, {len(replacements)} improved, {before:,} -> {after:,} compressed bytes")
    return len(structures), before, after


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path)
    parser.add_argument("cache", type=Path)
    parser.add_argument("--iterations", type=int, default=15)
    parser.add_argument("--dependency-dir", type=Path, default=BUILD / "structure-tools/python")
    parser.add_argument("--pip-cache", type=Path, default=BUILD / "structure-tools/pip-cache")
    args = parser.parse_args()
    compressor, version = load_compressor(args.dependency_dir, args.pip_cache)
    optimize(args.jar, args.cache, args.iterations, compressor, version)
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, RuntimeError, ImportError, struct.error, zipfile.BadZipFile, subprocess.CalledProcessError) as error:
        print(f"Structure compression failed: {error}", file=sys.stderr)
        sys.exit(1)
