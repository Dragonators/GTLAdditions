// A ship trail: the lane arc from the tail (quv.x 0) to the stern (1), widened across its direction on screen.
// A ribbon turned to the eye per vertex twists where the eye looks along it (right behind the stern) and the twist
// jumps from frame to frame; the screen direction of the projected arc cannot flip.
// iArc: radius, tail angle, stern angle, height. iProf: width in blocks, head power, tail power, dark-cushion amount.
// iMeta: alpha, core sharpness.
attribute vec2 quv;
attribute vec4 iArc;
attribute vec4 iProf;
attribute vec3 iColCore;
attribute vec3 iColMid;
attribute vec3 iColVoid;
attribute vec4 iMeta;
uniform float uPix;
varying vec2 vUv;
varying float vEnv;
varying float vScaleAA;
varying vec3 vCore;
varying vec3 vMid;
varying vec3 vVoid;
varying vec4 vMeta;

vec4 clipAt(float u) {
    float a = mix(iArc.y, iArc.z, clamp(u, 0.0, 1.0));
    return projectionMatrix * viewMatrix * vec4(iArc.x * cos(a), iArc.x * sin(a), iArc.w, 1.0);
}

void main() {
    float u = quv.x;
    vec4 c = clipAt(u);
    vec4 c0 = clipAt(u - 1.0 / 256.0);
    vec4 c1 = clipAt(u + 1.0 / 256.0);
    // Screen-proportional coordinates: (ndc.x * aspect, ndc.y).
    float aspect = projectionMatrix[1][1] / projectionMatrix[0][0];
    vec2 d = c1.xy / max(c1.w, 1e-3) * vec2(aspect, 1.0) - c0.xy / max(c0.w, 1e-3) * vec2(aspect, 1.0);
    d = length(d) > 1e-7 ? normalize(d) : vec2(1.0, 0.0);

    float hp = abs(iProf.y);
    float tp = max(iProf.z, 0.08);
    float uPeak = hp / (hp + tp);
    float peak = pow(uPeak, hp) * pow(1.0 - uPeak, tp);
    float envelope = clamp(pow(max(u, 0.0), hp) * pow(max(1.0 - u, 0.0), tp) / max(peak, 1e-5), 0.0, 1.0);

    // Half width in NDC: the world width at this depth, never under about 1.8 px.
    float world = 0.5 * iProf.x * projectionMatrix[1][1] / max(c.w, 1e-3);
    float halfWidth = max(world, 0.9 * uPix * projectionMatrix[1][1]);
    c.xy += vec2(-d.y / aspect, d.x) * halfWidth * quv.y * c.w;

    vUv = quv;
    vEnv = pow(envelope, 0.78);
    vScaleAA = min(1.0, world / halfWidth);
    vCore = iColCore;
    vMid = iColMid;
    vVoid = iColVoid;
    vMeta = vec4(iMeta.x, iMeta.y, 0.0, iProf.w);
    gl_Position = c;
}
