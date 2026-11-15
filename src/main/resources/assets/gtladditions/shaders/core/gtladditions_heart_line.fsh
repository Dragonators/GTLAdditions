#version 150

in vec4 LineColor;

out vec4 fragColor;

void main() {
    fragColor = vec4(LineColor.rgb, 0.0);
}
