#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
// xy: rune lateral/vertical scale; zw: main stroke lateral/vertical scale.
uniform vec4 RadiusScale;
uniform float ViewportHeight;

out vec3 viewPosition;
out vec3 viewNormal;
out vec3 viewTangent;
out vec4 lightColor;
out float coverage;
out vec2 ringDirection;
out float flowingRing;

void main() {
    vec3 normal = normalize(Normal);
    vec4 centre = ModelViewMat * vec4(Position - normal * UV0.x, 1.0);
    float depth = ProjMat[3][3] > 0.5 ? 1.0 : max(-centre.z, 0.001);
    float pixelSize = 2.0 * depth / (ViewportHeight * abs(ProjMat[1][1]));
    // Vertex alpha tags fine strokes (0), main strokes (0.5), or flowing main rings (1).
    float mainStroke = step(0.25, Color.a);
    vec2 scale = mix(RadiusScale.xy, RadiusScale.zw, mainStroke);
    vec2 radius = UV0.x * scale;
    vec2 support = max(radius, vec2(pixelSize * 0.85));
    vec3 radii = vec3(radius.x, radius.y, radius.x);
    vec3 supportRadii = vec3(support.x, support.y, support.x);
    coverage = length(normal * radii) / length(normal * supportRadii);
    vec3 position = Position + normal * (supportRadii - UV0.x);
    vec4 view = ModelViewMat * vec4(position, 1.0);
    viewPosition = view.xyz;
    // Inverse scale preserves the correct outward normal on a laterally widened halo.
    viewNormal = mat3(ModelViewMat) * normalize(normal / supportRadii);
    vec3 tangent = vec3(cos(UV0.y), 0.0, sin(UV0.y));
    viewTangent = mat3(ModelViewMat) * tangent;
    ringDirection = vec2(tangent.z, -tangent.x);
    flowingRing = step(0.75, Color.a);
    lightColor = vec4(Color.rgb, mainStroke);
    gl_Position = ProjMat * view;
}
