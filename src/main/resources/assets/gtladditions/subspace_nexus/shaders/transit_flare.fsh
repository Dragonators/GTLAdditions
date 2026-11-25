uniform float uGlow;
varying vec2 vUv;
varying vec4 vSize;
varying vec3 vCore;
varying vec3 vRim;
varying vec3 vVoid;
varying vec4 vMeta;

// Premultiplied: the dark cushion dims a bright sky first, so the thin hues keep their colour in daylight.
void main() {
    float alpha = vMeta.x * uGlow;
    vec3 emit;
    float occlusion;
    if (vSize.w < 1.5) {
        float r = length(vUv);
        if (r > 1.0) discard;
        float edge = smoothstep(1.0, 0.74, r);
        float d = (r - vSize.z) / max(0.025, vMeta.w);
        float crescent = pow(max(0.0, cos(atan(vUv.y, vUv.x))), 5.5);
        float arc = exp(-d * d * 2.4) * crescent * edge;
        float core = exp(-d * d * 11.0) * crescent * edge;
        float cushion = clamp(alpha * vMeta.y * exp(-r * r * 3.2) * edge + alpha * 0.42 * arc, 0.0, 0.76);
        emit = vVoid * cushion + vRim * (alpha * arc * 0.95) + vCore * (alpha * core * 0.82);
        occlusion = clamp(cushion + alpha * arc * 0.95 * 0.22, 0.0, 0.82);
    } else {
        float r2 = dot(vUv, vUv);
        if (r2 > 1.0) discard;
        float edge = smoothstep(1.0, 0.70, sqrt(r2));
        float halo = exp(-r2 * 7.2) * edge;
        float glint = exp(-(vUv.y / 0.038) * (vUv.y / 0.038)) * exp(-vUv.x * vUv.x * 9.0) * vMeta.w;
        float cushion = clamp(alpha * vMeta.y * exp(-r2 * 4.5) * edge, 0.0, 0.48);
        emit = vVoid * cushion + vRim * (alpha * halo * 0.54) + vCore * (alpha * (exp(-r2 * 40.0) * 0.98 + glint * 0.52));
        occlusion = clamp(cushion + alpha * halo * 0.54 * 0.14, 0.0, 0.56);
    }

    if (occlusion < 0.001 && dot(emit, vec3(1.0)) < 0.001) discard;
    gl_FragColor = vec4(emit, occlusion);
}
