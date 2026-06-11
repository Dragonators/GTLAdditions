import argparse
import base64
import json
import re
import shutil
import struct
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
KOTLIN_STRUCTURE_ROOT = ROOT / "src/main/java/com/gtladd/gtladditions/common/machine/multiblock/structure"
SOURCE_ROOT = ROOT / "scripts/structure_sources"
RESOURCE_ROOT = ROOT / "src/main/resources/assets/gtladditions/structures"
GTLCORE_JAR = ROOT / "libs/gtlcore-1.2.2.9-fix3.jar"
MAGIC = b"GTLASB2"
KIND_FACTORY_PATTERN = 1
KIND_RING_SET = 2


def fail(message: str) -> None:
    raise SystemExit(message)


def mask_strings(source: str) -> str:
    result = []
    index = 0
    in_string = False
    escaped = False
    while index < len(source):
        char = source[index]
        if not in_string:
            if char == '"':
                in_string = True
                escaped = False
                result.append(" ")
            else:
                result.append(char)
            index += 1
            continue

        if escaped:
            escaped = False
            result.append("\n" if char == "\n" else " ")
        elif char == "\\":
            escaped = True
            result.append(" ")
        elif char == '"':
            in_string = False
            result.append(" ")
        else:
            result.append("\n" if char == "\n" else " ")
        index += 1
    return "".join(result)


def val_expressions(path: Path) -> list[tuple[str, str]]:
    source = path.read_text(encoding="utf-8")
    masked = mask_strings(source)
    matches = list(re.finditer(r"(?m)^\s{4}val\s+(\w+)\s*(?::\s*[^=\n]+)?\s*=\s*", masked))
    expressions = []
    object_end = masked.rfind("\n}")
    for index, match in enumerate(matches):
        start = match.end()
        end = matches[index + 1].start() if index + 1 < len(matches) else object_end
        expressions.append((match.group(1), source[start:end]))
    return expressions


def scan_string(source: str, start: int) -> tuple[str, int]:
    index = start + 1
    escaped = False
    while index < len(source):
        char = source[index]
        if escaped:
            escaped = False
        elif char == "\\":
            escaped = True
        elif char == '"':
            return json.loads(source[start:index + 1]), index + 1
        index += 1
    fail(f"Unclosed string literal near offset {start}")


def skip_space(source: str, start: int) -> int:
    while start < len(source) and source[start].isspace():
        start += 1
    return start


def parse_identifier(source: str, start: int) -> tuple[str, int]:
    match = re.match(r"[A-Za-z_]\w*", source[start:])
    if not match:
        fail(f"Expected identifier near offset {start}")
    return match.group(0), start + len(match.group(0))


def parse_array_of(source: str, start: int, identifiers: dict[str, object] | None = None) -> tuple[list[object], int]:
    identifiers = identifiers or {}
    start = skip_space(source, start)
    if not source.startswith("arrayOf", start):
        fail(f"Expected arrayOf near offset {start}")
    index = skip_space(source, start + len("arrayOf"))
    if index >= len(source) or source[index] != "(":
        fail(f"Expected '(' after arrayOf near offset {index}")
    index += 1

    values = []
    while True:
        index = skip_space(source, index)
        if index >= len(source):
            fail("Unclosed arrayOf")
        if source[index] == ")":
            return values, index + 1
        if source.startswith("arrayOf", index):
            value, index = parse_array_of(source, index, identifiers)
        elif source[index] == '"':
            value, index = scan_string(source, index)
        else:
            value, index = parse_identifier(source, index)
            if value not in identifiers:
                fail(f"Unknown array reference {value}")
            value = identifiers[value]
        values.append(value)

        index = skip_space(source, index)
        if index < len(source) and source[index] == ",":
            index += 1
        elif index < len(source) and source[index] == ")":
            continue
        else:
            fail(f"Expected ',' or ')' near offset {index}")


