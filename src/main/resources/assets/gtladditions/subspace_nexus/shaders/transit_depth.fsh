flat varying float vAlpha;

void main() {
    if (vAlpha <= 0.002) discard;
    gl_FragColor = vec4(0.0);
}
