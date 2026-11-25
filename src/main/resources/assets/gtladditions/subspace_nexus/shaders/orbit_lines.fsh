uniform vec3 uCol;uniform float uA;uniform float uR;uniform float uCoreW;uniform float uHaloW;uniform float uHaloA;uniform float uTh0;uniform float uDir;uniform float uGap;uniform float uFloor;uniform float uFade;
uniform float uTick;uniform float uTickL;uniform float uFlow;uniform float uTime;uniform float uGlow;uniform vec2 uArc;uniform vec2 uDash;uniform float uDepthK;
varying float vTh;varying float vS;varying float vW;varying float vPx;varying float vD;
void main(){
  if(fract(vTh-uArc.x)>uArc.y)discard;
  if(uDash.x>.5&&fract(vTh*uDash.x)>uDash.y)discard;
  float C=6.2831853*uR,b=fract(uDir*(vTh-uTh0)),gd=min(b,1.-b)*C,gapK=uGap<0.?1.:smoothstep(uGap,uGap+1.2,gd);if(gapK<=0.)discard;
  float y=abs(vS)*vW,px=max(vPx,1e-5),cw=max(uCoreW,.55*px),coreA=min(1.,uCoreW/cw),core=(1.-smoothstep(cw*.35,cw+.6*px,y))*coreA;
  float hw=max(uHaloW*.5,1.6*px),halo=exp(-y*y/(hw*hw))*min(1.,uHaloW*.5/hw)*(1.-smoothstep(.75,1.,abs(vS)));
  float tail=mix(uFloor,1.,pow(1.-b,uFade)),I=core+uHaloA*halo;
  if(uFlow>0.){float n=max(1.,floor(C/22.+.5)),gs=C/n,ph=fract((vTh*C+uDir*uTime*5.)/gs)-.5,pu=exp(-ph*ph*gs*gs/9.);I*=1.+uFlow*pu;tail=max(tail,uFloor+uFlow*.35*pu);}
  float tk=0.;if(uTick>0.){float st=C/72.,fd=smoothstep(5.,12.,st/px),d5=abs(fract(vTh*72.+.5)-.5)*st,d30=abs(fract(vTh*12.+.5)-.5)*C/12.,tw=max(.014,.45*px),ta=min(1.,.014/tw);
    float t5=(1.-smoothstep(tw,tw+px*.7,d5))*(1.-smoothstep(.09*uTickL,.09*uTickL+px,y)),t30=(1.-smoothstep(tw,tw+px*.7,d30))*(1.-smoothstep(.2*uTickL,.2*uTickL+px,y));tk=uTick*max(t5*.5,t30*.95)*ta*fd;}
  float a=(I*tail+tk*max(tail,.45))*uA*mix(1.,uDepthK,vD)*gapK*uGlow;
  gl_FragColor=vec4(mix(uCol,vec3(1.),.32*core*tail)*a,a);}