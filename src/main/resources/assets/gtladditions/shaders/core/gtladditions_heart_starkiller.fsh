#version 150

// The Starkiller base: unit radius, weapon on +x, pole +z; positions are camera-relative world coordinates.
// Vertex colours are sRGB; Sampler2 and Sampler3 hold the worn-metal picture and its normals, projected along the base's axes.
#moj_import <gtladditions:heart_scene_hand.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;
uniform mat4 ModelMat;
uniform vec3 Center;
uniform float Power;
uniform float BreathPhase;
uniform float Role;
uniform float Metal;
uniform float Rough;
uniform float Emit;

in vec3 VertexColor;
in vec3 WorldNormal;
in vec3 ObjectNormal;
in vec3 WorldPos;
in vec3 ObjectPos;
in vec3 ObjectLight;
in float ClipDepthFromFar;

out vec4 fragColor;

const vec3 CORE = vec3(0.768, 0.0, -0.02);
// The cut: its floor and ceiling at the rim, the rim's radius, and the plane of its end walls.
const float FLOOR = -0.205;
const float CEIL = 0.155;
const float RIM = 0.99;
const float ENDS = 0.3;
// The worn-metal picture repeats every WEAR_TILE radii (about 1.35 blocks); WEAR_MEAN is its mean in linear light.
const float WEAR_TILE = 0.12;
const float WEAR_MEAN = 0.343;
// Spread of the worn metal's light and dark, and the share of its relief.
const float WEAR_CONTRAST = 0.7;
const float WEAR_BUMP = 0.5;

vec3 lin(vec3 c) { return mix(c / 12.92, pow((c + 0.055) / 1.055, vec3(2.4)), step(vec3(0.04045), c)); }
vec3 srgb(vec3 c) { c = max(c, vec3(0.0)); return mix(c * 12.92, 1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055, step(vec3(0.0031308), c)); }

// How much of the sky a point in the trench sees (x) and whether the black hole's light reaches it through the opening (y).
vec2 trench(vec3 p, vec3 l) {
    float d = RIM - length(p.xy);
    float w = smoothstep(FLOOR - 0.015, FLOOR, p.z) * (1.0 - smoothstep(CEIL, CEIL + 0.015, p.z)) * smoothstep(0.0, 0.02, d) * smoothstep(ENDS - 0.03, ENDS, p.x);
    if (w <= 0.0) return vec2(1.0);
    float depth = clamp(d / 0.27, 0.0, 1.0);
    float edge = min(p.z - FLOOR, CEIL - p.z) / 0.18;
    float ao = mix(1.0, 0.42, pow(depth, 0.8)) * (0.68 + 0.32 * smoothstep(0.0, 0.35, edge));
    float a = dot(l.xy, l.xy);
    float b = dot(p.xy, l.xy);
    float t = (-b + sqrt(max(b * b - a * (dot(p.xy, p.xy) - RIM * RIM), 0.0))) / max(a, 1e-4);
    vec3 e = p + t * l;
    float lit = smoothstep(FLOOR - 0.012, FLOOR + 0.012, e.z) * (1.0 - smoothstep(CEIL - 0.012, CEIL + 0.012, e.z)) * smoothstep(ENDS - 0.05, ENDS, e.x);
    return mix(vec2(1.0), vec2(ao, lit), w);
}

