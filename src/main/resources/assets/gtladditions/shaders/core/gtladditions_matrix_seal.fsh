#version 150
in vec4 vertexColor;
uniform float Intensity;
out vec4 fragColor;
void main() {
    fragColor = vec4(vertexColor.rgb * Intensity, vertexColor.a);
}
