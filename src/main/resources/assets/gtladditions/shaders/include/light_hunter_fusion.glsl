uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform sampler2D SceneDepth;
uniform vec2 SceneViewport;
uniform float Time;
uniform float Seconds;
in vec3 localPosition;
flat in vec3 localEye;
flat in vec3 localForward;
flat in float orthoCamera;
vec4 fusionColor;
#ifndef LIGHT_HUNTER_FUSION_LAYERED
vec2 effectScreenPixel() { return gl_FragCoord.xy; }
void layerBegin(vec3 eyeView,vec3 dirView) {}
float layerClipSegment(float distance,float segment) { return segment; }
void layerAccumulateEmission(vec3 emission,float segment,float distance) {}
#endif
const float PI=3.14159265359, TAU=6.28318530718, R=32.5;

#moj_import <gtladditions:light_hunter_fusion_noise.glsl>
uniform sampler3D FusionField;
uniform int UseFusionField;
uniform vec2 FusionFieldOrigin;
float sampleFusionWisp(float angle,float radial,float x){
    if(UseFusionField==0)return fusionWispNoise(angle,radial,x,Time);
    vec3 size=vec3(textureSize(FusionField,0));
    float axialCell=(x*1.4-Time*.28-FusionFieldOrigin.x)/.078125+FusionFieldOrigin.y;
    vec3 address=vec3((angle-Time*.055)/TAU+.5/size.x,
        ((radial+.90)/1.80*(size.y-1.0)+.5)/size.y,(axialCell+.5)/size.z);
    return fusionWispDensity(textureLod(FusionField,address,0.0).rg);
}
vec2 cylinder(vec2 ro,vec2 rd,float radius){
    float a=dot(rd,rd),b=dot(ro,rd),c=dot(ro,ro)-radius*radius;
    if(a<.00000001)return c<=0.0?vec2(-1e20,1e20):vec2(1,-1);
    float d=b*b-a*c;if(d<0.0)return vec2(1,-1);
    return vec2(-b-sqrt(d),-b+sqrt(d))/a;
}
float connectionArc(float angle){
    // Four sectors, sixteen sites each, with gaps at the corners.
    float local=mod(angle+PI*.25,PI*.5),spacing=(PI*.5-.13)/16.0;
    float index=clamp(floor((local-.065)/spacing),0.0,15.0);
    return (local-(.065+(index+.5)*spacing))*R;
}
vec3 coronaField(float angle,float radial,float x){
    if(Seconds<=0.0)return vec3(0);
    float sectorAngle=mod(angle+PI*.25,TAU),sector=floor(sectorAngle/(PI*.5));
    float local=mod(sectorAngle,PI*.5),spacing=(PI*.5-.13)/16.0;
    float id=sector*16.0+clamp(floor((local-.065)/spacing),0.0,15.0);
    float illuminated=smoothstep(0.0,2.1,Seconds)*64.0;
    float ignition=smoothstep(id,id+1.0,illuminated);
    float grow=smoothstep(2.1,3.5,Seconds),bridge=smoothstep(3.3,3.65,Seconds);
    float reach=mix(.20,spacing*R*.5+.08,grow);
    float segment=1.0-smoothstep(reach,reach+.09,abs(connectionArc(angle)));
    float tubeGate=mix(segment*ignition,1.0,bridge)*(.36+.64*grow);
    float coronaGate=smoothstep(2.5,4.1,Seconds);
    float chargePulse=1.0+.22*exp(-pow((Seconds-3.65)/.22,2.0))*(1.0-smoothstep(4.0,4.3,Seconds));
    float wisp=0.0;
    if(coronaGate>0.0){
        float shoulder=exp(-radial*radial/.28-x*x/3.6);
        wisp=shoulder*sampleFusionWisp(angle,radial,x);
    }
    float center=radial+.012*sin(angle*35.0-Time*.45);
    float core=exp(-(x*x*2.2+center*center)/(.06075*1.75*1.75));
    float hot=exp(-(x*x*.9+center*center)/(.095*1.6*1.6));
    vec3 light=(vec3(.82,.93,1.0)*core*(6.666667/1.75)+vec3(.035,.40,1.0)*hot*(.30/1.6))*tubeGate*chargePulse;
    light+=vec3(.025,.38,1.0)*wisp*.84*coronaGate;
    float arc=connectionArc(angle);
    float band=exp(-pow(arc/.13,2.0))*ignition;
    float mantle=exp(-(x*x*1.4+radial*radial)/(.13*1.6*1.6));
    float ringMark=exp(-pow((sqrt(x*x*1.4+radial*radial)-.608)/.072,2.0));
    light+=vec3(.34,.77,1.0)*band*(mantle*(.95/1.6)+ringMark*(1.3/1.6));
    return light;
}
vec3 emission(vec3 p,vec3 direction){
    float radius=length(p.yz),radial=radius-R,x=p.x;
    if(abs(radial)>=.90||abs(x)>=3.20)return vec3(0);
    float angle=atan(p.z,p.y);
    float envelope=(1.0-smoothstep(.76,.90,abs(radial)))*(1.0-smoothstep(2.75,3.20,abs(x)));
    // Normalize by distance crossed through the elliptical tube, not distance travelled along it.
    // The radial component changes along a grazing ray, accounting for the ring's curvature.
    float radialDirection=dot(direction.yz,p.yz)/radius;
    float crossing=length(vec2(direction.x,radialDirection*.67419986)); // 1 / sqrt(2.2)
    float fullCrossing=sqrt(direction.x*direction.x+(1.0-direction.x*direction.x)/2.2);
    float rayWeight=(.26+.74*abs(direction.x))*mix(1.0,crossing/fullCrossing,smoothstep(2.1,3.65,Seconds));
    return coronaField(angle,radial,x)*envelope*rayWeight;
}
vec3 fusionIntegrate(vec3 ro,vec3 rd,vec2 interval,float offset) {
    float distance=max(interval.x,0.0),finish=interval.y;
    vec3 light=vec3(0);
    for(int i=0;i<256;i++) {
        if(distance>=finish)break;
        float stepSize=layerClipSegment(distance+offset,min(.10,finish-distance));
        vec3 energy=(emission(ro+rd*(distance+stepSize*.211324865),rd)+emission(ro+rd*(distance+stepSize*.788675135),rd))*.5;
        light+=energy*stepSize;
        layerAccumulateEmission(energy,stepSize,distance+offset);
        distance+=stepSize;
    }
    return light;
}
vec3 fusionToneMap(vec3 light,float gain) {
    light*=gain;
    float intensity=max(light.r,max(light.g,light.b));
    vec3 color=(light/max(intensity,.00001))*(1.0-exp(-intensity*1.55));
    return pow(max(color,vec3(0)),vec3(.70));
}
void fusionMain() {
    if(gl_FrontFacing || Seconds<=0.0)discard;
    vec3 direction=normalize(orthoCamera>0.5?localForward:localPosition-localEye);
    vec3 eye=orthoCamera>0.5?localPosition-direction*700.0:localEye;
    vec3 origin=eye-vec3(-18,0,0);
    vec3 eyeView=(ModelViewMat*vec4(eye,1)).xyz,dirView=(ModelViewMat*vec4(direction,0)).xyz;
    float z=texelFetch(SceneDepth,ivec2(effectScreenPixel()-SceneViewport),0).r*2.0-1.0;
    float viewZ=(ProjMat[3][2]-z*ProjMat[3][3])/(z*ProjMat[2][3]-ProjMat[2][2]);
    float finish=(viewZ-eyeView.z)/dirView.z;
    vec2 axial=vec2(0,finish);
    if(abs(direction.x)<.000001){if(abs(origin.x)>3.20)discard;}
    else{vec2 t=(vec2(-3.20,3.20)-origin.x)/direction.x;axial=vec2(max(0,min(t.x,t.y)),min(finish,max(t.x,t.y)));}
    if(axial.y<=axial.x)discard;
    float offset=axial.x;origin+=direction*offset;
    vec2 outer=cylinder(origin.yz,direction.yz,R+.90);outer=vec2(max(outer.x,0),min(outer.y,axial.y-offset));
    if(outer.y<=outer.x)discard;
    vec2 inner=cylinder(origin.yz,direction.yz,R-.90);
    layerBegin(eyeView,dirView);
    vec3 light=vec3(0);
    if(inner.y>inner.x&&inner.y>outer.x&&inner.x<outer.y){
        light+=fusionIntegrate(origin,direction,vec2(outer.x,min(outer.y,inner.x)),offset);
        light+=fusionIntegrate(origin,direction,vec2(max(outer.x,inner.y),outer.y),offset);
    }else light=fusionIntegrate(origin,direction,outer,offset);
    float gain=1.0;
    vec3 color=fusionToneMap(light,gain);
    if(max(color.r,max(color.g,color.b))<.001)discard;
    fusionColor=vec4(color,0);
#ifdef LIGHT_HUNTER_FUSION_LAYERED
    if(LayerMode==1){
        vec3 previous=LayerBase==0?vec3(0):fusionToneMap(prefixes[0],gain);
        for(int i=0;i<4;i++){
            vec3 mapped=LayerBase+i<LayerCount?fusionToneMap(prefixes[i+1],gain):color;
            plumeBins[i]=max(mapped-previous,vec3(0));previous=mapped;
        }
    }
#endif
}
