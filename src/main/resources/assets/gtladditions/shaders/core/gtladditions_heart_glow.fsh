#version 150

in vec2 SpriteUv;
in vec4 SpriteColor;

out vec4 fragColor;

void main() {
    float r = length(SpriteUv - 0.5);
    float a = (1.0 - smoothstep(0.0, 0.5, r));
    fragColor = vec4(SpriteColor.rgb * (a * a * 0.9 + (1.0 - smoothstep(0.0, 0.17, r))), 0.0);
}
