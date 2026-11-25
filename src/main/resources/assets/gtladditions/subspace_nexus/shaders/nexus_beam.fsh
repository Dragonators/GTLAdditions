uniform vec3 uCam;uniform float uGlow;
uniform vec3 uBeamPhase; // jet knots, brightness bands, rainbow drift (cycles)
uniform vec2 uNoisePhase[6]; // bounded Y/Z coordinates for each layer's three octaves
uniform vec3 uZ;uniform vec3 uR;uniform float uRK;uniform vec3 uRs;uniform float uTop;uniform float uBottom;uniform float uBotK;uniform float uTopK;uniform vec4 uStar;uniform float uSoft;uniform float uRootK;uniform vec4 uDisc;uniform float uDiscK;
uniform vec3 uColLow;uniform vec3 uColHigh;uniform float uAlpha;uniform float uSkyFade;uniform float uProf;uniform float uVol;
uniform vec4 uBands;uniform float uRb;uniform float uRbMode;uniform vec3 uPulse[10];uniform float uPulseMode;uniform float uPulseK;
uniform float uMode;uniform float uGain;uniform vec3 uHot;uniform vec3 uCool;uniform vec4 uJet;uniform vec3 uJet2;
varying vec3 vP;
float h31(vec3 p){p=fract(p*vec3(.1031,.1030,.0973));p+=dot(p,p.yxz+33.33);return fract((p.x+p.y)*p.z);}
vec3 h33(vec3 p){p=fract(p*vec3(.1031,.1030,.0973));p+=dot(p,p.yxz+33.33);return fract((p.xxy+p.yxx)*p.zyx);}
// Wrap lattice neighbours as well as CPU offsets, so crossing a period boundary is seamless.
float beamHash(vec3 p){p.yz=mod(p.yz,1024.);return h31(p);}
float vnoise(vec3 p){vec3 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);
  return mix(mix(mix(beamHash(i),beamHash(i+vec3(1,0,0)),f.x),mix(beamHash(i+vec3(0,1,0)),beamHash(i+vec3(1,1,0)),f.x),f.y),
             mix(mix(beamHash(i+vec3(0,0,1)),beamHash(i+vec3(1,0,1)),f.x),mix(beamHash(i+vec3(0,1,1)),beamHash(i+vec3(1,1,1)),f.x),f.y),f.z);}
float fbm(vec3 p){float a=.5,s=0.;for(int i=0;i<5;i++){s+=a*vnoise(p);p=p*2.03+vec3(1.7,9.2,3.1);a*=.5;}return s;}
float beamNoise(float lateral,float height,int layer){
  float x=lateral*(layer==0?2.2:5.),y=height/(layer==0?16.:6.),a=.5,s=0.;
  for(int i=0;i<3;i++){
    vec2 phase=uNoisePhase[layer*3+i];s+=a*vnoise(vec3(x,y+phase.x,phase.y));
    x=x*2.07+5.3;y*=2.07;a*=.5;
  }
  return s/.875;
}
vec3 worley(vec3 p){vec3 i=floor(p),f=fract(p);float d1=8.,d2=8.,id=0.;
  for(int z=-1;z<=1;z++)for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++){vec3 g=vec3(float(x),float(y),float(z)),o=h33(i+g);vec3 r=g+o-f;float d=dot(r,r);
    if(d<d1){d2=d1;d1=d;id=h31(i+g+.5);}else if(d<d2)d2=d;}
  return vec3(sqrt(d1),sqrt(d2),id);}
vec3 hsv(float h,float s,float v){vec3 k=clamp(abs(mod(h*6.+vec3(0.,4.,2.),6.)-3.)-1.,0.,1.);return v*mix(vec3(1.),k,s);}
float rAt(vec3 R,float z){return z<uZ.y?mix(R.x,R.y,(z-uZ.x)/(uZ.y-uZ.x)):mix(R.y,R.z,min(1.,(z-uZ.y)/(uZ.z-uZ.y)));}
vec3 rainbow(vec3 col,float dist,float x){if(uRb<=0.)return col;vec3 rc;float k=uRb;
  if(uRbMode<1.5)rc=hsv(.74*(1.-clamp((dist/rAt(uRs,vP.z)-.08)/.62,0.,1.)),.75,1.);
  else if(uRbMode<2.5)rc=hsv(.74*clamp((vP.z-106.)/364.,0.,1.),.75,1.);
  else if(uRbMode<3.5){rc=hsv(vP.z/uBands.x*.25-uBeamPhase.z,.8,1.);k*=x;}
  else{rc=hsv(vP.z/240.,.8,1.);k*=x;}
  if(uRbMode<2.5)rc*=mix(1.,clamp(.72/max(dot(rc,vec3(.2126,.7152,.0722)),.1),1.,1.8),k);
  return mix(col,rc,k);}
