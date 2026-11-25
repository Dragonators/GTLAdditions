#moj_import <gtladditions:heart_scene_hand.glsl>

// Four render targets are available in the OpenGL 3.3 baseline. Larger overlap
// sets use successive groups, never a truncated list of Hearts.
layout(location=0) out vec4 layer0;
layout(location=1) out vec4 layer1;
layout(location=2) out vec4 layer2;
layout(location=3) out vec4 layer3;
uniform sampler2DArray LayerDepths;
uniform vec2 LayerOrigin;
uniform vec4 LayerRect;
uniform int LayerCount;
uniform int LayerBase;
uniform int LayerMode;
float boundaries[5];
vec2 effectScreenPixel() { return gl_FragCoord.xy + LayerOrigin; }
void loadLayers() {
    vec3 nearDepths=vec3(1e30);
    if (LayerCount<=3) {
        nearDepths=texelFetch(LayerDepths,ivec3(ivec2(gl_FragCoord.xy),0),0).xyz;
        for (int i=0; i<3; i++) if (i>=LayerCount) nearDepths[i]=1e30;
        if (nearDepths.x>nearDepths.y) nearDepths.xy=nearDepths.yx;
        if (nearDepths.y>nearDepths.z) nearDepths.yz=nearDepths.zy;
        if (nearDepths.x>nearDepths.y) nearDepths.xy=nearDepths.yx;
    }
    for (int i=0; i<5; i++) {
        int rank=LayerBase+i-1;
        boundaries[i]=rank<0 ? 0.0 : rank>=LayerCount ? 1e30 :
            LayerCount<=3 ? nearDepths[rank] : texelFetch(LayerDepths,ivec3(ivec2(gl_FragCoord.xy),rank),0).r;
    }
}
void emitLayer(int index,vec4 color) {
    if (index==0) layer0=color;
    else if (index==1) layer1=color;
    else if (index==2) layer2=color;
    else if (index==3) layer3=color;
}
