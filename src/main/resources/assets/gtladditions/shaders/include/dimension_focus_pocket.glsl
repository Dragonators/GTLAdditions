// Frame: game axes, origin at the centre of the structure's centre block.
// NoiseTex, GasNear and GasDeep are a 3D texture and two cube maps bound by hand, so the shader json declares them as int uniforms.
#define PI 3.14159265359
#define TAU 6.28318530718

uniform sampler3D NoiseTex;
uniform samplerCube GasNear;
uniform samplerCube GasDeep;
uniform float Time;
uniform float Flow;
uniform float Spin;
uniform float Level;
uniform float Align;

const float POCKET_RADIUS = 20.8;
// Ramp centre and half width: near map in xy, deep map in zw.
const vec4 GAS_RAMP = vec4(0.664, 0.504, 0.626, 0.472);
const float GAS_DETAIL = 0.45;
const float GAS_NARROW = 0.81;
const float GAS_LIGHT = 1.2;
const vec3 SPECTRUM[6] = vec3[6](
    vec3(0.92, 0.28, 0.30), vec3(0.94, 0.64, 0.22), vec3(0.34, 0.82, 0.34),
    vec3(0.26, 0.84, 0.86), vec3(0.30, 0.40, 0.98), vec3(0.86, 0.32, 0.88)
);

vec3 ro, rd;
float rb, rc0;

mat3 rotY(float a) {
    float c = cos(a), s = sin(a);
    return mat3(c, 0.0, -s, 0.0, 1.0, 0.0, s, 0.0, c);
}

vec3 noise3(vec3 q) {
    return textureLod(NoiseTex, q * 0.25, 0.0).rgb;
}

void setRay(vec3 camera, vec3 point) {
    ro = camera;
    rd = normalize(point - camera);
    rb = dot(ro, rd);
    rc0 = dot(ro, ro);
}

vec3 hit(float radius) {
    float h = rb * rb - (rc0 - radius * radius);
    return normalize(ro + rd * (-rb + sqrt(max(h, 0.0))));
}

// Fade along the forward ray, as in the Heart volume: space behind the eye must not form a second opaque half.
// Clamping the closest point to the eye keeps the same soft boundary when looking out from inside the rim.
float softEdge() {
    vec3 closest = ro + rd * max(-rb, 0.0);
    float rc2 = dot(closest, closest);
    return smoothstep(0.04, 0.42, sqrt(max(1.0 - rc2 / (POCKET_RADIUS * POCKET_RADIUS), 0.0)));
}

// The bend depends only on the ray's line, keeping the pocket stable as the eye moves.
// Steeper bending would fold the sky over itself at the rim.
void lens() {
    vec3 c = ro - rd * rb;
    float rc = length(c), phi = 0.9 * rc / POCKET_RADIUS;
    if (rc > 1e-3) rd = normalize(rd * cos(phi) + c / rc * sin(phi));
    ro = c;
    rb = dot(ro, rd);
    rc0 = dot(ro, ro);
}

vec3 spectrum(float x) {
    x = fract(x) * 6.0;
    int i = int(x);
    float f = x - float(i);
    f = f * f * (3.0 - 2.0 * f);
    return mix(SPECTRUM[i], SPECTRUM[(i + 1) % 6], f);
}

float mercator(float y) {
    float l = abs(asin(clamp(y, -1.0, 1.0)));
    return log(tan(PI * 0.25 + min(l, 1.54) * 0.5));
}

void eternityField(vec3 d, out vec3 hue, out float arm) {
    vec3 n = noise3(d * 1.15 + 1.0);
    float w = (n.r - 0.5) * 3.2 + (n.g - 0.5) * 1.2, ps = mercator(d.y) * 2.2, lon = atan(d.x, d.z);
    float up = smoothstep(-0.12, 0.12, d.y + (n.b - 0.5) * 0.5), r = Flow * 0.3;
    float au = lon + r + ps + w, al = -lon + r + ps + w + 1.3, t = Time * 0.05;
    float pole = smoothstep(0.76, 0.97, abs(d.y));
    hue = mix(mix(spectrum(al / TAU + t + 0.4), spectrum(au / TAU + t), up), vec3(0.82, 0.78, 0.97), pole);
    float c = mix(0.5 + 0.5 * cos(3.0 * al), 0.5 + 0.5 * cos(3.0 * au), up);
    arm = mix(c * c * (3.0 - 2.0 * c), 0.8, pole);
}

vec3 azure(vec3 d) {
    return mix(vec3(0.035, 0.17, 0.33), vec3(0.015, 0.075, 0.19), smoothstep(0.0, 0.8, abs(d.y)));
}

// Fetch both hemisphere maps so derivatives stay valid across their seam.
vec3 gas(samplerCube map, vec3 d, float rate, float width, float seed) {
    vec3 sn = noise3(d * 2.7 + seed);
    float up = smoothstep(-width, width, d.y + (sn.r - 0.5) * 0.42 + (sn.g - 0.5) * 0.2);
    vec3 a = texture(map, rotY(Flow * rate + seed) * d).rgb;
    vec3 b = texture(map, rotY(-Flow * rate + seed + 1.9) * vec3(d.x, -d.y, d.z)).rgb;
    return mix(b, a, up);
}

// R is the soft base, G the detail octaves that continue it, B the baked light.
float density(vec3 s, float centre, float width) {
    float w = width * GAS_NARROW;
    return smoothstep(centre - w, centre + w, s.r + GAS_DETAIL * (s.g - 0.5));
}

vec3 pocketColor() {
    lens();
    float g = mix(0.5, 1.0, Spin) * Level;
    vec3 far = hit(3000.0);
    vec3 a = gas(GasNear, hit(46.0), 0.60, 0.16, 0.0);
    float near = density(a, GAS_RAMP.x, GAS_RAMP.y), light = 0.5 + (a.b - 0.5) * GAS_LIGHT;
    float mid = density(gas(GasDeep, hit(170.0), 0.22, 0.16, 2.1), GAS_RAMP.z, GAS_RAMP.w);
    float deep = density(gas(GasDeep, hit(1200.0), 0.07, 0.2, 4.3), GAS_RAMP.z, GAS_RAMP.w);
    vec3 hueNear, hueFar;
    float armNear, armFar;
    eternityField(hit(60.0), hueNear, armNear);
    eternityField(hit(400.0), hueFar, armFar);
    float pole = max(abs(far.y), 1e-4);
    vec3 col = azure(far) + vec3(0.80, 0.76, 0.98) * pow(pole, 28.0) * 0.44 + vec3(0.28, 0.32, 0.62) * pow(pole, 6.0) * 0.26;
    col = mix(col, mix(hueFar * 0.32, hueFar * 0.92, deep), smoothstep(0.15, 0.95, deep) * (0.25 + 0.75 * armFar) * 0.62);
    col = mix(col, mix(hueNear * 0.36, hueNear * 1.05, mid), smoothstep(0.12, 0.9, mid) * (0.2 + 0.8 * armNear) * 0.72);
    float cover = smoothstep(0.08, 0.92, near) * (0.16 + 0.84 * armNear) * 0.84;
    vec3 veil = mix(hueNear * 0.46, hueNear * 1.22, near) * mix(0.8, 1.18, light);
    veil = mix(veil, vec3(0.86, 0.82, 1.0), near * near * near * 0.4);
    col = mix(col, veil, cover) * (0.5 + 0.5 * g) * mix(0.92, 1.1, Align * Level) * 1.38;
    // Long gradients band in an 8-bit target without a little dither.
    float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715)))) - 0.5;
    return 1.0 - exp(-col * 1.3) + dither * 1.5 / 255.0;
}
