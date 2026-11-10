#version 150

// Local coordinates are Z-up.
out vec4 fragColor;

vec3 srgb(vec3 c) {
    c = max(c, vec3(0.));
    return mix(c * 12.92, 1.055 * pow(c, vec3(1. / 2.4)) - .055, step(vec3(.0031308), c));
}

uniform vec3 cameraPosition;

in vec3 vLocal, vWorld;
uniform sampler2D uBase;
uniform vec3 uScale, uEyeScale, uTint, uSkin;
uniform vec2 uGaze;
uniform float uIrisScale, uDilate, uTime, uGlow, uSeam;

const float PI = 3.14159265;
const vec3 L1 = vec3(-.3437, -.4296, .8594), L2 = vec3(.7372, .3686, .5660);
uniform vec3 uCamLight;
uniform float uLidU, uLidL, uEnvGlow;

vec3 lin(vec3 c) {
    return mix(c / 12.92, pow((c + .055) / 1.055, vec3(2.4)), step(vec3(.04045), c));
}

// w and h are the slit's width and height in iris radii.
float slit(vec2 p, float w, float h) {
    float r = (h * h / w + w) * .5, d = r - w;
    return max(length(p - vec2(d, 0.)) - r, length(p + vec2(d, 0.)) - r);
}

// Light-window direction l and half size s.
float softbox(vec3 r, vec3 l, vec2 s) {
    float c = dot(r, l);
    if (c <= 0.)
        return 0.;
    vec3 u = normalize(cross(l, vec3(0., 0., 1.))), v = cross(u, l);
    vec2 q = vec2(dot(r, u), dot(r, v)) / c, e = s * .45;
    float box = (1. - smoothstep(s.x - e.x, s.x + e.x, abs(q.x))) * (1. - smoothstep(s.y - e.y, s.y + e.y, abs(q.y)));
    return box + .06 * exp(-length(q / s) * .9);
}

// k widens and dims the light windows for rougher surfaces (1 = glossy cornea).
vec3 env(vec3 r, float k) {
    vec3 c = mix(vec3(.006, .006, .008), vec3(.30, .31, .34), smoothstep(.08, .9, r.z));
    c += vec3(1., .98, .95) * 14. / (k * k) * softbox(r, uCamLight, vec2(.07, .04) * k);
    c += vec3(1., .96, .9) * 18. / (k * k) * softbox(r, L1, vec2(.055, .032) * k);
    c += vec3(.78, .85, 1.) * 5. / (k * k) * softbox(r, L2, vec2(.035, .020) * k);
    c += uTint * uEnvGlow * (1. - smoothstep(-.05, .35, r.z));
    return c;
}

// Lid free edges as polar angles about the x axis of the unit dome: phi=atan(z,y) runs from the +y rim (0) over the top to the -y rim (PI).
// The upper lid covers phi<lidU(x), the lower lid covers phi>PI-lidL(x); both edges meet at the corners x=+-1.
float lidU(float x) {
    return uLidU + .05 * sin(x * 2.3 + .7) * (1. - x * x);
}

float lidL(float x) {
    return uLidL + .04 * sin(x * 2.9 - 1.1) * (1. - x * x);
}

vec2 irisCoord(vec2 xy) {
    vec2 p = xy / uEyeScale.xy;
    return (p - uGaze * (1. - smoothstep(.35, .98, length(p)))) / uIrisScale;
}

float hash3(vec3 p) {
    p = fract(p * .3183099 + .1);
    p *= 17.;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float vnoise(vec3 x) {
    vec3 i = floor(x), f = fract(x);
    f = f * f * (3. - 2. * f);
    return mix(mix(mix(hash3(i), hash3(i + vec3(1, 0, 0)), f.x), mix(hash3(i + vec3(0, 1, 0)), hash3(i + vec3(1, 1, 0)), f.x), f.y),
        mix(mix(hash3(i + vec3(0, 0, 1)), hash3(i + vec3(1, 0, 1)), f.x), mix(hash3(i + vec3(0, 1, 1)), hash3(i + vec3(1, 1, 1)), f.x),
            f.y),
        f.z);
}

void main() {
    float x = vLocal.x, phi = atan(vLocal.z, vLocal.y);
    float aU = lidU(x), aL = PI - lidL(x);
    float cover = max(aU - phi, phi - aL);
    if (cover <= 0.)
        discard;
    bool up = phi < aU;
    float s = up ? aU - phi : phi - aL;
    // 1 = shut, 0 = parted; continuity prevents a twitch from stepping along x.
    float shut = 1. - smoothstep(-.02, .02, aL - aU);
    float lipS = up ? s : mix(s, 1., shut);
    float tuck = up ? 1. : mix(1., mix(.25, 1., smoothstep(0., .07, phi - aU)), shut);
    vec3 N = normalize(vLocal / uScale);
    vec3 tPhi = normalize(vec3(0., -sin(phi) * uScale.y, cos(phi) * uScale.z));
    vec3 toEdge = up ? tPhi : -tPhi;
    float folds = sin(s * 30. + 3. * vnoise(vec3(x * 3., s * 5., 1.7))) * exp(-s / .35);
    float grain = vnoise(vWorld * 2.1), skin = grain * .65 + vnoise(vWorld * 5.7) * .35;
    float hgt = folds * .05 + grain * .012;
    vec3 dpx = dFdx(vWorld), dpy = dFdy(vWorld);
    float hx = dFdx(hgt), hy = dFdy(hgt);
    vec3 r1 = cross(dpy, N), r2 = cross(N, dpx);
    float det = dot(dpx, r1);
    N = normalize(abs(det) * N - sign(det) * (hx * r1 + hy * r2));
    float lip = 1. - smoothstep(0., .08, lipS);
    N = normalize(N + toEdge * lip * lip * 1.8);
    vec3 V = normalize(cameraPosition - vWorld);
    float NV = max(dot(N, V), 1e-3);
    float diff = .10 + .70 * max(dot(N, L1), 0.) + .20 * max(dot(N, L2), 0.);
    vec3 col = uSkin * (.5 + .5 * skin) * diff * (1. - .6 * lip) * tuck;
    vec2 ir = irisCoord(vWorld.xy);
    vec4 ib = texture(uBase, .5 + .47 * vec2(ir.x, -ir.y));
    float w = .12 * uDilate, h = .80 + .05 * (uDilate - 1.);
    float pupil = (1. - smoothstep(-.03, 0., slit(ir, w, h)));
    float thin = .02 + .07 * exp(-lipS / .15);
    col += lin(ib.rgb) * ib.a * (1. - pupil) * thin * (.25 + .75 * uGlow) * tuck;
    float seamD = abs(phi - aU);
    float along = (1. - smoothstep(.5, 1., abs(x))) * (.65 + .35 * vnoise(vec3(x * 7., uTime * .6, 3.1)));
    vec3 emis = uTint * uSeam * along * shut * (exp(-seamD / .010) * 1.6 + exp(-seamD / .06) * .22);
    emis += uTint * uGlow * exp(-s / .03) * .35 * (1. - shut);
    float F = .03 + .97 * pow(1. - NV, 5.);
    vec3 R = reflect(-V, N);
    col = col * (1. - F) + env(R, 3.) * F * .4 * tuck + emis;
    fragColor = vec4(srgb(col), 1.);
}
