#version 150

// Local coordinates are Z-up.
out vec4 fragColor;

vec3 srgb(vec3 c) {
    c = max(c, vec3(0.));
    return mix(c * 12.92, 1.055 * pow(c, vec3(1. / 2.4)) - .055, step(vec3(.0031308), c));
}

uniform vec3 cameraPosition;

in vec3 vLocal, vWorld;
uniform sampler2D uBase, uData, uNormal;
uniform vec3 uScale, uTint, uSclera, uAbyss;
uniform vec2 uGaze;
uniform float uSeat, uIrisScale, uIrisDepth, uDilate, uPupilDepth, uRelief, uAngularTime, uGlow, uRim;

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

const float IRIS_R = .47;

float h21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float vn(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3. - 2. * f);
    return mix(mix(h21(i), h21(i + vec2(1, 0)), f.x), mix(h21(i + vec2(0, 1)), h21(i + vec2(1, 1)), f.x), f.y);
}

float fbm(vec2 p) {
    float s = 0., a = .5;
    for (int i = 0; i < 5; i++) {
        s += a * vn(p);
        p = p * 2.03 + 17.1;
        a *= .5;
    }
    return s;
}

float seg(vec2 p, vec2 a, vec2 b) {
    vec2 ba = b - a;
    float h = clamp(dot(p - a, ba) / max(dot(ba, ba), 1e-5), 0., 1.);
    return length(p - a - ba * h);
}

// pd is in dome radii; the band stays fixed as the gaze moves the iris.
vec3 bandColor(vec2 pd, float diff, float breath) {
    float rp = length(pd), Ri = uIrisScale;
    float b = clamp((rp - Ri) / max(1. - Ri, .02), 0., 1.); // 0 at the iris edge, 1 at the rim
    vec3 deep = uTint * .05 * (.35 + .65 * diff);
    float glow = uGlow * breath;
    const float N = 28.;
    float a = (atan(pd.y, pd.x) + uAngularTime * .02) / 6.2831853 * N;
    float id = mod(floor(a), N), u = fract(a);
    float bandW = (1. - Ri) * uScale.x, arc = 6.2831853 * max(rp, Ri) * uScale.x / N;
    vec2 g = vec2((u - .5) * arc, (b - .5) * bandW) / (bandW * .62);
    float d = 1e3;
    for (int k = 0; k < 4; k++) {
        float h1 = h21(vec2(id, float(k) * 7.3)), h2 = h21(vec2(id * 1.7, float(k) * 3.1 + 9.));
        vec2 p0 = vec2(floor(h1 * 3.) - 1., floor(fract(h1 * 7.) * 3.) - 1.) * .42;
        vec2 p1 = vec2(floor(h2 * 3.) - 1., floor(fract(h2 * 7.) * 3.) - 1.) * .42;
        if (k == 3 && h1 < .5)
            p1 = p0;
        d = min(d, seg(g, p0, p1));
    }
    float frame = min(abs(b - .12), abs(b - .88)) * bandW;
    float flick = .75 + .25 * sin(uAngularTime * 1.3 + id * 2.1);
    vec3 c = uTint * .04 * (.35 + .65 * diff);
    c += uTint * glow * flick * (exp(-d / .05) * .9 + exp(-d / .16) * .18) * step(.16, b) * step(b, .84);
    return c + uTint * glow * exp(-frame / .025) * .5;
}

// The gaze moves the pupil and inner iris while the rim stays anchored, so no edge is ever exposed.
vec2 irisCoord(vec2 xy) {
    vec2 p = xy / uScale.xy;
    return (p - uGaze * (1. - smoothstep(.35, .98, length(p)))) / uIrisScale;
}

