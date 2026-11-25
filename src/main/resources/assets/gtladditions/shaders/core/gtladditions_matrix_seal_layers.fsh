#version 330 core
#moj_import <gtladditions:heart_layer_bins.glsl>
uniform sampler2D SceneDepth;
uniform vec2 SceneViewport;
uniform float Intensity;
in vec4 vertexColor;
in float viewDepth;

void main() {
    layer0=vec4(0.0); layer1=vec4(0.0); layer2=vec4(0.0); layer3=vec4(0.0);
    vec2 pixel=effectScreenPixel();
    if (LayerMode==0 && all(greaterThanEqual(pixel,LayerRect.xy)) &&
        all(lessThan(pixel,LayerRect.xy+LayerRect.zw))) discard;
    // The bins have no hardware depth test; keep the direct draw's LEQUAL against the scene.
    if (gl_FragCoord.z>texelFetch(SceneDepth,ivec2(pixel-SceneViewport),0).r) discard;
    // The additive light as premultiplied colour that covers nothing.
    vec4 light=vec4(vertexColor.rgb*Intensity*vertexColor.a,0.0);
    if (LayerMode==0) {
        layer0=light;
        return;
    }
    loadLayers();
    for (int i=0; i<4; i++) {
        int bin=LayerBase+i;
        if (bin>LayerCount) break;
        bool after=bin==0 || viewDepth>boundaries[i];
        bool before=bin==LayerCount || viewDepth<=boundaries[i+1];
        if (after && before) {
            emitLayer(i,light);
            break;
        }
    }
}
