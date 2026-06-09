#version 150

uniform sampler2D Sampler0;
uniform vec4 Color;
uniform float Gamma;

in vec2 texCoord;

out vec4 fragColor;

vec3 toYIQ(vec3 rgb) {
    return mat3(
        0.299, 1.0, 0.40462981,
        0.587, -0.46081557, -1.0,
        0.114, -0.53918443, 0.59537019
    ) * rgb;
}

vec3 toRGB(vec3 yiq) {
    return mat3(
        1.0, 1.0, 1.0,
        0.5696804, -0.1620848, -0.6590654,
        0.3235513, -0.3381869, 0.8901581
    ) * yiq;
}

void main() {
    vec3 texel = texture(Sampler0, texCoord).rgb;
    vec3 original = toYIQ(texel);

    if (length(original.xy) < 0.01) {
        fragColor = vec4(texel, 1.0);
    } else {
        vec3 targetYIQ = toYIQ(Color.rgb);
        vec3 yiqColor = vec3(original.x, targetYIQ.yz);
        vec3 finalRgb = toRGB(yiqColor);
        finalRgb = pow(max(finalRgb, vec3(0.0)), vec3(1.0 / Gamma));
        fragColor = vec4(finalRgb, Color.a);
    }
}