def string_literals(source: str) -> list[str]:
    values = []
    index = 0
    while index < len(source):
        if source[index] == '"':
            value, index = scan_string(source, index)
            values.append(value)
        else:
            index += 1
    return values


def balanced_call(source: str, open_paren: int) -> tuple[str, int]:
    depth = 0
    index = open_paren
    in_string = False
    escaped = False
    while index < len(source):
        char = source[index]
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
        else:
            if char == '"':
                in_string = True
            elif char == "(":
                depth += 1
            elif char == ")":
                depth -= 1
                if depth == 0:
                    return source[open_paren + 1:index], index + 1
        index += 1
    fail(f"Unclosed call near offset {open_paren}")


def aisle_calls(source: str) -> list[str]:
    calls = []
    search_start = 0
    marker = ".aisle("
    while True:
        marker_index = source.find(marker, search_start)
        if marker_index == -1:
            return calls
        call_source, search_start = balanced_call(source, marker_index + len(".aisle"))
        calls.append(call_source)


def structure_id(property_name: str) -> str:
    name = property_name
    if name.endswith("_STRUCTURE"):
        name = name.removesuffix("_STRUCTURE")
    return name.lower()


def source_path_for(structure_id_value: str) -> Path:
    return SOURCE_ROOT / "multiblock" / f"{structure_id_value}.json"


def resource_path_for(source_path: Path) -> Path:
    return RESOURCE_ROOT / source_path.relative_to(SOURCE_ROOT).with_suffix(".bin")


def canonical_factory(structure_id_value: str, aisles: list[list[str]]) -> dict[str, object]:
    return canonicalize(
        {
            "version": 1,
            "type": "factory_pattern",
            "id": structure_id_value,
            "aisles": aisles,
        },
        f"factory_pattern:{structure_id_value}",
    )


def canonical_ring_set(structure_id_value: str, rings: list[list[list[str]]]) -> dict[str, object]:
    return canonicalize(
        {
            "version": 1,
            "type": "ring_set",
            "id": structure_id_value,
            "rings": rings,
        },
        f"ring_set:{structure_id_value}",
    )


def validate_string_grid(grid: object, context: str, field: str) -> list[list[str]]:
    if not isinstance(grid, list) or not grid:
        fail(f"{context}: {field} must be a non-empty array")
    result = []
    expected_rows = None
    expected_width = None
    for aisle_index, aisle in enumerate(grid):
        if not isinstance(aisle, list) or not aisle:
            fail(f"{context}: {field}[{aisle_index}] must be a non-empty array")
        if expected_rows is None:
            expected_rows = len(aisle)
        elif len(aisle) != expected_rows:
            fail(f"{context}: inconsistent row count at {field}[{aisle_index}]")
        rows = []
        for row_index, row in enumerate(aisle):
            if not isinstance(row, str):
                fail(f"{context}: {field}[{aisle_index}][{row_index}] must be a string")
            if expected_width is None:
                expected_width = len(row)
            elif len(row) != expected_width:
                fail(f"{context}: inconsistent row width at {field}[{aisle_index}][{row_index}]")
            rows.append(row)
        result.append(rows)
    return result


def validate_ring_grid(grid: object, context: str, field: str) -> list[list[list[str]]]:
    if not isinstance(grid, list) or not grid:
        fail(f"{context}: {field} must be a non-empty array")
    rings = []
    for ring_index, ring in enumerate(grid):
        ring_context = f"{context}:{field}[{ring_index}]"
        rings.append(validate_string_grid(ring, ring_context, "planes"))
    return rings


