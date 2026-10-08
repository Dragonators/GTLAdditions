# Structure resources

Edit the JSON under `scripts/structure_sources`, then regenerate and verify:

```sh
python -B scripts/generate_structure_resources.py generate
python -B scripts/generate_structure_resources.py verify
```

The generator requires Python 3.10 or newer and only the standard library. It tries
all six axis orders for each complete resource and compares raw DEFLATE level 6
sizes. Ties prefer the smaller binary, then the earlier axis order. All grids in
a ring-set candidate use the same order. Orders whose dictionary would exceed
65,535 rows are skipped. The selected resource is decoded and
compared to its JSON before writing. JSON coordinates remain `[aisle,row,column]`.

`verify` checks content, the resource set and the current binary version. If a
resource was restored from an older Git revision, run `generate` again.

## GTLASB3 binary format

All multi-byte integers are unsigned, big endian. A resource contains:

| Field | Encoding |
|---|---|
| Magic | Seven ASCII bytes: `GTLASB3` |
| Kind | u8: 1 = factory pattern, 2 = ring set |
| ID | u16 UTF-8 byte length, followed by ID bytes |
| Factory pattern | One grid |
| Ring set | u16 ring count, followed by that many grids in their original order |

Each grid contains:

| Field | Encoding |
|---|---|
| Axis order | u8: 0 = 012, 1 = 021, 2 = 102, 3 = 120, 4 = 201, 5 = 210 |
| Stored dimensions | Three nonzero u16 values in the selected axis order |
| Unique row count | Nonzero u16 |
| Row dictionary | Printable ASCII rows, each exactly the third stored dimension wide |
| Row indexes | First dimension × second dimension indexes in row-major order |

An index uses u8 when the dictionary has at most 256 rows, otherwise u16. There
is no separate index-width flag. The loader restores the original dimensions and
axis order before returning arrays to factory patterns or previews. Equal rows
share String instances within the restored grid. Spaces, punctuation, controller
positions and repeatable aisle coordinates must remain exact.

The runtime loader also accepts `GTLASB2`, which has no axis byte and always uses
u16 indexes. Checked-in generated resources must use `GTLASB3`.

## Build-time compression

Normal `build` and direct `remapJar` automatically apply Zopfli to the final
remapped JAR. The build waits for Loom's remapping worker before compression, so
the optimized artifact is also the one used by the existing publication setup.
The optimizer is `scripts/optimize_structure_jar.py`. It requires Python 3.10 or
newer and the pinned `zopfli==0.4.3` wheel listed in
`scripts/requirements-structure-tools.txt`. On first use, it installs the wheel
into `build/structure-tools/python/<interpreter-fingerprint>` with the existing
Python interpreter. Pip downloads and temporary files stay under
`build/structure-tools/pip-cache` and `build/structure-tools/pip-temp`; the global
Python environment is not modified. Later builds validate and reuse that local
installation. No build tool or extra compression library is included in the mod JAR.

Gradle defaults to `python` on Windows and `python3` elsewhere. Set
`-PstructurePython=/absolute/path/to/python` to choose a different interpreter.
The first installation needs pip and package-index access; later runs can reuse
the installed wheel and compression cache offline.

Zopfli uses 15 iterations and produces ordinary raw DEFLATE for ZIP entries.
Only `assets/gtladditions/structures/**/*.bin` entries are candidates; the existing
compressed stream is retained if it is smaller. Other entries keep their raw
compressed bytes. Content-addressed compression results are cached under
`build/structure-compression/cache`. A cached stream must decode byte-for-byte
before reuse. Clearing `build` simply causes recompression on the next build.

The script reads the standard single-volume ZIP32 JAR layout directly. It replaces
only smaller structure payloads and updates ZIP sizes, data descriptors and
central-directory offsets. Other local ZIP records remain byte-for-byte identical.
Before replacing the JAR, it compares structure contents and metadata, verifies
all CRCs, and checks that the source JAR has not changed concurrently. Replacement
uses a same-directory temporary file and `os.replace`; failures leave the original
JAR intact. ZIP64 and multi-volume archives are rejected before replacement.

## Validation

Use the generator's `verify` command to compare every resource with its source
JSON and reject older binary versions. The build-time optimizer also verifies
cached DEFLATE streams and final JAR contents before replacing the archive.

Run the complete build with `./gradlew build`. If the Windows wrapper launcher is
absent, invoke the bundled wrapper JAR from the project root:

```powershell
java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain build
```

Measured on 2026-10-08 for the current 31 resources / 33 grids / 6,274,112 cells:

| Representation | Bytes |
|---|---:|
| Old binary resources | 821,782 |
| GTLASB3 binary resources | 581,777 |
| Old structure entries compressed in JAR | 78,062 |
| GTLASB3 with ordinary ZIP compression | 51,062 |
| GTLASB3 with build-time Python Zopfli | 42,018 |

The final compressed structure payload is 46.17% smaller than the old JAR payload.
These values exclude unchanged ZIP directory overhead. Python calls the native
Zopfli implementation, so its compressed bytes can differ slightly from the former
Pngtastic JVM port. Runtime game startup time and heap use were not benchmarked;
byte-for-byte structure equality and shared rows were verified.
