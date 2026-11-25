uniform float uGlow;
varying vec2 vUv;
varying float vEnv;
varying float vScaleAA;
varying vec3 vCore;
varying vec3 vMid;
varying vec3 vVoid;
varying vec4 vMeta;

void main() {
    float u = vUv.x;
    float r = abs(vUv.y);
    if (vEnv <= 0.0005 || r >= 1.0) discard;
    float env = vEnv * smoothstep(0.0, 0.08, u) * smoothstep(1.0, 0.90, u);
    float phase = vMeta.z;
    float wisp = 0.84 + 0.10 * sin(u * 14.0 - phase) + 0.06 * sin(u * 25.0 + phase * 1.4) * cos(vUv.y * 2.2);
    float edge = smoothstep(1.0, 0.68, r);
    float a = vMeta.x * env * uGlow;
    float cushion = clamp(a * vMeta.w * exp(-r * r * 2.1) * edge, 0.0, 0.82);
    float mist = a * exp(-r * r * 4.2) * edge * wisp;
    float core = vMeta.y > 0.5 ? a * pow(max(env, 0.0), 0.70) * exp(-r * r * vMeta.y) * vScaleAA : 0.0;
    vec3 emit = vVoid * cushion + vMid * (mist * 0.42) + vCore * (core * 0.88);
    float occlusion = clamp(cushion + mist * 0.24 + core * 0.22, 0.0, 0.88);
    if (occlusion < 0.001 && dot(emit, vec3(1.0)) < 0.001) discard;
    gl_FragColor = vec4(emit, occlusion);
}
