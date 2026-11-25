uniform vec3 uCam;uniform vec3 uStarC;uniform vec3 uIce;uniform vec3 uKey;uniform vec3 uGold;uniform vec3 uRZ;uniform float uLook;uniform float uE;uniform float uGlow;uniform float uInv;uniform float uStarR;uniform float uFlow;uniform float uFlowL;uniform float uEdgeK;uniform sampler2DArray uBlockAtlas;uniform sampler2D uBlockFrames;
varying vec3 vP;varying vec3 vN;varying vec3 vT;varying float vRole;varying vec2 vUV;varying float vBoost;varying float vLod;varying float vRad;
float hh3(vec3 p){return fract(sin(dot(p,vec3(127.1,311.7,74.7)))*43758.5453);}
vec3 envD(vec3 P,vec3 R,vec3 ice,float bk,float bw){
  float up=R.z,az=atan(R.y,R.x);vec3 e=mix(vec3(.006,.009,.018),vec3(.05,.07,.13),smoothstep(-.1,.6,up));
  e+=ice*exp(-pow((up-.03)/.15,2.))*(.18+.4*pow(.5+.5*cos(az*6.),4.))+vec3(.006,.007,.009)*smoothstep(.05,-.5,up);
  float rr=dot(R.xy,R.xy);if(rr>1e-4){float t=-dot(P.xy,R.xy)/rr,z=P.z+t*R.z;vec2 q=P.xy+t*R.xy;
    if(t>0.&&z>10.&&z<980.){float d2=dot(q,q);e+=vec3(.78,.8,1.)*bk*(exp(-d2/(6.*bw))*.9+exp(-d2/(60.*bw))*.22)*smoothstep(980.,600.,z);}}
  return e;}
