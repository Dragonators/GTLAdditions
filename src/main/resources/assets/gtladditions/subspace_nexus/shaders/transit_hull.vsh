attribute vec3 aBase;
attribute vec3 aEmit;
attribute vec4 aMat;
attribute vec4 nexusM0;
attribute vec4 nexusM1;
attribute vec4 nexusM2;
attribute vec4 nexusM3;
// iDefA: tail and nose shift in hull lengths, the curve between them, heat per stretch.
// iDefB: girth at the tail and the nose, extra vertical thinning towards the nose.
// iStyle: base heat, charge-wave position along the hull (below -1.4 is off), opacity.
attribute vec4 iDefA;
attribute vec4 iDefB;
attribute vec4 iStyle;
varying vec3 vNw;
varying vec3 vW;
varying vec3 vO;
varying vec3 vNo;
varying mat3 vRot;
varying vec2 vUv;
varying vec3 vBase;
varying vec3 vEmit;
flat varying vec4 vMat;
varying float vHeat;
varying float vAlpha;
varying float vWave;

void main() {
    mat4 m = mat4(nexusM0, nexusM1, nexusM2, nexusM3);
    vec3 q = position;
    float u = clamp(q.x + 0.5, 0.0, 1.0);
    float curve = max(iDefA.z, 0.08);
    float s = pow(u, curve);
    float d = mix(iDefA.x, iDefA.y, s);
    float du = curve * pow(max(u, 1e-3), curve - 1.0);
    float sx = max(0.22, 1.0 + (iDefA.y - iDefA.x) * du);
    float syz = mix(iDefB.x, iDefB.y, smoothstep(0.0, 1.0, s));
    float sy = max(0.12, syz);
    float sz = max(0.08, syz * (1.0 - 0.45 * iDefB.z * smoothstep(0.0, 1.0, s)));

    vec4 w = m * vec4(q.x + d, q.y * sy, q.z * sz, 1.0);
    vW = w.xyz;
    vO = position;
    vNo = normal;
    vRot = mat3(m);
    vNw = normalize(mat3(m) * normalize(vec3(normal.x / sx, normal.y / sy, normal.z / sz)));
    vUv = uv;
    vBase = aBase;
    vEmit = aEmit;
    vMat = aMat;

    // Heat follows the real longitudinal stretch, not the rigid shift, so the unstretched part stays plain metal.
    float pull = max(0.0, abs(d) - min(abs(iDefA.x), abs(iDefA.y)));
    vHeat = clamp(iStyle.x + clamp((pull * 0.48 + max(0.0, sx - 1.0) * 0.32) * iDefA.w, 0.0, 1.0), 0.0, 1.0);
    vAlpha = iStyle.z;
    float wave = (position.x - iStyle.y) / 0.15;
    vWave = iStyle.y > -1.4 ? exp(-wave * wave) : 0.0;
    gl_Position = projectionMatrix * viewMatrix * w;
}
