uniform sampler2D ImageRects;
uniform vec2 ImageStorageSize;
vec4 sampleHeartImage(vec2 pixel, int index) {
    vec4 rect=texelFetch(ImageRects,ivec2(index,0),0);
    vec2 p=pixel-rect.xy;
    if(any(lessThan(p,vec2(0.0))) || any(greaterThanEqual(p,rect.zw))) return vec4(0.0);
    return textureLod(HeartImages,vec3(p/ImageStorageSize,float(index)),0.0);
}
