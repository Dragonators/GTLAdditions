#version 150

uniform sampler2D Sampler0;
uniform vec2 TexelStep;
uniform float BrightnessThreshold;

in vec2 texCoord;
out vec4 fragColor;

vec4 sampleInput(vec2 coordinate) {
    vec4 color = texture(Sampler0, coordinate);
    if (BrightnessThreshold >= 0.0) {
        float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
        float contribution = smoothstep(BrightnessThreshold, BrightnessThreshold + 0.01, brightness);
        color = vec4(color.rgb * contribution, 0.0);
    }
    return color;
}

void main() {
    // Filter premultiplied RGB, alpha and volume mask together.
    vec4 color = sampleInput(texCoord + TexelStep * vec2(-1.0, -1.0));
    color += sampleInput(texCoord + TexelStep * vec2(1.0, -1.0));
    color += sampleInput(texCoord + TexelStep * vec2(-1.0, 1.0));
    color += sampleInput(texCoord + TexelStep * vec2(1.0, 1.0));
    fragColor = color * 0.25;
}
