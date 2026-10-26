#version 330 core
#define LIGHT_HUNTER_FUSION_LAYERED
#moj_import <gtladditions:light_hunter_layers.glsl>
#moj_import <gtladditions:light_hunter_fusion.glsl>
void main() {
    layer0=vec4(0);layer1=vec4(0);layer2=vec4(0);layer3=vec4(0);
    vec2 pixel=effectScreenPixel();
    if (sceneHandOccluded(pixel)) discard;
    if(LayerMode==0 && all(greaterThanEqual(pixel,LayerRect.xy)) && all(lessThan(pixel,LayerRect.xy+LayerRect.zw)))discard;
    if(LayerMode==1)loadLayers();
    fusionMain();
    if(LayerMode==0)layer0=fusionColor;
    else{layer0=vec4(plumeBins[0],0);layer1=vec4(plumeBins[1],0);layer2=vec4(plumeBins[2],0);layer3=vec4(plumeBins[3],0);}
}
