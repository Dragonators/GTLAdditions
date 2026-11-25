#version 150
#moj_import <gtladditions:light_hunter_interior_materials.glsl>
in vec2 uv;
in vec3 localPosition;
flat in int material;
flat in float surfaceLight;
uniform float DisplayTime;
uniform vec2 FanCenters;
out vec4 fragColor;

vec3 fanBacklight() {
    float center = abs(localPosition.x - FanCenters.x) < abs(localPosition.x - FanCenters.y) ? FanCenters.x : FanCenters.y;
    vec2 local = vec2(localPosition.z, center - localPosition.x);
    float radius = length(local) / 0.765;
    vec3 glow = vec3(0.07, 0.24, 0.30) * mix(0.45, 1.0, 1.0 - smoothstep(0.15, 1.0, radius));
    vec2 pitch = vec2(0.075, 0.065);
    vec2 cell = local / pitch;
    cell.x += 0.5 * mod(floor(cell.y), 2.0);
    float hole = length((fract(cell) - 0.5) * pitch);
    float aa = max(length(fwidth(local)), 1e-5);
    float open = 1.0 - smoothstep(0.021 - aa, 0.021 + aa, hole);
    // Distant plates resolve to their average opening instead of shimmering.
    open = mix(open, 0.28, smoothstep(0.15, 0.4, aa / pitch.y));
    return mix(vec3(0.016, 0.032, 0.040), glow, open);
}

// UVs rotate with the rotor.
vec3 fanRotor(float grain) {
    vec3 metal = vec3(0.30, 0.36, 0.39);
    if (material == INTERIOR_FAN_HUB) {
        float aa = max(length(fwidth(uv)), 1e-5);
        float bolt = 1.0;
        for (int i = 0; i < 7; i++) {
            float angle = 0.147 + float(i) * 0.8975979;
            bolt = min(bolt, length(uv - 0.66 * vec2(cos(angle), sin(angle))));
        }
        float cap = 1.0 - smoothstep(0.34 - aa, 0.34 + aa, length(uv));
        float hole = 1.0 - smoothstep(0.15 - aa, 0.15 + aa, bolt);
        return mix(mix(metal * 1.2, metal * 0.7, cap), vec3(0.06, 0.08, 0.09), hole);
    }
    float aa = fwidth(uv.x);
    float lead = smoothstep(0.84 - aa, 0.88 + aa, uv.x);
    return mix(metal * 0.8, metal * 1.2, uv.x) + vec3(0.20, 0.24, 0.25) * lead + grain * 0.012;
}

vec3 hullDisplayColor() {
    float u = uv.x;
    float v = uv.y;
    float scan = 0.158421875 + fract(DisplayTime / 8.0 + 0.12) * 0.68315625;
    float dist = abs(u - scan);
    float pulse = exp(-dist * 65.0);
    if (material == INTERIOR_DISPLAY_HOUSING) return vec3(0.018,0.038,0.052) * surfaceLight;
    if (material == INTERIOR_DISPLAY_GLASS) return vec3(0.040,0.075,0.095) * surfaceLight;
    if (material == INTERIOR_DISPLAY_OUTLINE) return vec3(0.27,0.48,0.55) + vec3(0.20,0.30,0.32) * pulse;
    if (material == INTERIOR_DISPLAY_LABEL) return vec3(0.60,0.74,0.79);
    if (material == INTERIOR_DISPLAY_MARKER) return vec3(0.68,0.56,0.35);
    if (material == INTERIOR_DISPLAY_TRIM) return vec3(0.25,0.48,0.55);
    vec2 gridPosition = vec2(u * 20.0, v * 9.0);
    vec2 gridDistance = abs(fract(gridPosition + 0.5) - 0.5);
    float aa = max(max(fwidth(gridPosition.x), fwidth(gridPosition.y)), 1e-5);
    float grid = 1.0 - smoothstep(0.009, 0.009 + aa, min(gridDistance.x, gridDistance.y));
    float band = step(0.0833,v) * step(v,0.8444);
    float beam = 1.0 - smoothstep(0.0008, 0.0008 + max(fwidth(u),0.0003), dist);
    float trail = exp(-max(scan-u,0.0)*40.0) * step(u,scan);
    float header = 1.0-smoothstep(0.001,0.001+max(fwidth(v),0.0003),abs(v-0.864));
    return vec3(0.025,0.055,0.075) + vec3(0.018,0.040,0.055)*grid
        + vec3(0.12,0.30,0.35)*beam*band + vec3(0.015,0.04,0.05)*trail*band
        + vec3(0.025,0.06,0.07)*header;
}

void main() {
    if (material >= INTERIOR_DISPLAY_HOUSING) {
        fragColor = vec4(hullDisplayColor(), 1.0);
        return;
    }
    float edge = smoothstep(0.0, 0.14, min(uv.y, 1.0 - uv.y));
    float grain = fract(sin(dot(floor(localPosition * 256.0), vec3(12.9898,78.233,37.719))) * 43758.5453) - 0.5;
    vec3 color;
    if (material == INTERIOR_HOUSING) {
        color = vec3(0.07,0.12,0.16);
    } else if (material == INTERIOR_WHITE) {
        color = mix(vec3(0.42,0.64,0.73), vec3(0.84,0.94,1.0), edge);
    } else if (material == INTERIOR_DIFFUSER) {
        float phase = fract(localPosition.x / 32.0 - DisplayTime / 8.0);
        float wave = exp(-(1.0 - phase) * 4.0) * (1.0 - smoothstep(0.96, 1.0, phase));
        color = mix(vec3(0.78,0.89,0.98), vec3(1.0), wave) + grain * 0.012;
    } else if (material == INTERIOR_RECESS) {
        color = fanBacklight();
    } else if (material == INTERIOR_FAN_TRIM) {
        color = vec3(0.18,0.58,0.68);
    } else {
        if (material == INTERIOR_FAN_FRAME) color = vec3(0.22,0.27,0.30) + grain * 0.012;
        else if (material == INTERIOR_FAN_MARKING) color = vec3(0.463,0.584,0.631);
        else if (material == INTERIOR_FAN_MOUNT) color = vec3(0.094,0.165,0.204);
        else if (material == INTERIOR_FAN_BLADE || material == INTERIOR_FAN_HUB) color = fanRotor(grain);
        else color = vec3(0.20,0.25,0.28);
        color *= surfaceLight;
    }
    fragColor = vec4(color,1.0);
}
