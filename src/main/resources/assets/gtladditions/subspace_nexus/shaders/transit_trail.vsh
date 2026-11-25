// A ship trail: the lane arc from the tail (quv.x 0) to the stern (1), widened across its direction on screen.
// A ribbon turned to the eye per vertex twists where the eye looks along it (right behind the stern) and the twist
// jumps from frame to frame; the screen direction of the projected arc cannot flip.
// iArc is radius, tail angle, stern angle and height. iTrail is world width and opacity.
attribute vec3 quv; // u, side and the baked longitudinal envelope
attribute vec4 iArc;
attribute vec2 iTrail;
uniform float uPix;
uniform vec3 uCam;
varying vec2 vUv;
varying float vEnv;
varying float vScaleAA;
flat varying float vAlpha;

float angleAt(float u) {
    return mix(iArc.y, iArc.z, clamp(u, 0.0, 1.0));
}

vec2 radialAt(float a) {
    return vec2(cos(a), sin(a));
}

vec4 clipAt(vec2 radial) {
    return projectionMatrix * viewMatrix * vec4(iArc.x * radial.x, iArc.x * radial.y, iArc.w, 1.0);
}

void main() {
    float u = quv.x;
    vec2 radial = radialAt(angleAt(u));
    float a0 = angleAt(u - 1.0 / 256.0), a1 = angleAt(u + 1.0 / 256.0);
    vec4 c = clipAt(radial);
    float aspect = projectionMatrix[1][1] / projectionMatrix[0][0];
    vec2 d;
    // From within the ribbon's height the projection can turn back on itself. Keep the original
    // clipped secant there, including its one-sided directions at the ends of the strip.
    if (abs(uCam.z - iArc.w) <= iTrail.x) {
        vec4 c0 = clipAt(radialAt(a0));
        vec4 c1 = clipAt(radialAt(a1));
        d = c1.xy / max(c1.w, 1e-3) * vec2(aspect, 1.0) - c0.xy / max(c0.w, 1e-3) * vec2(aspect, 1.0);
    } else {
        mat4 clip = projectionMatrix * viewMatrix;
        vec4 tangent = (clip[1] * radial.x - clip[0] * radial.y) * (iArc.x * (a1 - a0));
        float w = max(c.w, 1e-3);
        d = (tangent.xy - c.xy * (c.w > 1e-3 ? tangent.w / w : 0.0)) * vec2(aspect, 1.0) / w;
    }
    d = length(d) > 1e-7 ? normalize(d) : vec2(1.0, 0.0);

    // Half width in NDC: the world width at this depth, never under about 1.8 px.
    float world = 0.5 * iTrail.x * projectionMatrix[1][1] / max(c.w, 1e-3);
    float halfWidth = max(world, 0.9 * uPix * projectionMatrix[1][1]);
    c.xy += vec2(-d.y / aspect, d.x) * halfWidth * quv.y * c.w;

    vUv = quv.xy;
    vEnv = quv.z;
    vScaleAA = min(1.0, world / halfWidth);
    vAlpha = iTrail.y;
    gl_Position = c;
}
