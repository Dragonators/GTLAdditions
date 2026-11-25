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

## Hidden structure resources

Animated structure masks read packaged GTLASB3 data through
`StructureResourceLoader`, using the same classloader and caches as FOA:

- FOA uses `rings/forge_of_the_antichrist_rings.bin`.
- Dimension Focus reuses `multiblock/dimension_focus_infinity_crafting_array.bin`,
  selecting `ADKLM` outside rows 18 through 22.
- Antientropy Condensation reuses `multiblock/antientropy_condensation_center.bin`,
  selecting the 32 `J` glass blocks and one `M` generator.
- The Matrix uses `masks/apocalyptic_matrix_frames.bin`. Its 3,072 accepted
  coordinates are a subset of materials also used elsewhere in the full machine,
  so selecting those materials from the machine would hide additional blocks.
  The eye also hides the 110 central `X` coils through `MatrixEyeBlockLayout`.

The former runtime JSON under `effects/` is no longer used. Resource packs do not
override these classpath-loaded structure grids.

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
