uniform int WindowPass;
uniform float WindowPlane;
uniform sampler2D PortalMap;
uniform sampler2D GalaxySampler;
uniform vec3 WindowEye;
// Opacity, distortion, deterministic glitch frame, rim opacity.
uniform vec4 Signal;
in vec2 windowCoord;
vec4 paradoxColor;
#moj_import <gtladditions:light_hunter_navigation_region.glsl>
#moj_import <gtladditions:light_hunter_window_coverage.glsl>

vec3 paradoxSky(vec3 direction) {
    // The distant sky follows the window's view ray; the two foreground models keep their own parallax.
    vec3 sky=vec3(-direction.z,direction.y,direction.x);
    sky.xy=mat2(0.9800666,-0.1986693,0.1986693,0.9800666)*sky.xy;
    float skyScale=1.0/3.0;
    float denominator=(1.0-sky.x)+skyScale*skyScale*(1.0+sky.x);
    sky=vec3((skyScale*skyScale*(1.0+sky.x)-(1.0-sky.x))/denominator,
             sky.yz*(2.0*skyScale/denominator));
    sky.yz=mat2(0.9063078,-0.4226183,0.4226183,0.9063078)*sky.yz;
    vec2 uv=vec2(atan(sky.z,sky.x)/6.28318530718+0.5,
                 asin(clamp(sky.y,-0.999999,0.999999))/3.14159265359+0.5);
    // Direction derivatives avoid the panorama's longitude seam when choosing a mip level.
    float footprint=max(length(dFdx(sky)),length(dFdy(sky)));
    float latitudeScale=max(length(sky.xz),0.02);
    float lod=clamp(log2(max(footprint*2048.0/(6.28318530718*latitudeScale),1.0)),0.0,11.0);
    vec3 radiance=textureLod(GalaxySampler,uv,lod).rgb*(6.78494e-5*0.12);
    vec3 color=pow(max(radiance,vec3(0.0))/(1.0+radiance),vec3(0.454545));
    return color*vec3(0.80,0.92,1.0)+vec3(0.003,0.005,0.009);
}
float paradoxStroke(float distance,float width) {
    float aa=max(fwidth(distance),0.001);
    return 1.0-smoothstep(width,width+aa,abs(distance));
}
float paradoxWindowCoverage(vec2 q) {
    return lightHunterWindowCoverage(q,Signal.xyz);
}
void paradoxWindow() {
    vec2 q=windowCoord,v=q*0.5+0.5;
    // Reserve the whole NAV aperture, including its transparent background, in every color/depth pass.
    vec3 chartPosition=vec3(WindowPlane,q.y*12.2+6.0,-q.x*12.2+12.0);
    if (lightHunterNavigationPanelRegion(chartPosition)) discard;
    float radius=length(q);
    float glitch=Signal.y,frame=Signal.z;
    float row=floor(v.y*11.0),rowSeed=paradoxHash(vec2(row,frame+3.0));
    float tear=step(0.48,rowSeed);
    float alpha=paradoxWindowCoverage(q);
    // Only intact, opaque window pixels write depth; holes and the glowing rim never do.
    if (WindowPass==1 && alpha<0.999) discard;
    if (WindowPass==2 && alpha>=0.999) discard;
    vec2 imageUv=v+vec2((rowSeed-0.5)*0.24,(paradoxHash(vec2(row+11.0,frame))-0.5)*0.024)*glitch*tear;
    vec3 ro=vec3((imageUv-0.5)*2.0,0.0),rd=normalize(ro-WindowEye);
    vec3 col=paradoxSky(rd);
    if (Signal.x>0.0) {
        vec2 sampleUv=clamp(imageUv,vec2(0.001),vec2(0.999));
        vec4 model=texture(PortalMap,sampleUv);
        if (glitch>0.001) {
            float split=0.014*glitch*tear;
            vec4 left=texture(PortalMap,clamp(sampleUv+vec2(split,0),vec2(0.001),vec2(0.999)));
            vec4 right=texture(PortalMap,clamp(sampleUv-vec2(split,0),vec2(0.001),vec2(0.999)));
            model.r=mix(model.r,left.r,0.55*glitch);
            model.b=mix(model.b,right.b,0.55*glitch);
            model.a=max(model.a,max(left.a,right.a)*0.3*glitch);
        }
        vec3 lit=pow(max(model.rgb,vec3(0.0))/(1.0+max(model.rgb,vec3(0.0))),vec3(0.454545));
        col=mix(col,lit,model.a);
    }
    float edge=smoothstep(0.93,1.0,radius);
    float shoulder=0.7+0.3*dot(normalize(q+0.0001),normalize(WindowEye.xy+0.0001));
    col=mix(col,vec3(0.025,0.05,0.065)*shoulder,edge*0.78);
    float seam=pow(1.0-abs(fract(v.y*14.0)*2.0-1.0),24.0);
    col+=vec3(0.045,0.085,0.10)*seam*tear*glitch;
    vec3 premultiplied=col*alpha;

    float rim=paradoxStroke(radius-1.0,0.006)*0.65;
    rim+=exp(-pow((radius-1.0)/0.023,2.0))*0.08;
    vec2 axis=abs(q);
    float guide=(paradoxStroke(min(axis.x,axis.y),0.003))*
        step(12.8/12.2,max(axis.x,axis.y))*(1.0-step(13.6/12.2,max(axis.x,axis.y)))*0.5;
    vec2 target=q-vec2(-10.0,3.0)/12.2;
    float selected=paradoxStroke(length(target)-2.2/12.2,0.006)*0.65;
    selected+=exp(-dot(target,target)/0.004)*0.8;
    float lineAlpha=clamp((rim+guide+selected)*Signal.w,0.0,0.96);
    premultiplied=mix(premultiplied,vec3(0.58,0.81,0.86),lineAlpha);
    alpha=alpha+(1.0-alpha)*lineAlpha;
    paradoxColor=vec4(premultiplied/max(alpha,0.00001),alpha);
}
void paradoxMain() {
    paradoxWindow();
    if (paradoxColor.a<0.0001) discard;
}
