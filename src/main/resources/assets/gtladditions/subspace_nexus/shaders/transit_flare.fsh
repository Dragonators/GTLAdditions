uniform float uGlow;
varying vec2 vUv;
varying vec4 vSize;
uniform vec3 uCore;
varying vec3 vRim;
uniform vec3 uVoid;
varying vec4 vMeta;

// Premultiplied: the dark cushion dims a bright sky first, so the thin hues keep their colour in daylight.
void main() {
    float alpha = vMeta.x * uGlow;
    vec3 emit;
    float occlusion;
    if (vSize.w < 1.5) {
        float r = length(vUv);
        if (r > 1.0) discard;
        float edge = (1.0 - smoothstep(0.74,1.0,r));
        float d = (r - vSize.z) / max(0.025, vMeta.w);
        float crescent = pow(max(0.0, (r > 0.0 ? vUv.x / r : 0.0)), 5.5);
        float arc = exp(-d * d * 2.4) * crescent * edge;
        float core = exp(-d * d * 11.0) * crescent * edge;
        float cushion = clamp(alpha * vMeta.y * exp(-r * r * 3.2) * edge + alpha * 0.42 * arc, 0.0, 0.76);
        emit = uVoid * cushion + vRim * (alpha * arc * 0.95) + uCore * (alpha * core * 0.82);
        occlusion = clamp(cushion + alpha * arc * 0.95 * 0.22, 0.0, 0.82);
    } else {
        float r2 = dot(vUv, vUv);
        if (r2 > 1.0) discard;
        float edge = (1.0 - smoothstep(0.70,1.0,sqrt(r2)));
        float halo = exp(-r2 * 7.2) * edge;
        vec2 glintAxis = vUv * vec2(3.0, 1.0 / 0.038);
        float glint = exp(-dot(glintAxis, glintAxis)) * vMeta.w;
        float cushion = clamp(alpha * vMeta.y * exp(-r2 * 4.5) * edge, 0.0, 0.48);
        emit = uVoid * cushion + vRim * (alpha * halo * 0.54) + uCore * (alpha * (exp(-r2 * 40.0) * 0.98 + glint * 0.52));
        occlusion = clamp(cushion + alpha * halo * 0.54 * 0.14, 0.0, 0.56);
    }

    if (occlusion < 0.001 && dot(emit, vec3(1.0)) < 0.001) discard;
    gl_FragColor = vec4(emit, occlusion);
}
