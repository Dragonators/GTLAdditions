#version 150
// Positions are in each shell's own frame from the black hole's centre, so the overlay turns with the shell.
#moj_import <gtladditions:heart_scene_hand.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform float Time;
uniform float CircuitTime;
uniform float Power;
uniform float Gap;
uniform float Kappa;
uniform float Reach;
uniform float Cell;
uniform float MaxAngle;
uniform float Layer;
uniform vec3 Sweep;

in vec3 LocalPos;
in vec3 LocalNormal;
in vec2 AtlasUv;
flat in vec4 Data;

out vec4 fragColor;

// BED (off) would leave a dark channel under the collectors' circuit line.
const vec3 LINE_REST = vec3(0.800, 0.565, 0.160);
const vec3 LINE_HOT = vec3(1.000, 0.771, 0.375);

// At the Sweep's peak the line covers the collector beneath it with its own colour (up to 0.9 * SWEEP_TINT), so it
// reads as warm gold on the white plates instead of adding up to white; the additive light fades by (1 - SWEEP_TINT)^3.
const float SWEEP_TINT = 0.25;
const float SWEEP_INTENSITY = 1.1;

// Away from the Sweep the circuit is all but invisible, so the collectors read as clean white plates.
const float REST_VIS = 0.05;
const float BED = 0.0;
const float BED_TINT = 0.10;
const float REST_GLOW = 0.25;
const float REST = 0.2;
const float PEAK = 0.7;
const float WHITE = 0.2;
const float LIT = 0.75;
const float INNER_LIT = 0.6;
const float COIL = 0.4;

// The flat collectors lack the voxel steps' AO: FLAT_AO darkens the world light on them evenly (the black hole's light
// keeps its strength), and EDGE_AO is the shade along a straight stretch of frame on both broad faces (deeper in corners),
// gone EDGE_WIDTH blocks in.
const float FLAT_AO = 0.1;
const float EDGE_AO = 0.3;
const float EDGE_WIDTH = 1.5;

/* Sampler1 rows (HeartShellRenderer.fillPlateTexture): shell * 256 + plate holds the plate frame (tangent, bitangent,
   normal toward the hole, centre); TILE_ROW.. the long-run circuit tile; COLLECTOR_ROW + shell its collector rows. */
const int TILE_ROW = 1024;
const int COLLECTOR_ROW = 1056;

uint pcg(uint v) {
    uint s = v * 747796405u + 2891336453u;
    uint w = ((s >> ((s >> 28u) + 4u)) ^ s) * 277803737u;
    return (w >> 22u) ^ w;
}

float segmentDistance(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    return length(pa - ba * clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0));
}

// Distance (in nodes, 0.5 block) to the 32 x 32 node tile's links to E, N, NE and NW (bits 1, 2, 4, 8).
float circuitDistance(vec2 g) {
    ivec2 f = ivec2(floor(g));
    float d = 9.0;
    for (int j = -1; j <= 2; j++) {
        for (int i = -1; i <= 2; i++) {
            ivec2 node = f + ivec2(i, j);
            int links = int(texelFetch(Sampler1, ivec2(node.x & 31, TILE_ROW + (node.y & 31)), 0).r + 0.5);
            if (links == 0) continue;
            vec2 a = vec2(node);
            if ((links & 1) != 0) d = min(d, segmentDistance(g, a, a + vec2(1.0, 0.0)));
            if ((links & 2) != 0) d = min(d, segmentDistance(g, a, a + vec2(0.0, 1.0)));
            if ((links & 4) != 0) d = min(d, segmentDistance(g, a, a + vec2(1.0, 1.0)));
            if ((links & 8) != 0) d = min(d, segmentDistance(g, a, a + vec2(-1.0, 1.0)));
        }
    }
    return d;
}

// Share of [a, b] (relative to the sample) under a smooth window of half-width EDGE_WIDTH.
float windowShare(float a, float b) {
    return smoothstep(-EDGE_WIDTH, EDGE_WIDTH, b) - smoothstep(-EDGE_WIDTH, EDGE_WIDTH, a);
}

/* From the collector's rows (one run of cells per row) around p:
   x: L-infinity distance to the nearest master cell outside the collector;
   y: share of a smooth separable window around p outside the collector. A shade from it follows every voxel step of the
      frame and deepens into corners; a shade from the distance would turn each step into a tooth. */
vec2 collectorField(vec2 p, int shell) {
    int y0 = int(floor(p.y + 0.5));
    float d = 4.0;
    float inside = 0.0;
    for (int j = -2; j <= 2; j++) {
        int y = y0 + j;
        vec2 run = y >= -16 && y < 16 ? texelFetch(Sampler1, ivec2(y + 16, COLLECTOR_ROW + shell), 0).xy : vec2(1.0, -1.0);
        if (run.x > run.y) {
            d = min(d, max(abs(p.y - float(y)) - 0.5, 0.0));
            continue;
        }
        float dy = max(abs(p.y - float(y)) - 0.5, 0.0);
        d = min(d, max(max(0.0, min(p.x - run.x + 0.5, run.y + 0.5 - p.x)), dy));
        inside += windowShare(float(y) - 0.5 - p.y, float(y) + 0.5 - p.y) * windowShare(run.x - 0.5 - p.x, run.y + 0.5 - p.x);
    }
    return vec2(d, 1.0 - inside);
}