void main() {
    vec3 P = vWorld, V = normalize(cameraPosition - P);
    vec3 N = normalize(vLocal / uScale);
    float NV = max(dot(N, V), 1e-3);
    vec3 Rd = refract(-V, N, 1. / 1.38);
    vec3 C = vec3(0., 0., uSeat), ai = vec3(uScale.xy * .985, max(uScale.z * uIrisDepth, .05));
    vec3 o = (P - C) / ai, d = Rd / ai;
    float A = dot(d, d), B = dot(o, d), disc = B * B - A * (dot(o, o) - 1.), tHit = -1.;
    if (disc > 0.) {
        float t0 = (-B - sqrt(disc)) / A;
        if (t0 > 0.)
            tHit = t0;
    }
    bool onIris = tHit > 0.;
    if (!onIris)
        tHit = max((uSeat - P.z) / min(Rd.z, -1e-3), 0.);
    vec3 Q = P + Rd * tHit;
    vec2 ir = irisCoord(Q.xy);
    float tIr = length(ir);
    vec2 uv = .5 + IRIS_R * vec2(ir.x, -ir.y);
    vec4 base = texture(uBase, uv), dat = texture(uData, uv);
    vec3 nm = texture(uNormal, uv).xyz * 2. - 1.;
    float w = .12 * uDilate, h = .80 + .05 * (uDilate - 1.), sd = slit(ir, w, h);
    vec3 Ni = normalize((Q - C) / (ai * ai));
    vec3 Tg = normalize(vec3(1., 0., 0.) - Ni * Ni.x), Bg = cross(Ni, Tg);
    vec3 Nb = normalize(Ni * nm.z + (Tg * nm.x + Bg * nm.y) * uRelief);
    float enter1 = smoothstep(0., .8, dot(N, L1)), enter2 = smoothstep(0., .8, dot(N, L2));
    float diff = .12 + .80 * max(dot(Nb, L1), 0.) * enter1 + .22 * max(dot(Nb, L2), 0.) * enter2;
    float caustic =
        pow(max(dot(normalize(ir + 1e-4), -normalize(L1.xy)), 0.), 3.) * smoothstep(.35, .95, tIr) * (1. - smoothstep(.95, 1.02, tIr));
    // uRim: 0 dark limbus, 1 full-bleed iris, 2-5 smaller iris inside a band.
    float edge = smoothstep(.78, 1., tIr);
    float ao = mix(uRim < .5 ? .45 : .85, 1., (1. - smoothstep(.68, 1.02, tIr)));
    float outside = max(sd, 0.), collar = 1. - .6 * exp(-outside / .045), center = 1. - .35 * (1. - smoothstep(0., .22, tIr));
    float breath = .9 + .1 * sin(uAngularTime * .42);
    vec3 pigment = lin(base.rgb);
    pigment = max(mix(vec3(dot(pigment, vec3(.2126, .7152, .0722))), pigment, 1.25), 0.);
    if (uRim < .5)
        pigment *= 1. - .7 * smoothstep(.84, .99, tIr);
    else {
        float l = dot(pigment, vec3(.2126, .7152, .0722));
        pigment = mix(pigment, uTint * l * 1.15, edge * .6) * mix(1., .62, edge);
    }
    vec3 irisCol = pigment * diff * ao * collar * center + uTint * caustic * .16;
    float ring = .55 * exp(-pow((outside - .035) / .03, 2.));
    vec3 emis = uTint * (dat.g * collar * .32 + ring * .5) * uGlow * breath;
    vec3 scl = uRim < .5 ? uSclera * (.35 + .65 * diff) * ao : bandColor(Q.xy / uScale.xy, diff, breath);
    vec3 inside = mix(scl, irisCol + emis, base.a);
    if (onIris && sd < 0.) {
        float s = 0., depth = 0., stepL = .06;
        bool wall = false;
        vec3 X = Q;
        for (int i = 0; i < 48; i++) {
            s += stepL;
            X = Q + Rd * s;
            depth = Q.z - X.z;
            if (depth > uPupilDepth)
                break;
            if (slit(irisCoord(X.xy), w, h) > 0.) {
                wall = true;
                break;
            }
        }
        if (wall) {
            float lo = s - stepL, hi = s;
            for (int k = 0; k < 6; k++) {
                float m = .5 * (lo + hi);
                if (slit(irisCoord((Q + Rd * m).xy), w, h) > 0.)
                    hi = m;
                else
                    lo = m;
            }
            X = Q + Rd * hi;
            depth = Q.z - X.z;
        }
        float fall = exp(-depth * 2.2);
        vec3 cav = wall ? lin(base.rgb) * .14 * fall * fall + uTint * .35 * exp(-depth * 14.) * uGlow
                        : uAbyss * (.010 + .008 * sin(uAngularTime * .6 + X.x * 1.7 + X.y * 1.3)) * uGlow;
        inside = mix(inside, cav, (1. - smoothstep(-.012, 0., sd)));
    }
    inside *= mix(uRim < .5 ? .35 : .75, 1., smoothstep(0., .3, vLocal.z));
    float lx = vLocal.x, phi = atan(vLocal.z, vLocal.y);
    float lidShade = smoothstep(0., .18, min(phi - lidU(lx), (PI - lidL(lx)) - phi));
    inside *= mix(.08, 1., lidShade);
    float F = .04 + .96 * pow(1. - NV, 5.);
    vec3 R = reflect(-V, N);
    float sheen = pow(max(dot(N, normalize(L1 + V)), 0.), 60.) * .10;
    vec3 col = inside * (1. - F) + (env(R, 1.) * F + vec3(1., .96, .9) * sheen) * mix(.35, 1., lidShade);
    fragColor = vec4(srgb(col), 1.);
}
