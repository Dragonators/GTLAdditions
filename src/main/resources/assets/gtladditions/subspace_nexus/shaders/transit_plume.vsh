// A parallel-sided engine wake with rounded ends. iPlume is world width and opacity.
attribute vec4 quv; // u, side, baked envelope and width profile
attribute vec3 iA;
attribute vec3 iB;
attribute vec2 iPlume;
uniform vec3 uCam;
uniform float uPix;
varying vec2 vUv;
varying float vEnv;
varying float vScaleAA;
flat varying float vAlpha;

void main() {
    float u = clamp(quv.x, 0.0, 1.0);
    vec3 P = mix(iA, iB, u);
    vec3 tangent = iB - iA;
    vec3 axis = dot(tangent, tangent) > 1e-10 ? normalize(tangent) : vec3(1.0, 0.0, 0.0);
    vec3 eye = uCam - P;
    float distanceToEye = length(eye);
    vec3 side = cross(axis, eye / distanceToEye);
    side = dot(side, side) > 1e-8 ? normalize(side) : vec3(0.0, 1.0, 0.0);

    // Never thinner than about two pixels; the alpha thins instead.
    float width = max(iPlume.x * quv.w, 1.8 * uPix * distanceToEye);
    P += side * (quv.y * width * 0.5);

    vUv = vec2(u, quv.y);
    vEnv = quv.z;
    vScaleAA = min(1.0, (iPlume.x + 1e-4) / max(width, 1e-4));
    vAlpha = iPlume.y;
    gl_Position = projectionMatrix * viewMatrix * vec4(P, 1.0);
}
