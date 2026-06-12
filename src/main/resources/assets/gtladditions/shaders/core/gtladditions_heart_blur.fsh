#version 150

uniform sampler2D Sampler0;
uniform vec2 TexelStep;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord) * 0.22702703;
    color += texture(Sampler0, texCoord + TexelStep * 1.38461538) * 0.31621622;
    color += texture(Sampler0, texCoord - TexelStep * 1.38461538) * 0.31621622;
    color += texture(Sampler0, texCoord + TexelStep * 3.23076923) * 0.07027027;
    color += texture(Sampler0, texCoord - TexelStep * 3.23076923) * 0.07027027;
    fragColor = color;
}
