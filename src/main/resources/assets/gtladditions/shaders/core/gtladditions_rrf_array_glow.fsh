#version 150

uniform mat4 ProjMat;
in vec3 viewPosition;
in vec3 viewNormal;
in vec3 viewTangent;
in vec4 lightColor;
out vec4 fragColor;

void main() {
    vec3 direction = ProjMat[3][3] > 0.5 ? vec3(0.0, 0.0, 1.0) : normalize(-viewPosition);
    float facing = max(dot(normalize(viewNormal), direction), 0.0);
    float axisLength = length(viewTangent);
    float projectedAxis = axisLength > 0.5 ? max(length(cross(direction, viewTangent / axisLength)), 0.05) : 1.0;
    float radial = clamp(facing / projectedAxis, 0.0, 1.0);
    float density = axisLength > 0.5 ? smoothstep(0.18, 0.58, radial) * (0.72 + 0.28 * radial) : radial * radial * radial * radial;
    vec3 tint = axisLength > 0.5 ? mix(lightColor.rgb, vec3(1.0), 0.20 * smoothstep(0.90, 0.995, radial)) : lightColor.rgb;
    // The tube silhouette still reaches zero, so the stronger core cannot expose a polygon plate.
    fragColor = vec4(tint * (lightColor.a * density), 0.0);
}
