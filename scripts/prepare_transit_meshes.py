"""Compact transit hull vertices losslessly and bake the two fixed ribbon profiles."""

import argparse
import gzip
import json
import struct
from pathlib import Path


ASSETS = Path(__file__).resolve().parents[1] / "src/main/resources/assets/gtladditions/subspace_nexus"


def compact(source, output, geometry):
    relative = geometry["file"]
    packed = (source / relative).read_bytes()
    raw = gzip.decompress(packed)
    fields = list(geometry["attributes"].values())
    widths = [4 * field["size"] for field in fields]
    distinct = {}
    remap = []
    for vertex in range(fields[0]["count"]):
        row = tuple(
            raw[field["offset"] + vertex * width:field["offset"] + (vertex + 1) * width]
            for field, width in zip(fields, widths)
        )
        remap.append(distinct.setdefault(row, len(distinct)))
    if len(distinct) == len(remap):
        (output / relative).write_bytes(packed)
        return

    vertices = bytearray()
    for column, field in enumerate(fields):
        field.update(count=len(distinct), offset=len(vertices))
        vertices.extend(b"".join(row[column] for row in distinct))
    indices = struct.iter_unpack("<I", raw[geometry["indexOffset"]:])
    geometry["indexOffset"] = len(vertices)
    vertices.extend(b"".join(struct.pack("<I", remap[index]) for (index,) in indices))
    (output / relative).write_bytes(gzip.compress(vertices, compresslevel=9, mtime=0))


def smooth(start, end, x):
    t = max(0.0, min(1.0, (x - start) / (end - start)))
    return t * t * (3 - 2 * t)


def ribbon(output, node, segments, head, tail, plume=False):
    peak_u = head / (head + tail)
    peak = peak_u ** head * (1 - peak_u) ** tail
    raw = bytearray()
    for i in range(segments + 1):
        u = struct.unpack("<f", struct.pack("<f", i / segments))[0]
        envelope = min(1.0, u ** head * (1 - u) ** tail / peak)
        for side in (-1.0, 1.0):
            raw.extend(struct.pack("<3f", u, side, envelope ** 0.78))
            if plume:
                profile = 0.65 * smooth(0, 0.14, u) * smooth(1, 0.82, u) + 0.35 * envelope ** 0.35
                raw.extend(struct.pack("<f", profile))

    geometry = node["geometry"]
    geometry["attributes"]["quv"].update(count=2 * (segments + 1), size=4 if plume else 3)
    geometry.update(indexOffset=len(raw), indexCount=6 * segments)
    for i in range(0, 2 * segments, 2):
        raw.extend(struct.pack("<6I", i, i + 2, i + 1, i + 1, i + 2, i + 3))
    shuffled = b"".join(raw[lane::4] for lane in range(4))
    (output / geometry["file"]).write_bytes(gzip.compress(shuffled, compresslevel=9, mtime=0))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path, help="exported subspace_nexus asset directory")
    parser.add_argument("output", type=Path, help="separate directory for the five meshes and matching scene.json")
    args = parser.parse_args()
    if args.output.resolve() in (args.source.resolve(), ASSETS):
        parser.error("choose a separate output directory to keep the input assets intact")
    (args.output / "meshes").mkdir(parents=True, exist_ok=True)
    exported = json.loads((args.source / "scene.json").read_bytes())
    source_nodes = {node["label"]: node for node in exported["nodes"]}
    scene = json.loads((ASSETS / "scene.json").read_bytes())
    nodes = {node["label"]: node for node in scene["nodes"]}
    for craft in ("gozanti", "decimator", "bff1"):
        prefix = f"transit.{craft}."
        geometry = source_nodes[prefix + "solid"]["geometry"]
        compact(args.source, args.output, geometry)
        for part in ("solid", "depth", "fade"):
            nodes[prefix + part]["geometry"] = geometry
    ribbon(args.output, nodes["transit.plumes"], 28, 0.9, 0.75, plume=True)
    ribbon(args.output, nodes["transit.trails"], 160, 2.4, 0.12)
    (args.output / "scene.json").write_bytes((json.dumps(scene, separators=(",", ":"), ensure_ascii=False) + "\n").encode())


if __name__ == "__main__":
    main()
