uniform vec4 OrbColor;
uniform float Phase;

vec4 orbShell(vec3 viewPosition, vec3 viewNormal) {
    float facing = abs(dot(normalize(viewNormal), normalize(-viewPosition)));
    float alpha = OrbColor.a * (0.3 + 0.7 * pow(1.0 - facing, 2.0)) * (0.85 + 0.15 * sin(Phase));
    return vec4(OrbColor.rgb * alpha, alpha);
}
