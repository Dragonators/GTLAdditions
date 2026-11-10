#version 150

#moj_import <gtladditions_scene_example:orb_material.glsl>
#moj_import <gtladditions:scene_depth.glsl>

in vec3 viewPosition;
in vec3 viewNormal;

out vec4 fragColor;

void main() {
    vec2 scenePixel = gl_FragCoord.xy - SceneViewport;
    float scene = sceneRayDistance(scenePixel / SceneSize, sceneDepthAt(ivec2(scenePixel)));
    fragColor = orbShell(viewPosition, viewNormal) * smoothstep(0.0, 0.4, scene - length(viewPosition));
}
