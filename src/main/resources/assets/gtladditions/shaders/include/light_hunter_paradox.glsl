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

vec3 paradoxSky(vec3 direction,float rayFootprint) {
    // The distant sky follows the window's view ray; the two foreground models keep their own parallax.
    vec3 sky=vec3(-direction.z,direction.y,direction.x);
    sky.xy=mat2(0.9800666,-0.1986693,0.1986693,0.9800666)*sky.xy;
    float skyScale=1.0/3.0;
    float denominator=(1.0-sky.x)+skyScale*skyScale*(1.0+sky.x);
    float conformalScale=2.0*skyScale/denominator;
    sky=vec3((skyScale*skyScale*(1.0+sky.x)-(1.0-sky.x))/denominator,
             sky.yz*conformalScale);
    sky.yz=mat2(0.9063078,-0.4226183,0.4226183,0.9063078)*sky.yz;
    float skyLon=(abs(sky.x)+abs(sky.z)>1e-6)?atan(sky.z,sky.x):0.0;
    vec2 uv=vec2(skyLon/6.28318530718+0.5,
                 asin(clamp(sky.y,-0.999999,0.999999))/3.14159265359+0.5);
    // Analytical conformal footprint avoids the longitude seam and post-discard screen derivatives.
    float footprint=rayFootprint*conformalScale;
    float latitudeScale=max(length(sky.xz),0.02);
    float lod=clamp(log2(max(footprint*2048.0/(6.28318530718*latitudeScale),1.0)),0.0,11.0);
    vec3 radiance=textureLod(GalaxySampler,uv,lod).rgb*(6.78494e-5*0.12);
    vec3 color=pow(max(radiance,vec3(0.0))/(1.0+radiance),vec3(0.454545));
    return color*vec3(0.80,0.92,1.0)+vec3(0.003,0.005,0.009);
}
float paradoxStroke(float dist,float width,float aaWidth) {
    float aa=max(aaWidth,0.001);
    return 1.0-smoothstep(width,width+aa,abs(dist));
}
float paradoxWindowCoverage(vec2 q) {
    return lightHunterWindowCoverage(q,Signal.xyz);
}
void paradoxWindow() {
    vec2 q=windowCoord,v=q*0.5+0.5;
    vec2 dqdx=dFdx(q),dqdy=dFdy(q);
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
    vec3 ro=vec3((imageUv-0.5)*2.0,0.0),rayDelta=ro-WindowEye;
    float invRayLen=inversesqrt(dot(rayDelta,rayDelta));
    vec3 rd=rayDelta*invRayLen;
    float projX=dot(rd.xy,dqdx),projY=dot(rd.xy,dqdy);
    float rayFootprint=sqrt(max(max(dot(dqdx,dqdx)-projX*projX,dot(dqdy,dqdy)-projY*projY),0.0))*invRayLen;
    vec3 col=paradoxSky(rd,rayFootprint);
    if (Signal.x>0.0) {
        vec2 sampleUv=clamp(imageUv,vec2(0.001),vec2(0.999));
        vec4 model=textureLod(PortalMap,sampleUv,0.0);
        if (glitch>0.001) {
            float split=0.014*glitch*tear;
            vec4 left=textureLod(PortalMap,clamp(sampleUv+vec2(split,0.0),vec2(0.001),vec2(0.999)),0.0);
            vec4 right=textureLod(PortalMap,clamp(sampleUv-vec2(split,0.0),vec2(0.001),vec2(0.999)),0.0);
            model.r=mix(model.r,left.r,0.55*glitch);
            model.b=mix(model.b,right.b,0.55*glitch);
            model.a=max(model.a,max(left.a,right.a)*0.3*glitch);
        }
        vec3 lit=pow(max(model.rgb,vec3(0.0))/(1.0+max(model.rgb,vec3(0.0))),vec3(0.454545));
        col=mix(col,lit,model.a);
    }
    float edge=smoothstep(0.93,1.0,radius);
    vec2 qDir=dot(q,q)>1e-10?q/radius:vec2(1.0,0.0);
    vec2 eyeDir=dot(WindowEye.xy,WindowEye.xy)>1e-10?normalize(WindowEye.xy):vec2(1.0,0.0);
    float shoulder=0.7+0.3*dot(qDir,eyeDir);
    col=mix(col,vec3(0.025,0.05,0.065)*shoulder,edge*0.78);
    float seam=pow(1.0-abs(fract(v.y*14.0)*2.0-1.0),24.0);
    col+=vec3(0.045,0.085,0.10)*seam*tear*glitch;
    vec3 premultiplied=col*alpha;

    float rimAa=abs(dot(qDir,dqdx))+abs(dot(qDir,dqdy));
    float rim=paradoxStroke(radius-1.0,0.006,rimAa)*0.65;
    float rimDelta=(radius-1.0)/0.023;
    rim+=exp(-rimDelta*rimDelta)*0.08;
    vec2 axis=abs(q);
    float guideAa=axis.x<axis.y?(abs(dqdx.x)+abs(dqdy.x)):(abs(dqdx.y)+abs(dqdy.y));
    float guide=(paradoxStroke(min(axis.x,axis.y),0.003,guideAa))*
        step(12.8/12.2,max(axis.x,axis.y))*(1.0-step(13.6/12.2,max(axis.x,axis.y)))*0.5;
    vec2 target=q-vec2(-10.0,3.0)/12.2;
    float targetRadius=length(target);
    vec2 targetDir=targetRadius>1e-5?target/targetRadius:vec2(1.0,0.0);
    float selectedAa=abs(dot(targetDir,dqdx))+abs(dot(targetDir,dqdy));
    float selected=paradoxStroke(targetRadius-2.2/12.2,0.006,selectedAa)*0.65;
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