void main(){
  vec3 n=normalize(vN),V=normalize(uCam-vP);if(dot(n,V)<0.)n=-n;
  vec3 T=normalize(vT),Ld=normalize(uStarC-vP),H=normalize(Ld+V),R=reflect(-V,n);
  float NV=max(0.,dot(n,V)),role=floor(vRole+.5),al=asin(min(uStarR/length(uStarC-vP),.999)),rA=acos(clamp(dot(R,Ld),-1.,1.));
  vec3 Ls=normalize(vec3(.35,-.45,.82)),sky=vec3(.42,.52,.72)*.75,Hs=normalize(Ls+V),fill=vec3(.16,.2,.32)*(.55+.45*n.z)+vec3(.045);
  vec3 lit=fill+uKey*(.2+.8*max(0.,dot(n,Ld)*.8+.2))+sky*(.2+.8*max(0.,dot(n,Ls)));
  float E=1.15*(1.-exp(-1.7*uE*(1.+.55*vBoost))),px=length(fwidth(vP));vec3 glowC=mix(uIce,vec3(1.),min(.8,.03+.3*vBoost));
  bool ti=uLook>2.5&&uLook<3.5,bead=uLook>4.5&&uLook<5.5,lamp=uLook<1.5,steelFar=uLook<2.5;
  vec3 alb=vec3(.05,.055,.065),F0=vec3(.4,.43,.48);vec3 Bt=normalize(cross(n,T));float bh=dot(Bt,H),an=pow(sqrt(max(0.,1.-bh*bh)),70.)*pow(max(0.,dot(n,H)),4.)*1.1;
  float bk=.5,bw=2.,envK=.9,fade=1.-smoothstep(.008,.03,px);
  if(ti){float g=(hh3(floor(vP*33.))-.5)*.1*fade,ln=(hh3(vec3(floor(vUV.y*30.),5.,2.))-.5)*fade;alb=vec3(.72,.73,.74)*.66*(1.+g+.1*ln);F0=clamp(alb*.8+.04,0.,.6);an=pow(sqrt(max(0.,1.-bh*bh)),36.)*pow(max(0.,dot(n,H)),3.)*.9*(1.+ln);lit=mix(lit,vec3(dot(lit,vec3(.333))),.65);bk=.45;bw=3.2;envK=.9;}
  else if(bead){alb=vec3(.02);F0=vec3(.86,.88,.92);an=pow(max(0.,dot(n,H)),400.)*2.;bk=1.;bw=.6;envK=1.;}
  else{float ln=(hh3(vec3(floor(vUV.y*40.),3.,7.))-.5)*fade;alb*=1.+.5*ln;an*=1.+1.2*ln;}
  if(abs(role-1.)<.5){F0=ti?min(F0*1.35,vec3(.85)):vec3(.72,.75,.8);an=pow(max(0.,dot(n,H)),36.)*1.4;if(ti)alb*=1.2;}
  vec3 F=F0+(1.-F0)*pow(1.-NV,5.),Env=envD(vP,R,uIce,bk,bw);if(ti)Env=mix(Env,vec3(dot(Env,vec3(.333))),.55)*(.75+.5*smoothstep(-1.,1.,n.z+.6*vUV.y))+vec3(.1,.105,.115);else Env+=vec3(.012,.014,.018);
  vec3 col=alb*lit*(ti?.8:1.)+F*(Env*envK+uKey*an);
  float far=0.;vec3 farC=vec3(0.);
  if(abs(role-8.)<.5){
    vec4 f=texelFetch(uBlockFrames,ivec2(0,0),0);vec2 tuv=vec2(.5+vUV.y*.5,vUV.x/(2.*uRZ.z)),dx=dFdx(tuv),dy=dFdy(tuv);
    vec2 tw=fract(tuv);vec3 tx=mix(textureGrad(uBlockAtlas,vec3(tw,f.x),dx,dy).rgb,textureGrad(uBlockAtlas,vec3(tw,f.y),dx,dy).rgb,f.z);   // the atlas clamps: tile here, gradients from the unwrapped coordinate
    col=col*.35+tx*uE*(.86+.14*NV)*(1.+.28*vBoost)+vec3(.75,.95,1.)*vBoost*.1*uE;farC=uIce*uE*.95*(1.+.2*vBoost);far=1.;}
  else if(abs(role-2.)<.5){float c=exp(-vUV.y*vUV.y*9.);col=mix(glowC,vec3(1.),c*.22)*E*(.84+.26*c);farC=glowC*E;far=1.;}
  else if(abs(role-3.)<.5){col=alb*.4+uIce*E*.38;farC=uIce*E*.7;far=1.;}
  else if(role<.5&&steelFar){col+=uIce*E*.16*exp(-max(0.,abs(vUV.y)-.4)/.14)*(lamp?0.:1.);farC=uIce*E*(lamp?.8:.68);far=1.;}
  else if(abs(role-6.)<.5&&steelFar){farC=uIce*E*(lamp?.8:.6);far=1.;}
  else if(abs(role-1.)<.5&&steelFar){farC=uIce*E*(lamp?.8:.6);far=1.;}
  else if(abs(role-7.)<.5){col=col*.6+glowC*E*.5;}
  else if(abs(role-4.)<.5){
    float w=.5+.5*cos(6.2831853*vUV.y*14.),aaw=clamp(fwidth(vUV.y*14.)*1.5,0.,1.),depth=clamp((.98-vRad)/.62,0.,1.),ao=1.-.68*pow(depth,1.15);w=mix(w,.5,aaw);
    float fl=uLook>5.5?.12+.88*pow(.5+.5*sin((vUV.x-uFlow)*6.2831853/uFlowL),2.):1.;
    vec3 b=vec3(.03,.033,.04)*(.8+.4*w);col=(b*lit+F*(Env*envK+uKey*an*(.7+.6*w)))*ao+glowC*E*fl*1.2*smoothstep(.25,.55,depth);farC=col*.5+uIce*E*fl*.46;far=1.;}
  else if(bead){
    float dsk=smoothstep(al+.025,al-.025,rA);col=mix(col,uKey*1.7,dsk*(1.-uInv));col*=1.-dsk*uInv;col+=uGold*1.8*exp(-pow((rA-al)/.05,2.))*uInv+uIce*vBoost*.22*E;}
  if(uEdgeK>0.&&abs(role-1.)<.5)col+=glowC*uE*uEdgeK;
  if(far>.5)col=mix(farC,col,vLod);
  gl_FragColor=vec4(col,1.);}