float pulses(){float pa=0.;
  for(int i=0;i<10;i++){vec3 p=uPulse[i];if(p.y<=0.)continue;float dz=vP.z-p.x;
    if(uPulseMode<1.5)pa+=p.y*(dz>0.?exp(-dz*dz/5.):exp(dz*3./p.z));
    else if(uPulseMode<2.5)pa+=p.y*exp(-dz*dz/(p.z*p.z*.02));
    else pa+=p.y*exp(-dz*dz/(p.z*p.z*.0625));}
  return uPulseMode>2.5&&uPulseMode<3.5?1.-exp(-pa):pa;}
float finish(float ds,float wd){float m=mix(1.,smoothstep(uTop,uTop-14.,vP.z),uTopK)*uGlow;if(uBotK>0.)m*=smoothstep(uBottom,uBottom+uBotK,vP.z);if(uSoft>0.)m*=smoothstep(0.,uSoft,ds);
  if(uDiscK*wd>0.){vec3 rd=normalize(vP-uCam),oc=uCam-uDisc.xyz;float b=dot(oc,rd),q=b*b-dot(oc,oc)+uDisc.w*uDisc.w;
    if(q>0.&&-b-sqrt(q)>length(vP-uCam))m*=1.-uDiscK*wd*smoothstep(0.,uDisc.w*uDisc.w*.15,q);}
  return m;}
void main(){
  if(vP.z>uTop||vP.z<uBottom)discard;
  float ds=length(vP-uStar.xyz)-uStar.w;if(ds<0.)discard;
  vec3 d=vP-uCam;float hl=length(d.xy),cr=d.y*uCam.x-d.x*uCam.y,dist=hl>1e-3?abs(cr)/hl:length(uCam.xy);
  float x=min(1.,dist/(rAt(uR,vP.z)*uRK));
  float h=clamp((vP.z-uZ.x)/(uZ.y-uZ.x),0.,1.),sky=clamp((vP.z-uZ.y)/(uZ.z-uZ.y),0.,1.),f=(1.-.5*h)*pow(1.-sky,uSkyFade);
  if(vP.z<uStar.z)f*=uRootK;
  vec3 col=mix(uColLow,uColHigh,h);
  float b=uBands.z>0.?1.+uBands.z*(.5+.5*sin((vP.z/uBands.x-uBeamPhase.y)*6.2831853)-.35):1.;
  if(uMode>.5){float u=sign(cr)*x,I,rim=1.-smoothstep(.8,1.,x);vec3 tint=col;
    if(uMode<1.5)I=.5*exp(-22.*x*x)+.3*exp(-4.5*x*x)+.2*exp(-1.3*x*x)*sqrt(1.-x*x);
    else{float zz=vP.z<uStar.z?2.*uStar.z-vP.z:vP.z,s=max(0.,zz-uZ.x-96.);
      float n1=beamNoise(u,zz,0),n2=beamNoise(u,zz,1),xe=x*(1.+.5*(n1-.5));
      I=(.55*exp(-9.*xe*xe)+.45*exp(-2.4*xe*xe))*(.4+1.2*n2);
      I*=1.+uJet.z*pow(.5+.5*sin((zz/uJet.x-uBeamPhase.x)*6.2831853),uJet2.z);
      if(uJet.w>0.){float p=fract(s/uJet2.x),w=.55*(1.-abs(2.*p-1.));I*=1.+uJet.w*exp(-s/90.)*smoothstep(w,w*.35,abs(u));}
      I*=1.+.45*exp(-s/18.);
      tint=mix(uHot,mix(col,uCool,uJet2.y),clamp(xe*1.3+s/500.,0.,1.));}
    tint=rainbow(tint,dist,x);
    I=I*uAlpha*f*rim*b+uPulseK*pulses()*uAlpha*f*exp(-3.*x*x)*rim;
    vec3 c=.5*(1.-exp(-I*uGain*tint));float m=finish(ds,smoothstep(.12,.55,x));
    gl_FragColor=vec4(c*m,max(c.r,max(c.g,c.b))*m);return;}
  float prof=exp(-uProf*x*x)*mix(1.,sqrt(1.-x*x),uVol),a=.5*uAlpha*prof*f*b;
  col=rainbow(col,dist,x);
  if(uPulseK>0.)a+=.5*uAlpha*prof*f*uPulseK*pulses();
  a*=finish(ds,1.);
  gl_FragColor=vec4(col*a,a);}
