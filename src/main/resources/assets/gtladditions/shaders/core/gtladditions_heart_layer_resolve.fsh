#version 150
uniform sampler2D Base;
uniform sampler2D Layer0;
uniform sampler2D Layer1;
uniform sampler2D Layer2;
uniform sampler2D Layer3;
uniform sampler2DArray HeartImages;
#moj_import <gtladditions:heart_image_sampling.glsl>

uniform sampler2DArray LayerDepths;
uniform vec2 Origin;
uniform int Count;
uniform int LayerBase;
out vec4 fragColor;
vec4 over(vec4 a,vec4 b) { return clamp(a+b*(1.0-a.a),0.0,1.0); }
void main() {
    ivec2 local=ivec2(gl_FragCoord.xy-Origin);
    vec4 bins[4]=vec4[4](texelFetch(Layer0,local,0),texelFetch(Layer1,local,0),
        texelFetch(Layer2,local,0),texelFetch(Layer3,local,0));
    vec4 result=texelFetch(Base,local,0);
    vec3 nearDepths=vec3(1e30);
    int indices[3]=int[3](0,1,2);
    if (Count<=3) {
        nearDepths=texelFetch(LayerDepths,ivec3(local,0),0).xyz;
        for (int i=0; i<3; i++) if (i>=Count) nearDepths[i]=1e30;
        for (int i=0; i<3; i++) for (int j=i+1; j<3; j++) {
            if (nearDepths[indices[i]]>nearDepths[indices[j]] ||
                (nearDepths[indices[i]]==nearDepths[indices[j]] && indices[i]>indices[j])) {
                int swap=indices[i]; indices[i]=indices[j]; indices[j]=swap;
            }
        }
    }
    for (int i=3; i>=0; i--) {
        int bin=LayerBase+i;
        if (bin>Count) continue;
        result=over(bins[i],result);
        if (bin>0) {
            int index=Count<=3 ? indices[bin-1] : 0;
            vec2 entry=Count<=3 ? vec2(nearDepths[index],nearDepths[index]<1e20 ? float(index) : -1.0) :
                texelFetch(LayerDepths,ivec3(local,bin-1),0).rg;
            if (entry.y>=0.0)
                result=over(sampleHeartImage(gl_FragCoord.xy,int(entry.y)),result);
        }
    }
    fragColor=vec4(result.rgb,1.0);
}
