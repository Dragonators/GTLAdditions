#version 330 core
#moj_import <gtladditions:heart_layer_bins.glsl>
#moj_import <gtladditions:matrix_energy_core.glsl>
uniform sampler2D SceneDepth;
uniform vec2 SceneViewport;

void main() {
    layer0=vec4(0.0); layer1=vec4(0.0); layer2=vec4(0.0); layer3=vec4(0.0);
    vec2 pixel=effectScreenPixel();
    if (LayerMode==0 && all(greaterThanEqual(pixel,LayerRect.xy)) &&
        all(lessThan(pixel,LayerRect.xy+LayerRect.zw))) discard;
    if (sceneHandOccluded(pixel)) discard;
    vec4 material=coreMaterial();
    // The material supplies entry-surface depth; the proxy exit face must not hide the core.
    if (gl_FragDepth>texelFetch(SceneDepth,ivec2(pixel-SceneViewport),0).r) discard;
    material.rgb*=material.a;
    if (LayerMode==0) {
        layer0=material;
        return;
    }
    loadLayers();
    float viewDepth=coreEntryViewDepth;
    for (int i=0; i<4; i++) {
        int bin=LayerBase+i;
        if (bin>LayerCount) break;
        bool after=bin==0 || viewDepth>boundaries[i];
        bool before=bin==LayerCount || viewDepth<=boundaries[i+1];
        if (after && before) {
            emitLayer(i,material);
            break;
        }
    }
}
