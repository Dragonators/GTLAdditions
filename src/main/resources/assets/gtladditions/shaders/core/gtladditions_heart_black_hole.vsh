#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 Center;
uniform float Time;
uniform float BlackHoleAndDiskRadius;
uniform float SpaceFadeRadius;

#moj_import <gtladditions:heart_volume_common.glsl>

out vec3 volumeSurfacePosition;
out vec3 volumeModelPosition;

void main() {
    float proxyRadius = getVolumeProxyRadius(Time);
    volumeModelPosition = Position * proxyRadius;
    volumeSurfacePosition = Center + volumeModelPosition;
    gl_Position = ProjMat * ModelViewMat * vec4(volumeSurfacePosition, 1.0);
}
