attribute vec3 aBase;
attribute vec3 aEmit;
attribute vec4 aMat;
attribute vec4 nexusM0;
attribute vec4 nexusM1;
attribute vec4 nexusM2;
attribute vec4 nexusM3;
// Tail/nose shifts and girths; style carries base heat, charge-wave position, opacity and heat per stretch.
attribute vec4 iDeform;
attribute vec4 iStyle;
varying vec3 vNw;
varying vec3 vW;
varying vec3 vO;
varying vec3 vNo;
flat varying mat3 vRot;
varying vec2 vUv;
varying vec3 vBase;
varying vec3 vEmit;
flat varying vec4 vMat;
varying float vHeat;
flat varying float vAlpha;
// The fading hull's two programs must agree under LEQUAL. Solid hulls draw in one pass.
#ifdef TRANSIT_DEPTH_MATCH
invariant gl_Position;
#endif
varying float vWave;

void main() {
    mat4 m = mat4(nexusM0, nexusM1, nexusM2, nexusM3);
    vec3 q = position;
    float u = clamp(q.x + 0.5, 0.0, 1.0);
    float d = mix(iDeform.x, iDeform.y, u);
    float sx = max(0.22, 1.0 + (iDeform.y - iDeform.x));
    float girth = mix(iDeform.z, iDeform.w, smoothstep(0.0, 1.0, u));
    float sy = max(0.12, girth);
    float sz = max(0.08, girth);

    vec4 w = m * vec4(q.x + d, q.y * sy, q.z * sz, 1.0);
    vW = w.xyz;
    vO = position;
    vNo = normal;
    vRot = mat3(m) * inversesqrt(dot(nexusM0.xyz, nexusM0.xyz));
    vNw = normalize(mat3(m) * vec3(normal.x / sx, normal.y / sy, normal.z / sz));
    vUv = uv;
    vBase = aBase;
    vEmit = aEmit;
    vMat = aMat;

    // Heat follows the real longitudinal stretch, not the rigid shift, so the unstretched part stays plain metal.
    float pull = max(0.0, abs(d) - min(abs(iDeform.x), abs(iDeform.y)));
    vHeat = clamp(iStyle.x + clamp((pull * 0.48 + max(0.0, sx - 1.0) * 0.32) * iStyle.w, 0.0, 1.0), 0.0, 1.0);
    vAlpha = iStyle.z;
    float wave = (position.x - iStyle.y) / 0.15;
    vWave = iStyle.y > -1.4 ? exp(-wave * wave) : 0.0;
    gl_Position = projectionMatrix * viewMatrix * w;
}
