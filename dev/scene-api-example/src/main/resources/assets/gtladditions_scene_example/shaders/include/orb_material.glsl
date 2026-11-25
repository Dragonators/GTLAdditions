uniform vec4 OrbColor;
uniform float Phase;

vec4 orbShell(vec3 viewPosition, vec3 viewNormal) {
    float facing = abs(dot(normalize(viewNormal), normalize(-viewPosition)));
    float rim = max(1.0 - facing, 0.0);
    float alpha = OrbColor.a * (0.3 + 0.7 * (rim * rim)) * (0.85 + 0.15 * sin(Phase));
    return vec4(OrbColor.rgb * alpha, alpha);
}
