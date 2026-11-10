#version 330 core
#moj_import <gtladditions:light_hunter_layers.glsl>
#moj_import <gtladditions:light_hunter_paradox.glsl>
uniform sampler2D SceneDepth;
uniform vec2 SceneViewport;
void main() {
    layer0=vec4(0.0); layer1=vec4(0.0); layer2=vec4(0.0); layer3=vec4(0.0);
    vec2 pixel=effectScreenPixel();
    if (LayerMode==0 && all(greaterThanEqual(pixel,LayerRect.xy)) &&
        all(lessThan(pixel,LayerRect.xy+LayerRect.zw))) discard;
    // Window anti-aliasing takes derivatives across the quad: per-pixel discards come after it.
    paradoxMain();
    if (sceneHandOccluded(pixel)) discard;
    if (gl_FragCoord.z>texelFetch(SceneDepth,ivec2(pixel-SceneViewport),0).r) discard;
    vec4 color=vec4(paradoxColor.rgb*paradoxColor.a,paradoxColor.a);
    if (LayerMode==0) {
        layer0=color;
    } else {
        loadLayers();
        for (int i=0;i<4;i++) {
            int bin=LayerBase+i;
            if (bin>LayerCount) break;
            bool after=bin==0 || effectViewDepth>boundaries[i];
            bool before=bin==LayerCount || effectViewDepth<=boundaries[i+1];
            if (after && before) { emitLayer(i,color); break; }
        }
    }
}