def canonicalize(data: object, path: str) -> dict[str, object]:
    if not isinstance(data, dict):
        fail(f"{path}: top-level JSON value must be an object")

    version = data.get("version")
    structure_type = data.get("type")
    structure_id_value = data.get("id")
    if version != 1:
        fail(f"{path}: version must be 1")
    if not isinstance(structure_type, str):
        fail(f"{path}: type must be a string")
    if not isinstance(structure_id_value, str) or not structure_id_value:
        fail(f"{path}: id must be a non-empty string")

    if structure_type == "factory_pattern":
        return {
            "version": 1,
            "type": structure_type,
            "id": structure_id_value,
            "aisles": validate_string_grid(data.get("aisles"), path, "aisles"),
        }
    if structure_type == "ring_set":
        return {
            "version": 1,
            "type": structure_type,
            "id": structure_id_value,
            "rings": validate_ring_grid(data.get("rings"), path, "rings"),
        }
    fail(f"{path}: unsupported type {structure_type!r}")


def pretty_json(data: dict[str, object]) -> str:
    return json.dumps(data, ensure_ascii=False, indent=2)


def write_source(path: Path, data: dict[str, object]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(pretty_json(data), encoding="utf-8")


def read_json(path: Path) -> dict[str, object]:
    try:
        with path.open("r", encoding="utf-8") as handle:
            return json.load(handle)
    except json.JSONDecodeError as error:
        fail(f"{path}: invalid JSON: {error}")


def pack_u8(value: int, context: str) -> bytes:
    if not 0 <= value <= 0xFF:
        fail(f"{context}: {value} does not fit in u8")
    return struct.pack(">B", value)


def pack_u16(value: int, context: str) -> bytes:
    if not 0 <= value <= 0xFFFF:
        fail(f"{context}: {value} does not fit in u16")
    return struct.pack(">H", value)


def unpack_u8(data: bytes, offset: int, context: str) -> tuple[int, int]:
    if offset + 1 > len(data):
        fail(f"{context}: truncated u8")
    return data[offset], offset + 1


def unpack_u16(data: bytes, offset: int, context: str) -> tuple[int, int]:
    if offset + 2 > len(data):
        fail(f"{context}: truncated u16")
    return struct.unpack_from(">H", data, offset)[0], offset + 2


def encode_id(structure_id_value: str, context: str) -> bytes:
    encoded = structure_id_value.encode("utf-8")
    return pack_u16(len(encoded), context) + encoded


def decode_id(data: bytes, offset: int, context: str) -> tuple[str, int]:
    length, offset = unpack_u16(data, offset, context)
    if offset + length > len(data):
        fail(f"{context}: truncated id")
    try:
        return data[offset:offset + length].decode("utf-8"), offset + length
    except UnicodeDecodeError as error:
        fail(f"{context}: invalid UTF-8 id: {error}")


def row_bytes(row: str, context: str) -> bytes:
    try:
        encoded = row.encode("ascii")
    except UnicodeEncodeError as error:
        fail(f"{context}: row contains non-ASCII character: {error}")
    for byte in encoded:
        if byte < 0x20 or byte > 0x7E:
            fail(f"{context}: row contains non-printable byte {byte}")
    return encoded


def decode_row(data: bytes, offset: int, width: int, context: str) -> tuple[str, int]:
    if offset + width > len(data):
        fail(f"{context}: truncated row")
    encoded = data[offset:offset + width]
    for byte in encoded:
        if byte < 0x20 or byte > 0x7E:
            fail(f"{context}: row contains non-printable byte {byte}")
    return encoded.decode("ascii"), offset + width


def build_row_dictionary(rows: list[str], context: str) -> tuple[list[str], list[int]]:
    dictionary = []
    indexes = []
    index_by_row = {}
    for row in rows:
        index = index_by_row.get(row)
        if index is None:
            index = len(dictionary)
            index_by_row[row] = index
            dictionary.append(row)
        indexes.append(index)
    if len(dictionary) > 0xFFFF:
        fail(f"{context}: unique row count {len(dictionary)} exceeds u16")
    return dictionary, indexes


def encode_grid(grid: list[list[str]], context: str) -> bytes:
    aisle_count = len(grid)
    row_count = len(grid[0])
    width = len(grid[0][0])
    rows = [row for aisle in grid for row in aisle]
    dictionary, indexes = build_row_dictionary(rows, context)

    output = bytearray()
    output += pack_u16(aisle_count, f"{context}: aisle count")
    output += pack_u16(row_count, f"{context}: row count")
    output += pack_u16(width, f"{context}: row width")
    output += pack_u16(len(dictionary), f"{context}: dictionary size")
    for row_index, row in enumerate(dictionary):
        output += row_bytes(row, f"{context}: dictionary row {row_index}")
    for row_index, index in enumerate(indexes):
        output += pack_u16(index, f"{context}: row index {row_index}")
    return bytes(output)


def decode_grid(data: bytes, offset: int, context: str) -> tuple[list[list[str]], int]:
    aisle_count, offset = unpack_u16(data, offset, f"{context}: aisle count")
    row_count, offset = unpack_u16(data, offset, f"{context}: row count")
    width, offset = unpack_u16(data, offset, f"{context}: row width")
    dictionary_size, offset = unpack_u16(data, offset, f"{context}: dictionary size")
    if aisle_count == 0 or row_count == 0 or width == 0 or dictionary_size == 0:
        fail(f"{context}: dimensions and dictionary size must be non-zero")

    dictionary = []
    for row_index in range(dictionary_size):
        row, offset = decode_row(data, offset, width, f"{context}: dictionary row {row_index}")
        dictionary.append(row)

    grid = []
    for aisle_index in range(aisle_count):
        aisle = []
        for row_index in range(row_count):
            index, offset = unpack_u16(data, offset, f"{context}: row index")
            if index >= dictionary_size:
                fail(f"{context}: row index {index} exceeds dictionary size {dictionary_size}")
            aisle.append(dictionary[index])
        grid.append(aisle)
    return grid, offset


def write_binary(path: Path, data: dict[str, object]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    output = bytearray(MAGIC)
    output += pack_u8(KIND_FACTORY_PATTERN if data["type"] == "factory_pattern" else KIND_RING_SET, str(path))
    output += encode_id(data["id"], str(path))

    if data["type"] == "factory_pattern":
        output += encode_grid(data["aisles"], str(path))
    elif data["type"] == "ring_set":
        rings = data["rings"]
        output += pack_u16(len(rings), f"{path}: ring count")
        for ring_index, ring in enumerate(rings):
            output += encode_grid(ring, f"{path}: ring {ring_index}")
    else:
        fail(f"{path}: unsupported type {data['type']!r}")

    path.write_bytes(bytes(output))


def read_binary(path: Path) -> dict[str, object]:
    data = path.read_bytes()
    offset = 0
    if data[:len(MAGIC)] != MAGIC:
        fail(f"{path}: invalid magic")
    offset += len(MAGIC)
    kind, offset = unpack_u8(data, offset, str(path))
    structure_id_value, offset = decode_id(data, offset, str(path))

    if kind == KIND_FACTORY_PATTERN:
        aisles, offset = decode_grid(data, offset, str(path))
        result = {
            "version": 1,
            "type": "factory_pattern",
            "id": structure_id_value,
            "aisles": aisles,
        }
    elif kind == KIND_RING_SET:
        ring_count, offset = unpack_u16(data, offset, f"{path}: ring count")
        if ring_count == 0:
            fail(f"{path}: ring count must be non-zero")
        rings = []
        for ring_index in range(ring_count):
            ring, offset = decode_grid(data, offset, f"{path}: ring {ring_index}")
            rings.append(ring)
        result = {
            "version": 1,
            "type": "ring_set",
            "id": structure_id_value,
            "rings": rings,
        }
    else:
        fail(f"{path}: unknown kind {kind}")

    if offset != len(data):
        fail(f"{path}: trailing {len(data) - offset} bytes")
    return result


def light_hunter_arrays() -> dict[str, list[str]]:
    arrays = {}
    for path in sorted((KOTLIN_STRUCTURE_ROOT / "lighthunterstructure").glob("LightHunterStructure*.kt")):
        for name, expression in val_expressions(path):
            if not name.startswith("A_"):
                continue
            values, _ = parse_array_of(expression, 0)
            if not all(isinstance(value, str) for value in values):
                fail(f"{path}:{name}: expected arrayOf strings")
            arrays[f"{path.stem}.{name}"] = values
    return arrays


def gtlcore_annihilate_arrays() -> dict[str, list[str]]:
    if not GTLCORE_JAR.exists():
        fail(f"Missing GTLCore jar required to extract AnnihilateGenerator arrays: {GTLCORE_JAR}")
    jshell = shutil.which("jshell")
    if jshell is None:
        fail("jshell is required to extract AnnihilateGenerator arrays from the GTLCore jar")

    script = """
import org.gtlcore.gtlcore.common.data.machines.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
for (int i = 1; i <= 109; i++) {
    Class<?> cls = i <= 53 ? AnnihilateGeneratorB.class : AnnihilateGeneratorA.class;
    String[] rows = (String[]) cls.getField("A_" + i).get(null);
    System.out.println("@@ " + cls.getSimpleName() + ".A_" + i + " " + rows.length);
    for (String row : rows) {
        System.out.println(Base64.getEncoder().encodeToString(row.getBytes(StandardCharsets.UTF_8)));
    }
}
/exit
"""
    process = subprocess.run(
        [jshell, "-s", "--class-path", str(GTLCORE_JAR)],
        input=script,
        text=True,
        capture_output=True,
        encoding="utf-8",
        errors="replace",
        timeout=60,
        check=False,
    )
    if process.returncode != 0:
        fail(f"jshell failed while extracting AnnihilateGenerator arrays:\n{process.stderr}")

    arrays: dict[str, list[str]] = {}
    current_reference = None
    expected_rows = 0
    rows: list[str] = []

    def finish_current() -> None:
        nonlocal current_reference, expected_rows, rows
        if current_reference is None:
            return
        if len(rows) != expected_rows:
            fail(f"{current_reference}: expected {expected_rows} rows from GTLCore, got {len(rows)}")
        arrays[current_reference] = rows
        current_reference = None
        expected_rows = 0
        rows = []

    for raw_line in process.stdout.splitlines():
        line = raw_line.strip()
        if not line or line.startswith("->") or line.startswith("|") or line.startswith("jshell>"):
            continue
        if line.startswith("@@ "):
            finish_current()
            parts = line.split()
            if len(parts) != 3:
                fail(f"Unexpected jshell marker line: {line}")
            current_reference = parts[1]
            expected_rows = int(parts[2])
            rows = []
            continue
        if current_reference is None:
            continue
        try:
            rows.append(base64.b64decode(line).decode("utf-8"))
        except Exception as error:
            fail(f"{current_reference}: invalid row payload from jshell: {error}")

    finish_current()
    if len(arrays) != 109:
        fail(f"Expected 109 AnnihilateGenerator arrays from GTLCore, got {len(arrays)}")
    return arrays


def extract_factory_patterns() -> list[tuple[Path, dict[str, object]]]:
    referenced_arrays = light_hunter_arrays()
    loaded_annihilate_arrays = False
    extracted = []
    for path in sorted(KOTLIN_STRUCTURE_ROOT.glob("MultiBlockStructure*.kt")):
        for property_name, expression in val_expressions(path):
            if "FactoryBlockPattern.start" not in expression:
                continue
            aisles = []
            for call in aisle_calls(expression):
                stripped = call.strip()
                if stripped.startswith("*"):
                    reference = stripped[1:].strip()
                    if reference not in referenced_arrays and reference.startswith("AnnihilateGenerator"):
                        if not loaded_annihilate_arrays:
                            referenced_arrays.update(gtlcore_annihilate_arrays())
                            loaded_annihilate_arrays = True
                    if reference not in referenced_arrays:
                        fail(f"{path}:{property_name}: unknown aisle reference {stripped}")
                    aisles.append(referenced_arrays[reference])
                    continue

                rows = string_literals(call)
                if not rows:
                    fail(f"{path}:{property_name}: aisle call contains no string rows")
                aisles.append(rows)

            if not aisles:
                continue
            structure_id_value = structure_id(property_name)
            extracted.append((source_path_for(structure_id_value), canonical_factory(structure_id_value, aisles)))
    return extracted


def extract_rings() -> tuple[Path, dict[str, object]]:
    path = KOTLIN_STRUCTURE_ROOT / "RingStructure.kt"
    ring_arrays: dict[str, object] = {}
    rings_expression = None
    for property_name, expression in val_expressions(path):
        if property_name == "RINGS":
            rings_expression = expression
            continue
        if "arrayOf" not in expression:
            continue
        values, _ = parse_array_of(expression, 0)
        ring_arrays[property_name] = values

    if rings_expression is None:
        fail(f"{path}: missing RINGS declaration")
    rings, _ = parse_array_of(rings_expression, 0, ring_arrays)
    return SOURCE_ROOT / "rings/forge_of_the_antichrist_rings.json", canonical_ring_set(
        "forge_of_the_antichrist_rings",
        rings,
    )


def extract() -> int:
    written = 0
    for path, data in extract_factory_patterns():
        write_source(path, data)
        print(f"wrote {path.relative_to(ROOT)}")
        written += 1
    path, data = extract_rings()
    write_source(path, data)
    print(f"wrote {path.relative_to(ROOT)}")
    written += 1
    if written == 0:
        fail("No legacy Kotlin structure data was extracted.")
    return 0


def source_files() -> list[Path]:
    return sorted(SOURCE_ROOT.glob("**/*.json"))


def generate() -> int:
    files = source_files()
    if not files:
        fail(f"No source JSON files found under {SOURCE_ROOT}")
    for source_path in files:
        data = canonicalize(read_json(source_path), str(source_path.relative_to(ROOT)))
        output_path = resource_path_for(source_path)
        write_binary(output_path, data)
        print(f"wrote {output_path.relative_to(ROOT)}")
    return 0


def verify() -> int:
    files = source_files()
    if not files:
        fail(f"No source JSON files found under {SOURCE_ROOT}")

    expected_resources = {resource_path_for(source_path) for source_path in files}
    actual_resources = {path for path in RESOURCE_ROOT.glob("**/*") if path.is_file()}
    extra_resources = sorted(actual_resources - expected_resources)
    missing_resources = sorted(expected_resources - actual_resources)
    if extra_resources:
        fail("Extra generated resources:\n" + "\n".join(str(path.relative_to(ROOT)) for path in extra_resources))
    if missing_resources:
        fail("Missing generated resources:\n" + "\n".join(str(path.relative_to(ROOT)) for path in missing_resources))

    for source_path in files:
        resource_path = resource_path_for(source_path)
        source_data = canonicalize(read_json(source_path), str(source_path.relative_to(ROOT)))
        resource_data = canonicalize(read_binary(resource_path), str(resource_path.relative_to(ROOT)))
        if source_data != resource_data:
            fail(f"{resource_path.relative_to(ROOT)} does not match {source_path.relative_to(ROOT)}")
    print(f"verified {len(files)} structure resources")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Extract, generate, or verify GTLAdditions structure resources.")
    parser.add_argument("mode", choices=("extract", "generate", "verify"))
    args = parser.parse_args()

    if args.mode == "extract":
        return extract()
    if args.mode == "generate":
        return generate()
    if args.mode == "verify":
        return verify()
    raise AssertionError(args.mode)


if __name__ == "__main__":
    sys.exit(main())
