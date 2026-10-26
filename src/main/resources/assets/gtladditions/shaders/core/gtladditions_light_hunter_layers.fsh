#version 330 core
#define LIGHT_HUNTER_LAYERED
#moj_import <gtladditions:light_hunter_layers.glsl>
#moj_import <gtladditions:light_hunter_material.glsl>
void main() {
    layer0=vec4(0.0); layer1=vec4(0.0); layer2=vec4(0.0); layer3=vec4(0.0);
    vec2 pixel=effectScreenPixel();
    if (sceneHandOccluded(pixel)) discard;
    if (LayerMode==0 && all(greaterThanEqual(pixel,LayerRect.xy)) &&
        all(lessThan(pixel,LayerRect.xy+LayerRect.zw))) discard;
    // Copied opaque depth also supports depth-stencil attachments.
    if (Effect!=0 && Effect!=1 && gl_FragCoord.z>texelFetch(SceneDepth,ivec2(pixel-SceneViewport),0).r) discard;
    if (LayerMode==1) loadLayers();
    materialMain();
    materialColor.rgb=clamp(materialColor.rgb,0.0,1.0);
    bool film=Effect==6 || Effect==7;
    if (LayerMode==0) {
        layer0=vec4(materialColor.rgb*materialColor.a,film?materialColor.a:0.0);
    } else if (Effect==0 || Effect==1) {
        layer0=vec4(plumeBins[0],0.0); layer1=vec4(plumeBins[1],0.0);
        layer2=vec4(plumeBins[2],0.0); layer3=vec4(plumeBins[3],0.0);
    } else {
        // Equal-depth boundaries belong to the nearer bin, as in the prototype.
        for (int i=0; i<4; i++) {
            int bin=LayerBase+i;
            if (bin>LayerCount) break;
            bool after=bin==0 || effectViewDepth>boundaries[i];
            bool before=bin==LayerCount || effectViewDepth<=boundaries[i+1];
            if (after && before) {
                emitLayer(i,vec4(materialColor.rgb*materialColor.a,film?materialColor.a:0.0));
                break;
            }
        }
    }
}
