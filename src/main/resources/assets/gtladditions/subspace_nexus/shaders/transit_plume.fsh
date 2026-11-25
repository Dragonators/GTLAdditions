uniform float uGlow;
varying vec2 vUv;
varying float vEnv;
varying float vScaleAA;
uniform vec3 uCore;
uniform vec3 uMid;
uniform vec3 uVoid;
flat varying float vAlpha;

void main() {
    float u = vUv.x;
    float r = abs(vUv.y);
    if (vEnv <= 0.0005 || r >= 1.0) discard;
    float env = vEnv * smoothstep(0.0, 0.08, u) * smoothstep(1.0, 0.90, u);
    float wisp = 0.84 + 0.10 * sin(u * 14.0) + 0.06 * sin(u * 25.0) * cos(vUv.y * 2.2);
    float edge = smoothstep(1.0, 0.68, r);
    float a = vAlpha * env * uGlow;
    float skirt = exp(-r * r * 2.1);
    float cushion = clamp(a * 0.4 * skirt * edge, 0.0, 0.82);
    float mist = a * (skirt * skirt) * edge * wisp;
    float core = a * pow(env, 0.70) * exp(-r * r * 30.0) * vScaleAA;
    vec3 emit = uVoid * cushion + uMid * (mist * 0.42) + uCore * (core * 0.88);
    float occlusion = clamp(cushion + mist * 0.24 + core * 0.22, 0.0, 0.88);
    if (occlusion < 0.001 && dot(emit, vec3(1.0)) < 0.001) discard;
    gl_FragColor = vec4(emit, occlusion);
}
