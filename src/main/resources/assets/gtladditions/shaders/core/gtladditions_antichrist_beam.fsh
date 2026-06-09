#version 150

uniform sampler2D Sampler0;
uniform float Intensity;
uniform vec3 CameraPosition;
uniform vec3 Color;

in vec2 texCoord;
in vec2 localPosition;
in float transparency;

out vec4 fragColor;

float luminanceTransform(vec3 color) {
    return 0.2126 * color.r + 0.7152 * color.g + 0.0722 * color.b;
}

void main() {
    float viewEffect = dot(normalize(CameraPosition.xy), normalize(localPosition));
    float angleAlpha = max((viewEffect - 0.5) * 2.0, 0.0);
    angleAlpha = pow(angleAlpha, Intensity);

    vec4 texColor = texture(Sampler0, texCoord);
    float luminance = 1.0 - luminanceTransform(texColor.xyz);
    luminance = mix(luminance, 1.0, 1.0 - pow(angleAlpha, 6.0));
    float alpha = clamp(angleAlpha * transparency * luminance, 0.0, 1.0);
    fragColor = vec4(Color, alpha);
}
