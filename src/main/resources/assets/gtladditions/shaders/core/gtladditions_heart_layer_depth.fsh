#version 150
uniform sampler2DArray HeartImages;
#moj_import <gtladditions:heart_image_sampling.glsl>

uniform sampler2D Centers;
uniform sampler2D Previous;
uniform mat4 InverseProjection;
uniform vec2 FrameSize;
uniform vec2 Origin;
uniform int Count;
uniform int Rank;
out vec4 fragColor;
void main() {
    vec2 uv=(gl_FragCoord.xy+Origin)/FrameSize;
    vec4 n=InverseProjection*vec4(uv*2.0-1.0,-1.0,1.0);
    vec4 f=InverseProjection*vec4(uv*2.0-1.0,1.0,1.0);
    vec3 eye=n.xyz/n.w;
    vec3 ray=normalize(f.xyz/f.w-eye);
    vec2 previous=Rank<=0 ? vec2(-1.0,-1.0) : texelFetch(Previous,ivec2(gl_FragCoord.xy),0).rg;
    vec2 best=vec2(1e30,-1.0);
    vec4 nearDepths=vec4(1e30);
    // Ping-pong targets avoid sampling a texture while any layer is attached for drawing.
    for (int i=0; i<Count; i++) {
        if (sampleHeartImage(uv*FrameSize,i).a<0.0005) continue;
        vec4 center=texelFetch(Centers,ivec2(i,0),0);
        vec3 delta=eye-center.xyz;
        float b=dot(delta,ray);
        vec3 perpendicular=delta-ray*b;
        float h=center.w*center.w-dot(perpendicular,perpendicular);
        float t=max(-b-sqrt(max(h,0.0)),0.0);
        float depth=max(-(eye+ray*t).z,0.0);
        if (Rank<0) {
            nearDepths[i]=depth;
            continue;
        }
        bool after=depth>previous.x || (depth==previous.x && float(i)>previous.y);
        if (after && (depth<best.x || (depth==best.x && float(i)<best.y)))
            best=vec2(depth,float(i));
    }
    fragColor=Rank<0 ? nearDepths : vec4(best,0.0,0.0);
}
