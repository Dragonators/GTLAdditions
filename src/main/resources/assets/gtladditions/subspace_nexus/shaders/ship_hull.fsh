flat varying float nexusIntensityValue;
uniform float uAmb;uniform float uEmissive;uniform float uNeg;uniform vec3 uNegCol;uniform float uNegGlow;uniform vec3 uCam;uniform float uTime;
uniform float uTMode;uniform vec3 uTDark;uniform vec3 uTMid;uniform vec3 uTBright;uniform float uTLit;uniform float uTSpec;uniform vec3 uTRim;uniform float uTRimPow;uniform float uTRimGain;
uniform vec3 uTEmi;uniform float uTEmiKeep;uniform float uTEmiGain;uniform float uTVein;uniform vec3 uTVeinCol;uniform float uTVeinScale;
uniform float uTRing;uniform vec3 uTRingCol;uniform vec3 uTRingEmi;uniform float uTEnv;uniform vec3 uTEnvHi;uniform vec3 uTEnvLo;
varying vec3 vN;varying vec3 vC;varying vec3 vE;varying vec3 vP;varying vec3 vO;
vec3 h33(vec3 p){p=vec3(dot(p,vec3(127.1,311.7,74.7)),dot(p,vec3(269.5,183.3,246.1)),dot(p,vec3(113.5,271.9,124.6)));return fract(sin(p)*43758.5453);}
float crack(vec3 p){vec3 i=floor(p),f=fract(p),c1=vec3(0.),c2=vec3(0.);float d1=8.,d2=8.;
  for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){vec3 g=vec3(float(x),float(y),float(z)),r=g+h33(i+g)-f;float d=dot(r,r);
    if(d<d1){d2=d1;c2=c1;d1=d;c1=i+g;}else if(d<d2){d2=d;c2=i+g;}}
  float e=sqrt(d2)-sqrt(d1),keep=step(.58,h33(c1+c2).x);
  return keep*((1.-smoothstep(0.,.04,e))+.22*(1.-smoothstep(0.,.22,e)));}
void main(){vec3 n=normalize(vN);float s=.6*n.x*n.x+.8*n.y*n.y+(n.z>0.?1.:.5)*n.z*n.z;vec3 base=vC*s*uAmb;vec3 col=base;
  if(uEmissive>.5){float k=clamp(nexusIntensityValue,0.,1.);col=mix(base,vE,k)+vE*max(nexusIntensityValue-1.,0.)*.35;}
  if(uNeg>0.){vec3 V=normalize(uCam-vP);float mu=clamp(abs(dot(n,V)),0.,1.),fm=1.-mu,fm2=fm*fm;vec3 alt;
    float ring=uTRing*step(length(vO.xy),1.62)*step(-.05,vO.z)*step(vO.z,.75);   // the round collar (+ hub) in model units
    if(uTMode<.5)alt=uEmissive>.5?uNegCol*uNegGlow+vec3(.02):vec3(.02,.025,.035)+uNegCol*fm2*fm*1.4;
    else if(uEmissive>.5)alt=mix(uTEmiKeep>.5?vE:uTEmi,uTRingEmi,ring)*uTEmiGain*(.3+.7*clamp(nexusIntensityValue,0.,1.4))+vec3(.015);
    else{float tb=smoothstep(.15,.85,dot(vC,vec3(.3,.59,.11)));vec3 b=tb<.5?mix(uTDark,uTMid,tb*2.):mix(uTMid,uTBright,tb*2.-1.);
      float lr=ring*tb;b=mix(b,uTRingCol,lr);
      vec3 L=normalize(vec3(.35,-.45,.82)),hv=L+V;float hl2=dot(hv,hv),sp=hl2>1e-10?pow(max(dot(n,hv*inversesqrt(hl2)),0.),36.)*uTSpec:0.;
      alt=b*mix(1.,s*(.35+.65*uAmb)*1.3,max(uTLit,lr))+mix(vec3(1.),mix(uTBright,uTRingCol,lr),.6)*sp*(.4+.6*uAmb)+uTRim*pow(fm,uTRimPow)*uTRimGain;
      if(uTEnv>0.){vec3 rr=reflect(-V,n);alt+=mix(uTEnvLo,uTEnvHi,smoothstep(-.35,.65,rr.z))*(.05+.95*fm2*fm2)*uTEnv;}
      if(uTVein>0.)alt+=uTVeinCol*crack(vO*uTVeinScale)*uTVein*(.75+.25*sin(uTime*1.6+dot(vO,vec3(3.,2.,5.))));}
    col=mix(col,alt,uNeg);}
  gl_FragColor=vec4(col,1.);}