void main() {
    vec3 n = normalize(LocalNormal);
    int flags = int(Data.r * 255.0 + 0.5) & 15;
    int plate = int(Data.g * 255.0 + 0.5);
    float plateAngle = Data.b * 3.14159265;
    vec3 base = texture(Sampler0, AtlasUv).rgb;
    vec3 dpx = dFdx(LocalPos);
    vec3 dpy = dFdy(LocalPos);

    float dist = length(LocalPos);
    float facing = dot(n, -LocalPos / dist);
    bool collector = (flags & 1) != 0;
    vec3 warm = vec3(1.0, 0.76, 0.52) * (0.92 + 0.08 * sin(Time * 2.3));
    vec3 add = base * warm * (0.55 + 0.45 * max(facing, 0.0)) * pow(40.0 / dist, 0.85) * 0.75 * Power
        * smoothstep(-0.25, 0.15, facing) * (collector ? 1.0 : 0.3) * LIT * (Layer < 1.5 ? INNER_LIT : 1.0);

    float alpha = 0.0;
    float shadow = 0.0;
    vec3 over = vec3(0.0);
    if (collector) {
        // The long-run circuit in the plate's own frame, on the face toward the hole, fading out 0.2-0.6 inside the frame.
        int shell = clamp(int(Layer - 0.5), 0, 3);
        int row = shell * 256 + plate;
        vec3 tangent = texelFetch(Sampler1, ivec2(0, row), 0).xyz;
        vec3 bitangent = texelFetch(Sampler1, ivec2(1, row), 0).xyz;
        vec3 plateNormal = texelFetch(Sampler1, ivec2(2, row), 0).xyz;
        vec3 q = LocalPos - texelFetch(Sampler1, ivec2(3, row), 0).xyz;
        vec2 local = vec2(dot(q, tangent), dot(q, bitangent));

        uint h = pcg(uint(plate) * 7u + uint(Layer + 0.5) * 131u);
        float dd = circuitDistance((local + 0.5) / Cell - 0.5 + vec2(float(h & 31u), float((h >> 5) & 31u)) + 64.0);
        float fp = max(length(vec2(dot(dpx, tangent), dot(dpx, bitangent))), length(vec2(dot(dpy, tangent), dot(dpy, bitangent)))) / Cell;
        vec2 field = collectorField(local, shell);
        float riserFade = step(0.5, dot(n, plateNormal)) * smoothstep(0.2, 0.6, field.x);

        // Half the window lies outside along a straight stretch of frame.
        shadow = step(0.5, abs(dot(n, plateNormal))) * EDGE_AO * 2.0 * field.y;

        float farK = smoothstep(0.45, 1.05, fp);
        float aa = min(fp * 0.55, 0.36);
        float core = (1.0 - smoothstep(0.06 - aa, 0.13 + aa, dd)) * riserFade;
        float haloVar = 0.016 + fp * fp * 0.18;
        float halo = exp(-dd * dd / haloVar) * (1.0 - smoothstep(0.32, 0.50, dd)) * pow(0.016 / haloVar, 0.25) * riserFade;
        float bed = (1.0 - smoothstep(0.13 - aa, 0.24 + aa, dd)) * riserFade;
        float tintA = core * 0.30 * smoothstep(0.10, 0.40, fp) * (1.0 - farK) * (0.35 + 0.65 * Power);
        core = mix(core, 0.05 * riserFade, farK);
        halo = mix(halo, 0.10 * riserFade, farK);
        bed = mix(bed, 0.12 * riserFade, farK);

        float tau = acos(clamp(-dot(normalize(LocalPos), Sweep), -1.0, 1.0)) * Reach;
        float sn = sin(3.14159265 * ((CircuitTime - tau) / Gap - Layer * 0.17));
        float flow = exp(-Kappa * sn * sn);
        float e = 0.10 * Power;
        float pf = flow * Power * PEAK;
        float restA = max(bed * BED, tintA) * REST_VIS;
        vec3 restOver = (LINE_REST * (bed * BED * BED_TINT + tintA) + LINE_REST * halo * REST_GLOW * Power + LINE_REST * core * (0.45 + 0.55 * e) * REST
            + LINE_HOT * (core * e * 0.75 * REST + halo * e * 0.25 * REST)) * REST_VIS;
        vec3 light = (LINE_HOT * (core * pf * 1.3 + halo * pf * 0.65 + bed * pf * 0.38) + vec3(1.0) * core * pf * pf * 0.55 * WHITE)
            * pow(1.0 - SWEEP_TINT, 3.0) * SWEEP_INTENSITY;
        float peakA = min(0.9 * SWEEP_TINT * (core + 0.6 * halo) * flow * Power, 0.95);
        alpha = 1.0 - (1.0 - restA) * (1.0 - peakA);
        over = restOver * (1.0 - peakA) + LINE_HOT * (0.95 * SWEEP_INTENSITY) * peakA + light;
    }

    float ringA = MaxAngle - plateAngle;
    float ringH = fract(Time / 5.0 - (Layer - 1.0) * 0.09) * (MaxAngle + 0.9) - 0.3;
    float ring = exp(-pow((ringA - ringH) / 0.30, 2.0)) * Power;
    if ((flags & 8) != 0) add += base * ring * 0.9;

    // Premultiplied: alpha darkens the terrain beneath (edge and flat AO); the edge also shades this overlay's own light.
    vec3 color = (add * (1.0 - alpha) + over) * (1.0 - shadow);
    alpha = 1.0 - (1.0 - alpha) * (1.0 - shadow) * (collector ? 1.0 - FLAT_AO : 1.0);

    if ((flags & 2) != 0) {
        color = (base * 1.5 + vec3(0.05, 0.30, 0.42) + base * ring * 0.6) * Power;
        alpha = Power;
    }

    if ((flags & 4) != 0) {
        color = (base * (0.55 + 0.70 * COIL) + vec3(0.18, 0.32, 0.40) * COIL) * Power;
        alpha = Power;
    }

    if (sceneHandOccluded(gl_FragCoord.xy)) discard;
    fragColor = vec4(color, alpha);
}