void main() {
    vec3 n = normalize(WorldNormal);
    if (!gl_FrontFacing) n = -n;
    vec3 l = normalize(Center - WorldPos);
    vec3 v = normalize(-WorldPos);
    float nl = max(dot(n, l), 0.0);
    float mu = max(dot(n, v), 0.0);
    float breath = 0.97 + 0.03 * sin(BreathPhase);
    float glow = 0.1 + 0.9 * Power;
    bool ice = Role > 0.5 && Role < 1.5;
    // Derivatives in uniform control flow, before any branch.
    vec3 o = normalize(ObjectPos);
    vec2 uv = vec2(atan(o.y, o.x) / 6.2831853 + 0.5, asin(clamp(o.z, -1.0, 1.0)) / 3.14159265 + 0.5);
    vec2 dx = dFdx(uv);
    vec2 dy = dFdy(uv);
    float hu = fract(uv.x + 0.5);
    if (abs(dFdx(hu)) < abs(dx.x)) dx.x = dFdx(hu);
    if (abs(dFdy(hu)) < abs(dy.x)) dy.x = dFdy(hu);
    float footprint = max(length(dFdx(ObjectPos)), length(dFdy(ObjectPos)));
    vec3 wx = dFdx(WorldPos);
    vec3 wy = dFdy(WorldPos);
    vec3 objX = dFdx(ObjectPos);
    vec3 hc = textureGrad(Sampler1, uv, dx, dy).rgb;
    float lum = textureGrad(Sampler0, uv, dx, dy).r;
    float h = lum * 0.0004;
    float hX = dFdx(h);
    float hY = dFdy(h);
    vec3 color;
    if (Role > 3.5) {
        vec3 e;
        if (Role < 4.5) e = mix(vec3(0.5, 0.018, 0.004), vec3(1.7, 0.6, 0.13), pow(mu, 1.5)) * 1.8;
        else if (Role < 5.5) e = lin(VertexColor) * Emit * 2.2;
        else e = lin(VertexColor) * Emit * 2.6 * mix(1.0, 0.45, smoothstep(0.0015, 0.006, footprint));
        color = srgb(e * glow * breath);
    } else if (ice) {
        vec3 tc = hc / max(dot(hc, vec3(0.3, 0.5, 0.2)), 0.02) * lum;
        vec3 base = lin(clamp(mix(vec3(lum), tc, 0.3) * vec3(0.84, 0.95, 1.12), 0.0, 1.0)) * 0.9;
        float texel = max(length(dx), length(dy)) * float(textureSize(Sampler0, 0).x);
        vec3 a = cross(wy, n);
        vec3 b = cross(n, wx);
        float d = dot(wx, a);
        float k = length(wx) / max(length(objX), 1e-7);
        n = normalize(abs(d) * n - sign(d) * (hX * a + hY * b) * k * smoothstep(0.7, 1.4, texel));
        nl = max(dot(n, l), 0.0);
        vec3 col = base * (vec3(0.04, 0.046, 0.056) + vec3(0.08) * mu + vec3(1.0, 0.93, 0.84) * nl * (0.45 + 0.8 * Power));
        col += mix(vec3(0.04), base, Metal) * pow(max(dot(n, normalize(l + v)), 0.0), mix(8.0, 140.0, 1.0 - Rough)) * nl * (1.2 - 0.8 * Rough) * (0.4 + 0.6 * Power);
        col += vec3(0.06, 0.12, 0.2) * pow(1.0 - mu, 5.0) * (0.3 + 0.7 * smoothstep(-0.3, 0.4, dot(n, l)));
        col += vec3(0.08, 0.13, 0.22) * pow(1.0 - mu, 4.0) * smoothstep(0.0, 0.9, -dot(v, l));
        color = srgb(col);
    } else {
        vec3 on = normalize(ObjectNormal);
        if (!gl_FrontFacing) on = -on;
        vec3 ww = pow(abs(on), vec3(4.0));
        ww /= dot(ww, vec3(1.0));
        vec3 wq = ObjectPos / WEAR_TILE;
        vec3 wa = texture(Sampler2, wq.zy).rgb * ww.x + texture(Sampler2, wq.xz).rgb * ww.y + texture(Sampler2, wq.xy).rgb * ww.z;
        vec3 base = lin(VertexColor) * clamp(1.0 + (lin(wa) / WEAR_MEAN - 1.0) * WEAR_CONTRAST, 0.3, 1.7);
        vec3 nx = texture(Sampler3, wq.zy).xyz * 2.0 - 1.0;
        vec3 ny = texture(Sampler3, wq.xz).xyz * 2.0 - 1.0;
        vec3 nz = texture(Sampler3, wq.xy).xyz * 2.0 - 1.0;
        nx.xy *= WEAR_BUMP;
        ny.xy *= WEAR_BUMP;
        nz.xy *= WEAR_BUMP;
        vec3 bent = normalize(vec3(nx.xy + on.zy, on.x).zyx * ww.x + vec3(ny.xy + on.xz, on.y).xzy * ww.y + vec3(nz.xy + on.xy, on.z) * ww.z);
        n = normalize(mat3(ModelMat) * bent);
        nl = max(dot(n, l), 0.0);
        mu = max(dot(n, v), 0.0);
        vec2 cut = trench(ObjectPos, normalize(ObjectLight));
        float sun = nl * cut.y;
        vec3 col = base * ((vec3(0.15, 0.165, 0.195) + vec3(0.2) * mu) * cut.x + vec3(1.0, 0.93, 0.84) * sun * (0.45 + 0.8 * Power));
        float spec = pow(max(dot(n, normalize(l + v)), 0.0), mix(10.0, 90.0, 1.0 - Rough));
        col += mix(vec3(0.04), vec3(0.3, 0.32, 0.36), Metal) * spec * sun * (1.1 - 0.7 * Rough) * (0.4 + 0.6 * Power);
        float q = length(ObjectPos.yz - CORE.yz);
        if (q < 0.2) col *= 0.75 + 0.25 * smoothstep(0.04, 0.22, q);
        vec3 tc = CORE - ObjectPos;
        float d = length(tc);
        col += (base + 0.02) * vec3(1.1, 0.07, 0.015) * max(dot(normalize(ObjectNormal), tc / d), 0.0) * exp(-d * d / 0.02) * 2.4 * Power * breath;
        color = srgb(col);
    }

    if (sceneHandOccluded(gl_FragCoord.xy)) discard;

    // Round the interpolated depth once per pixel without pulling fragments closer than the raster depth.
    float depth = gl_DepthRange.far - 0.5 * gl_DepthRange.diff * ClipDepthFromFar * gl_FragCoord.w;
    gl_FragDepth = max(gl_FragCoord.z, depth);
    fragColor = vec4(color, 1.0);
}
