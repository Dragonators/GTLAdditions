// iSize: half extents along iRight and iUp, ring radius (mode 1), mode (1 shock crescent across the course, 2 view-facing glare).
// iMeta: alpha, dark-cushion depth, rotation in the plane (mode 1), ring width (mode 1) or cross-glint strength (mode 2).
attribute vec3 iCenter;
attribute vec3 iRight;
attribute vec3 iUp;
attribute vec4 iSize;
attribute vec3 iColRim;
attribute vec4 iMeta;
uniform vec3 uCam;
varying vec2 vUv;
varying vec4 vSize;
varying vec3 vRim;
varying vec4 vMeta;

void main() {
    vec2 q = position.xy * 2.0;
    vec3 P;
    vec3 V = normalize(uCam - iCenter);
    if (iSize.w > 1.5) {
        vec3 tangent = iRight - V * dot(iRight, V);
        float tangentLength = length(tangent);
        vec3 e1 = tangentLength > 0.15 ? tangent / tangentLength : normalize(cross(V, vec3(0.0, 0.0, 1.0)));
        vec3 e2 = normalize(cross(V, e1));
        P = iCenter + e1 * (q.x * iSize.x) + e2 * (q.y * iSize.y);
    } else {
        vec3 R = normalize(iRight);
        vec3 U = normalize(iUp);
        vec3 N = normalize(cross(R, U));
        float facing = dot(N, V);
        // Seen edge-on the crescent would vanish; lean the plane towards the eye instead.
        if (abs(facing) < 0.22) {
            vec3 N2 = normalize(N * (facing >= 0.0 ? 1.0 : -1.0) * 0.94 + V * 0.31);
            U = normalize(U - N2 * dot(U, N2));
            R = normalize(cross(U, N2));
        }
        float c = cos(iMeta.z), s = sin(iMeta.z);
        P = iCenter + (R * c + U * s) * (q.x * iSize.x) + (U * c - R * s) * (q.y * iSize.y);
    }

    vUv = q;
    vSize = iSize;
    vRim = iColRim;
    vMeta = iMeta;
    gl_Position = projectionMatrix * viewMatrix * vec4(P, 1.0);
}
