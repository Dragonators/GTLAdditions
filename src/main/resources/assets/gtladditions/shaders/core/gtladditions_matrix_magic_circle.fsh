#version 150

uniform mat4 ProjMat;
uniform vec2 Emission;
uniform float FlowPhase;

in vec3 viewPosition;
in vec3 viewNormal;
in vec3 viewTangent;
in vec4 lightColor;
in float coverage;
in vec2 ringDirection;
in float flowingRing;
out vec4 fragColor;

void main() {
    vec3 direction = ProjMat[3][3] > 0.5 ? vec3(0.0, 0.0, 1.0) : normalize(-viewPosition);
    vec3 normal = normalize(viewNormal);
    vec3 tangent = normalize(viewTangent);
    float facing = max(dot(normal, direction), 0.0);
    // Correct for the tube axis so looking along a stroke does not extinguish its light.
    float projectedAxis = max(length(cross(direction, tangent)), 0.05);
    float cap = abs(dot(normal, tangent));
    float radial = clamp(facing / mix(projectedAxis, 1.0, cap), 0.0, 1.0);
    float density = radial * radial;
    density *= density;
    float intensity = mix(Emission.x, Emission.y, lightColor.a);
    float flow = 1.0;
    if (flowingRing > 0.5) {
        const float PI = 3.141592653589793;
        float angle = atan(ringDirection.y, ringDirection.x);
        float delta = mod(angle - FlowPhase + PI * 0.5, PI) - PI * 0.5;
        float extent = delta > 0.0 ? 0.10 : 0.55;
        float crest = 1.0 - smoothstep(0.0, extent, abs(delta));
        flow = 0.85 + 0.35 * crest;
    }
    vec3 emission = lightColor.rgb * (intensity * density * coverage * flow);
    fragColor = vec4(emission, 0.0);
}
