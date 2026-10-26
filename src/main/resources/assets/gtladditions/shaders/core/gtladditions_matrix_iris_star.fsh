#version 150

// Z-up model coordinates; screen-blended emitted light.
out vec4 fragColor;

in vec2 vLocal, vWorld;
in vec4 vA, vB, vC, vD;
in float vSeed;
uniform vec3 uW0, uW1, uW2, uPrimary, uSecondary;
uniform vec2 uGazeDir;
uniform float uTime, uLight, uIdle, uReveal, uFront, uGazeAmt, uOpen, uBright, uPhase;
const float PI = 3.14159265, TAU = 6.2831853;

float profile(float d, vec3 W, float pix) {
    vec3 I = vec3(.7, .3, .085), e;
    float s = 0.;
    for (int k = 0; k < 3; k++) {
        float w = W[k], we = max(w, pix * .85), x = d / we, f = max(0., 1. - x * x);
        s += I[k] * (w / we) * f * f;
    }
    return s;
}

float sdSeg(vec2 p, vec2 a, vec2 b, out float t) {
    vec2 pa = p - a, ba = b - a;
    float L2 = dot(ba, ba);
    t = L2 > 1e-8 ? clamp(dot(pa, ba) / L2, 0., 1.) : 0.;
    return length(pa - ba * t);
}

// Horizontal almond, tips at (+-b,0), half height a.
float sdAlmond(vec2 p, float b, float a) {
    float d = (b * b - a * a) / (2. * a), r = d + a;
    vec2 P = abs(p).yx;
    return ((P.y - b) * d > P.x * b) ? length(P - vec2(0., b)) : length(P - vec2(-d, 0.)) - r;
}

void main() {
    int w = int(vC.x + .5);
    vec3 W = w == 0 ? uW0 : w == 1 ? uW1 : uW2;
    float pix = max(length(fwidth(vLocal)) * .7, 1e-5);
    int type = int(vD.z + .5);
    float e = 0., param = 0., t = 0., inten = vD.x, flowAng = 0.;
    float rr = length(vWorld);
    float lit = (1. - smoothstep(uReveal - 1.5, uReveal + 1.5, rr));
    if (type == 0) {
        float d = sdSeg(vLocal, vA.xy, vA.zw, t);
        param = t * length(vA.zw - vA.xy);
        inten = mix(vD.x, vD.y, t);
        e = profile(d, W, pix);
    } else if (type == 1) {
        vec2 rel = vLocal - vA.xy;
        float ang = atan(rel.y, rel.x), len = length(rel), span = vB.y;
        float s = mod(ang - vB.x, TAU), ps = vB.z - vB.x, pe = ps + vB.w;
        bool last = pe >= span - 1e-4, first = ps <= 1e-4;
        float d;
        if (s <= span) {
            if (s < ps || (s >= pe && !last))
                discard;
            d = abs(len - vA.z);
        } else {
            bool beyond = (s - span) < (TAU - span) * .5;
            if (beyond ? !last : !first)
                discard;
            float ea = beyond ? vB.x + span : vB.x;
            d = length(rel - vA.z * vec2(cos(ea), sin(ea)));
        }
        param = min(s, span) * vA.z;
        inten = mix(vD.x, vD.y, clamp(s / span, 0., 1.));
        flowAng = ang;
        e = profile(d, W, pix);
    } else {
        vec2 rel = vLocal - vA.xy, ax = vec2(cos(vA.z), sin(vA.z));
        vec2 q = vec2(dot(rel, ax), dot(rel, vec2(-ax.y, ax.x)));
        float open = clamp(uOpen * lit, 0., 1.);
        float a = max(vB.x * open, .035 * vA.w);
        float sd = sdAlmond(q, vA.w, a);
        float inside = (1. - smoothstep(-pix, pix, sd));
        float ir = vB.y * clamp(open * 1.25, 0., 1.);
        float iris = profile(abs(length(q) - ir), uW0, pix) * inside;
        float slit = (1. - smoothstep(-pix, pix, sdAlmond(q.yx, ir * .92, max(vB.z * open, .01)))) * inside;
        e = profile(abs(sd), W, pix) + iris * .9 + slit * .75 * open;
    }
    float level = uIdle + (1. - uIdle) * lit * uLight;
    float front = uFront * exp(-pow((rr - uReveal) / 1.4, 2.));
    float gz = uGazeAmt * pow(max(dot(normalize(vWorld + 1e-4), uGazeDir), 0.), 8.) * .9;
    float flick = vD.w > .5 ? .8 + .2 * sin(uTime * 1.3 + vSeed * TAU) : 1.;
    float flow = 1.;
    int fm = int(vC.w + .5);
    if (fm == 1) {
        float dl = mod(flowAng - uPhase + PI * .5, PI) - PI * .5;
        float ext = dl > 0. ? .10 : .55;
        flow = .85 + .35 * (1. - smoothstep(0., ext, abs(dl)));
    } else if (fm >= 2) {
        float x = param / 7. - uTime * 1.1 * (fm == 2 ? 1. : -1.);
        flow = .7 + 1.1 * exp(-pow((fract(x) - .5) / .07, 2.)) * uLight;
    }
    vec3 col = mix(uPrimary, uSecondary, vC.y);
    vec3 c = col * e * inten * (level * (1. + gz) * flick * flow + front * 1.3) * uBright;
    fragColor = vec4(min(c, vec3(1.)), 1.);
}
