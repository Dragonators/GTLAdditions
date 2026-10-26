#version 150

uniform sampler2D Sampler0;
uniform vec3 CameraPosition;
uniform vec3 BeamColor;
uniform vec3 CoreColor;
uniform vec3 HaloColor;
uniform float BeamRadius;
uniform mat4 ProjMat;

in vec2 texCoord;
in vec2 localPosition;
in float transparency;
out vec4 fragColor;

void main() {
    vec2 toCamera = CameraPosition.xy;
    float cameraLength = length(toCamera);
    vec2 direction = cameraLength > 1e-5 ? toCamera / cameraLength : vec2(1.0, 0.0);
    vec2 side = vec2(-direction.y, direction.x);
    vec2 ray = localPosition - toCamera;
    float impact =
        ProjMat[3][3] > 0.5 ? abs(dot(localPosition, side)) : abs(toCamera.x * ray.y - toCamera.y * ray.x) / max(length(ray), 1e-5);
    float x = impact / max(BeamRadius, 1e-4);
    // Body, core and halo retain the 0.85 narrow + 0.25 wide Gaussian profile.
    float body = 0.70 * exp(-14.0 * x * x);
    float core = 0.15 * exp(-80.0 * x * x);
    float halo = 0.25 * exp(-3.0 * x * x);
    float edge = 1.0 - smoothstep(0.78, 1.0, x);

    vec3 texel = texture(Sampler0, texCoord).rgb;
    float flow = clamp(1.0 - dot(texel, vec3(0.2126, 0.7152, 0.0722)), 0.0, 1.0);
    vec3 emission = BeamColor * body * mix(0.15, 1.0, flow) + CoreColor * core * flow + HaloColor * halo;
    emission *= max(transparency, 0.0) * edge;
    // Premultiplied emitted light with screen blending preserves the scene behind the beam.
    fragColor = vec4(clamp(emission, 0.0, 1.0), 0.0);
}
