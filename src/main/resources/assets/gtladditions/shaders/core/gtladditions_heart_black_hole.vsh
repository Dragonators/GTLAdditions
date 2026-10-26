#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 Center;
uniform float VolumeProxyRadius;

out vec3 volumeSurfacePosition;

void main() {
    float proxyRadius = max(VolumeProxyRadius, 0.001);
    vec3 volumeModelPosition = Position * proxyRadius;
    volumeSurfacePosition = Center + volumeModelPosition;
    gl_Position = ProjMat * ModelViewMat * vec4(volumeSurfacePosition, 1.0);
}
