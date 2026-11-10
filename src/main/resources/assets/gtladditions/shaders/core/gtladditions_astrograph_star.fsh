#version 150

#moj_import <gtladditions:heart_scene_hand.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    fragColor = texture(Sampler0, texCoord) * vertexColor * ColorModulator;
    // After the sample: derivatives are undefined once a neighbour has been discarded.
    if (sceneHandOccluded(gl_FragCoord.xy)) discard;
}
