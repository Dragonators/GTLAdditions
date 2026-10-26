#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    fragColor = texture(Sampler0, texCoord) * vertexColor * ColorModulator;
}
