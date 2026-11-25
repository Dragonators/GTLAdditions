uniform vec3 uCam;uniform float uTime;
uniform float uBright;uniform float uWhite;uniform float uEmber;uniform float uAnti;uniform vec3 uEmberK;uniform float uRimK;
uniform vec3 uCore;uniform vec3 uMid;uniform vec3 uLane;uniform vec3 uLimb;uniform vec3 uSpot;uniform vec3 uRim;uniform float uGran;uniform float uSpots;uniform vec2 uLimbK;
uniform vec3 uEclAx;uniform vec3 uEclE1;uniform float uEclK;uniform vec3 uEclCol;
varying vec3 vN;varying vec3 vP;varying vec3 vO;
float h31(vec3 p){p=fract(p*vec3(.1031,.1030,.0973));p+=dot(p,p.yxz+33.33);return fract((p.x+p.y)*p.z);}
vec3 h33(vec3 p){p=fract(p*vec3(.1031,.1030,.0973));p+=dot(p,p.yxz+33.33);return fract((p.xxy+p.yxx)*p.zyx);}
float vnoise(vec3 p){vec3 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);
  return mix(mix(mix(h31(i),h31(i+vec3(1,0,0)),f.x),mix(h31(i+vec3(0,1,0)),h31(i+vec3(1,1,0)),f.x),f.y),
             mix(mix(h31(i+vec3(0,0,1)),h31(i+vec3(1,0,1)),f.x),mix(h31(i+vec3(0,1,1)),h31(i+vec3(1,1,1)),f.x),f.y),f.z);}
float fbm(vec3 p){float a=.5,s=0.;for(int i=0;i<5;i++){s+=a*vnoise(p);p=p*2.03+vec3(1.7,9.2,3.1);a*=.5;}return s;}
float fbm3(vec3 p){float a=.5,s=0.;for(int i=0;i<3;i++){s+=a*vnoise(p);p=p*2.07+vec3(5.3,1.1,7.7);a*=.5;}return s/.875;}
vec3 worley(vec3 p){vec3 i=floor(p),f=fract(p);float d1=8.,d2=8.,id=0.;
  for(int z=-1;z<=1;z++)for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++){vec3 g=vec3(float(x),float(y),float(z)),o=h33(i+g);vec3 r=g+o-f;float d=dot(r,r);
    if(d<d1){d2=d1;d1=d;id=h31(i+g+.5);}else if(d<d2)d2=d;}
  return vec3(sqrt(d1),sqrt(d2),id);}
vec3 hsv(float h,float s,float v){vec3 k=clamp(abs(mod(h*6.+vec3(0.,4.,2.),6.)-3.)-1.,0.,1.);return v*mix(vec3(1.),k,s);}
void main(){
  vec3 o=normalize(vO),n=normalize(vN),V=normalize(uCam-vP);float mu=max(0.,dot(n,V)),t=uTime;
  vec3 q=o*2.4+vec3(0.,0.,t*.006);float warp=fbm3(q*1.4+7.1),plasma=fbm(q*1.6+warp*1.2);
  vec3 w1=worley(o*uGran+warp*.35+vec3(t*.01,0.,0.)),w2=worley(o*uGran*2.15+vec3(0.,t*.013,warp*.5));
  float c1=1.-smoothstep(.0,.95,w1.x),c2=1.-smoothstep(.0,.95,w2.x),px=length(fwidth(o))*uGran;
  float gran=mix(.5,c1,1.-smoothstep(.3,.8,px))*.65+mix(.5,c2,1.-smoothstep(.14,.37,px))*.35;
  vec3 wn=worley(o*5.5+warp*.9+fbm3(o*9.)*.25);float net=(1.-smoothstep(.0,.16,wn.y-wn.x))*.5;
  float lat=abs(o.z),band=smoothstep(.06,.18,lat)*smoothstep(.56,.34,lat);
  float sf=fbm(o*1.7+vec3(13.,2.,5.)+vec3(0.,0.,t*.002));
  float um=smoothstep(.705,.735,sf)*band*uSpots,pen=smoothstep(.645,.705,sf)*band*uSpots;
  float I=.86+.24*(gran-.5)+.3*(plasma-.5)+.07*net;I*=1.-.4*pen;I*=1.-.8*um;
  float ld=1.-uLimbK.x*(1.-mu)-uLimbK.y*(1.-mu)*(1.-mu);
  vec3 col=mix(uLane,uMid,smoothstep(.55,.9,I));col=mix(col,uCore,smoothstep(.86,1.08,I));col=mix(col,uSpot,um*.9);
  col*=ld*uBright*1.12;col=mix(col,uLimb*(.55+.6*ld),pow(1.-mu,1.6)*.6);
  col+=uCore*.2*smoothstep(.6,.82,plasma)*pow(1.-mu,1.5)*(1.-um);
  col+=uRim*smoothstep(.15,.0,mu)*.6;
  col=mix(col,vec3(1.,.98,.95),uWhite);col=mix(col,col*uEmberK,uEmber);
  vec3 anti=vec3(.015,.012,.02)+vec3(1.,.82,.55)*pow(1.-mu,5.)*2.2*uRimK;
  col=mix(col,anti,uAnti);
  if(uEclK>0.){float d=dot(n,uEclAx),fw=fwidth(d),ph=atan(dot(n,cross(uEclAx,uEclE1)),dot(n,uEclE1))/6.2831853;   // The ecliptic stays fixed in space.
    float ln=1.-smoothstep(.0025,.0025+fw*1.5,abs(d)),tk=step(fract(ph*72.),.05)*(1.-smoothstep(.008,.008+fw,abs(d)))+step(fract(ph*12.+.006),.012)*(1.-smoothstep(.02,.02+fw,abs(d)));
    col+=uEclCol*(ln+tk*.7)*uEclK*smoothstep(.0,.12,mu);}
  gl_FragColor=vec4(col,1.);}