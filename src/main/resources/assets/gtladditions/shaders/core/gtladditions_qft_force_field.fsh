#version 150

uniform sampler2D Sampler0;
uniform float AnimationFrame;

in vec2 texCoord;
out vec4 fragColor;

vec4 sampleFrame(float frame, vec2 pixel, vec2 size) {
    vec2 uv = vec2(pixel.x, pixel.y + frame * size.x) / size;
    vec4 color = texture(Sampler0, uv);
    return vec4(color.rgb * color.a, color.a);
}

void main() {
    vec2 size = vec2(textureSize(Sampler0, 0));
    // Half-texel inset and clamp keep bilinear filtering inside each 64x64 frame.
    vec2 pixel = vec2(0.5) + clamp(texCoord, 0.0, 1.0) * (size.x - 1.0);
    float frame = floor(AnimationFrame);
    vec4 field = mix(sampleFrame(frame, pixel, size), sampleFrame(frame + 1.0, pixel, size), fract(AnimationFrame));

    float edgeDistance = min(texCoord.y, 1.0 - texCoord.y);
    float fade = smoothstep(0.0, 0.018, edgeDistance);
    float rim = 1.0 - smoothstep(0.025, 0.10, edgeDistance);
    float energy = smoothstep(0.12, 0.72, field.a);
    // Premultiplied interpolation avoids dark fringes where animated alpha changes.
    vec3 glow = field.rgb * (0.94 + 0.22 * energy + 0.12 * rim);
    glow += vec3(0.08, 0.32, 0.38) * field.a * rim * 0.18;
    fragColor = vec4(glow * fade, field.a * (0.82 + 0.12 * energy) * fade);
}
