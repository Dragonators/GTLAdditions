#version 150

// Z-up model coordinates; screen-blended emitted light.
in vec3 Position;
uniform mat4 ModelViewMat, ProjMat;

in vec4 iA, iB, iC, iD;
uniform float uZ, ViewportHeight, uRot0, uRot1, uRot2, uRot3;
uniform vec3 uW0, uW1, uW2;
out vec2 vLocal, vWorld;
out vec4 vA, vB, vC, vD;
out float vSeed;

vec2 rot(vec2 p, float a) {
    float c = cos(a), s = sin(a);
    return vec2(c * p.x - s * p.y, s * p.x + c * p.y);
}

void main() {
    int g = int(iC.z + .5);
    float ang = g == 0 ? uRot0 : g == 1 ? uRot1 : g == 2 ? uRot2 : uRot3;
    float z = uZ;
    int w = int(iC.x + .5);
    vec3 W = w == 0 ? uW0 : w == 1 ? uW1 : uW2;
    vec2 q = Position.xy, center, local;
    int type = int(iD.z + .5);
    if (type == 0) {
        center = (iA.xy + iA.zw) * .5;
    } else if (type == 1) {
        float m = iB.z + iB.w * .5;
        center = iA.xy + iA.z * vec2(cos(m), sin(m));
    } else
        center = iA.xy;
    vec4 cv = ModelViewMat * vec4(rot(center, ang), z, 1.);
    float depth = ProjMat[3][3] > .5 ? 1. : max(-cv.z, .1);
    float pix = 2. * depth / (max(ViewportHeight, 1.) * abs(ProjMat[1][1]));
    float M = max(W.z, 2.5 * pix) + W.x;
    if (type == 0) {
        vec2 d = iA.zw - iA.xy;
        float L = length(d);
        vec2 dir = L > 1e-5 ? d / L : vec2(1., 0.), n = vec2(-dir.y, dir.x);
        local = center + dir * (q.x * (L * .5 + M)) + n * (q.y * M);
    } else if (type == 1) {
        float h = iB.w * .5, m = iB.z + h;
        vec2 u = vec2(cos(m), sin(m)), v = vec2(-u.y, u.x);
        float rmin = max(iA.z * cos(h) - M, 0.), rmax = iA.z + M, tmax = (iA.z + M) * sin(h) + M;
        local = iA.xy + u * mix(rmin, rmax, q.y * .5 + .5) + v * (q.x * tmax);
    } else {
        vec2 ax = vec2(cos(iA.z), sin(iA.z)), ay = vec2(-ax.y, ax.x);
        local = iA.xy + ax * (q.x * (iA.w + M)) + ay * (q.y * (iB.x + M));
    }
    vLocal = local;
    vWorld = rot(local, ang);
    vA = iA;
    vB = iB;
    vC = iC;
    vD = iD;
    vSeed = fract(sin(dot(iA.xy, vec2(12.9898, 78.233))) * 43758.5453);
    gl_Position = ProjMat * ModelViewMat * vec4(vWorld, z, 1.);
}
