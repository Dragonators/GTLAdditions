// A quadratic ribbon from iA through iCtrl to iB, facing the eye.
// iProf: width, head power (negative: parallel sides with rounded ends), tail power, dark-cushion amount.
// iMeta: alpha, core sharpness (0 for none), wisp phase.
attribute vec2 quv;
attribute vec3 iA;
attribute vec3 iCtrl;
attribute vec3 iB;
attribute vec4 iProf;
attribute vec3 iColCore;
attribute vec3 iColMid;
attribute vec3 iColVoid;
attribute vec4 iMeta;
uniform vec3 uCam;
uniform float uPix;
varying vec2 vUv;
varying float vEnv;
varying float vScaleAA;
varying vec3 vCore;
varying vec3 vMid;
varying vec3 vVoid;
varying vec4 vMeta;

void main() {
    float u = clamp(quv.x, 0.0, 1.0);
    float om = 1.0 - u;
    vec3 P = om * om * iA + 2.0 * om * u * iCtrl + u * u * iB;
    vec3 tangent = 2.0 * om * (iCtrl - iA) + 2.0 * u * (iB - iCtrl);
    vec3 axis = length(tangent) > 1e-5 ? normalize(tangent) : vec3(1.0, 0.0, 0.0);
    vec3 side = cross(axis, normalize(uCam - P));
    side = length(side) > 1e-4 ? normalize(side) : vec3(0.0, 1.0, 0.0);

    float hp = abs(iProf.y);
    float tp = max(iProf.z, 0.08);
    float uPeak = hp / max(hp + tp, 1e-4);
    float peak = pow(max(uPeak, 1e-4), hp) * pow(max(1.0 - uPeak, 1e-4), tp);
    float envelope = clamp(pow(max(u, 0.0), hp) * pow(max(1.0 - u, 0.0), tp) / max(peak, 1e-5), 0.0, 1.0);
    float profile = iProf.y < 0.0
        ? smoothstep(0.0, 0.14, u) * smoothstep(1.0, 0.82, u) * 0.65 + 0.35 * pow(envelope, 0.35)
        : 0.06 + 0.94 * pow(envelope, 0.62);

    // Never thinner than about two pixels; the alpha thins instead.
    float width = max(iProf.x * profile, 1.8 * uPix * length(uCam - P));
    P += side * (quv.y * width * 0.5);

    vUv = vec2(u, quv.y);
    vEnv = pow(envelope, 0.78);
    vScaleAA = min(1.0, (iProf.x + 1e-4) / max(width, 1e-4));
    vCore = iColCore;
    vMid = iColMid;
    vVoid = iColVoid;
    vMeta = vec4(iMeta.x, iMeta.y, iMeta.z, iProf.w);
    gl_Position = projectionMatrix * viewMatrix * vec4(P, 1.0);
}
