#moj_import <gtladditions:heart_layer_bins.glsl>
in float effectViewDepth;
float rayBoundaries[5];
vec3 prefixes[5];
vec3 plumeBins[4];
void layerBegin(vec3 eyeView,vec3 dirView) {
    for (int i=0; i<5; i++) {
        prefixes[i]=vec3(0.0);
        rayBoundaries[i]=1e30;
        if (LayerMode==1 && boundaries[i]<1e20)
            rayBoundaries[i]=(-boundaries[i]-eyeView.z)/dirView.z;
    }
}
void layerAccumulate(float density,float segment,float rayDist,float absorption,float transmission,vec3 color) {
    if (LayerMode!=1) return;
    for (int i=0; i<5; i++) {
        float portion=clamp(rayBoundaries[i]-rayDist,0.0,segment);
        prefixes[i]+=transmission*(1.0-exp(-density*portion*absorption))*color;
    }
}
float layerClipSegment(float rayDist,float segment) {
    if (LayerMode!=1) return segment;
    for (int i=0; i<5; i++) {
        float remaining=rayBoundaries[i]-rayDist;
        if (remaining>0.00001) segment=min(segment,remaining);
    }
    return segment;
}
void layerAccumulateEmission(vec3 emission,float segment,float rayDist) {
    if (LayerMode!=1) return;
    for (int i=0; i<5; i++) {
        prefixes[i]+=emission*clamp(rayBoundaries[i]-rayDist,0.0,segment);
    }
}
vec4 layerFinish(vec3 light,float exposure) {
    vec3 complete=vec3(1.0)-exp(-light*exposure);
    if (LayerMode==0) return vec4(complete,1.0);
    vec3 previous=LayerBase==0 ? vec3(0.0) : vec3(1.0)-exp(-prefixes[0]*exposure);
    for (int i=0; i<4; i++) {
        vec3 mapped=LayerBase+i<LayerCount ?
            vec3(1.0)-exp(-prefixes[i+1]*exposure) : complete;
        plumeBins[i]=max(mapped-previous,vec3(0.0));
        previous=mapped;
    }
    return vec4(complete,1.0);
}
