uniform vec3 uCam;uniform float uTime;uniform vec3 uStarC;uniform vec3 uLight;uniform float uAmb;uniform float uKind;uniform float uSeed;
uniform vec3 uC1;uniform vec3 uC2;uniform vec3 uC3;uniform vec3 uAtmo;uniform float uAtmoK;uniform float uEmis;uniform vec3 uEmisCol;uniform float uBandF;
uniform vec4 uRingN;uniform vec3 uRingC;uniform vec2 uRingR;uniform float uNeg;uniform float uRimG;uniform vec3 uGold;
uniform sampler2D uMap;uniform float uMapK;uniform float uHue;uniform vec3 uTint;uniform sampler2D uHMap;uniform float uHK;uniform sampler2D uRingTex;uniform float uRingTexK;
uniform float uCityK;uniform float uBumpFix;uniform vec4 uShad[4];uniform float uShadW;uniform vec3 uShadC;uniform float uStarR;   // uBumpFix is the body radius in blocks.
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
float sphLight(float nc,float sa){float ca=sqrt(1.-sa*sa),a=ca*nc,b=sa*sqrt(max(0.,1.-nc*nc));return (a>=b?a:a<=-b?0.:(a*acos(clamp(-a/max(b,1e-5),-1.,1.))+sqrt(max(b*b-a*a,0.)))/3.14159265)/ca;}
float cityLights(vec3 o,float land){
  float fw=length(fwidth(o)),D=smoothstep(.4,.66,fbm3(o*3.1+uSeed*3.))*smoothstep(.34,.72,fbm3(o*13.+uSeed*7.))*mix(.42,1.,land);
  vec3 g1=o*150.+uSeed*3.,g2=o*62.+uSeed*5.;
  float p1=step(1.-D*.8,h31(floor(g1)))*(1.-smoothstep(.2,.48,length(fract(g1)-.5))),p2=step(1.-D*.3,h31(floor(g2)+3.))*(1.-smoothstep(.22,.5,length(fract(g2)-.5)));
  return mix(D*.34,max(p1,p2*1.4),1.-smoothstep(.006,.016,fw));}
float ringDen(float x){return (.55+.45*sin(x*41.+uSeed*7.))*(1.-smoothstep(.05,.0,abs(x-.63)))*(1.-smoothstep(.025,.0,abs(x-.3))*.8);}
vec3 rgb2hsv(vec3 c){vec4 K=vec4(0.,-1./3.,2./3.,-1.);vec4 p=mix(vec4(c.bg,K.wz),vec4(c.gb,K.xy),step(c.b,c.g));vec4 q=mix(vec4(p.xyw,c.r),vec4(c.r,p.yzx),step(p.x,c.r));float d=q.x-min(q.w,q.y);return vec3(abs(q.z+(q.w-q.y)/(6.*d+1e-10)),d/(q.x+1e-10),q.x);}
vec4 texEq(sampler2D m,vec3 o){   // equirectangular lookup; derivatives taken from whichever longitude chart has no wrap here, so there is no seam line
  vec2 uv=vec2(atan(o.y,o.x)/6.2831853+.5,asin(clamp(o.z,-1.,1.))/3.14159265+.5);float u2=fract(uv.x+.5);
  vec2 dx=vec2(dFdx(uv.x),dFdx(uv.y)),dy=vec2(dFdy(uv.x),dFdy(uv.y));float dx2=dFdx(u2),dy2=dFdy(u2);
  if(abs(dx2)<abs(dx.x))dx.x=dx2;if(abs(dy2)<abs(dy.x))dy.x=dy2;return textureGrad(m,uv,dx,dy);}
void main(){
  vec3 o=normalize(vO),n=normalize(vN),V=normalize(uCam-vP),L=normalize(uStarC-vP);float mu=max(0.,dot(n,V)),t=uTime,k=uKind;
  vec3 base=uC1,emis=vec3(0.);float spec=0.,pat=.5,bump=0.;
  if(k<.5){            // 0 rocky, cratered
    float h=fbm(o*3.2+uSeed);vec3 w=worley(o*4.6+uSeed*3.);float cr=smoothstep(.42,.06,w.x),rim=smoothstep(.1,.0,abs(w.x-.4));
    base=mix(uC1,uC2,smoothstep(.3,.7,h))*(.82+.3*h);base=mix(base,base*.62,cr*.55);base+=uC3*rim*.22;pat=h;bump=rim-cr*.4;
  }else if(k<1.5){     // 1 lava world.
    float h=fbm(o*2.8+uSeed);vec3 wp=o*3.4+uSeed*2.+(vec3(fbm3(o*5.),fbm3(o*5.+4.1),fbm3(o*5.+8.3))-.5)*.55;vec3 w=worley(wp+vec3(0.,0.,t*.004));
    float wid=.02+.07*fbm3(o*4.+uSeed*3.),hot=smoothstep(.35,.6,fbm3(o*2.3+uSeed*7.)),crack=(1.-smoothstep(.0,wid,w.y-w.x))*hot;
    float pool=smoothstep(.64,.74,fbm(o*1.8+uSeed*5.));base=mix(uC1,uC2,h)*(1.-.35*(1.-smoothstep(.0,.05,w.y-w.x))*(1.-hot));pat=h;
    emis=uEmisCol*(crack*(.75+.25*sin(t*1.3+w.z*20.))+pool*.85)*uEmis;bump=-crack;
  }else if(k<2.5){     // 2 ocean world with clouds
    float h=fbm(o*2.2+uSeed),land=smoothstep(.5,.56,h),ice=smoothstep(.8,.9,abs(o.z));
    vec3 sea=mix(uC1*.75,uC1,smoothstep(.2,.5,h));base=mix(sea,mix(uC2,uC3,smoothstep(.56,.75,h)),land);base=mix(base,vec3(.92,.95,.98),ice);
    float cl=smoothstep(.48,.78,fbm(o*3.3+vec3(t*.012,0.,0.)+uSeed*2.));base=mix(base,vec3(.96),cl*.85);spec=(1.-land)*(1.-cl)*(1.-ice);pat=h;
  }else if(k<3.5){     // 3 banded giant
    float tu=fbm(vec3(o.xy*3.,o.z*9.)+uSeed+vec3(t*.006,0.,0.));float b=sin(o.z*uBandF+tu*2.6);
    base=mix(uC1,uC2,smoothstep(-.6,.6,b));base=mix(base,uC3,smoothstep(.55,.95,sin(o.z*uBandF*.5+tu*4.)));
    vec2 sp=vec2(atan(o.y,o.x)-.9,(o.z+.32)*3.2);float st=exp(-dot(sp*vec2(1.6,1.),sp*vec2(1.6,1.))*4.);base=mix(base,uC3*1.15,st*.8);pat=b*.5+.5;
    emis=uEmisCol*uEmis*(1.-smoothstep(.0,.035,abs(o.z)))*(.75+.25*sin(atan(o.y,o.x)*6.-t*.8));
  }else if(k<4.5){     // 4 crystal world.
    vec3 w=worley(o*3.6+uSeed),w2=worley(o*8.+uSeed*2.);vec3 fn=normalize(h33(vec3(w.z*91.,w.z*17.,w.z*53.))-.5),fn2=normalize(h33(vec3(w2.z*37.,w2.z*71.,w2.z*13.))-.5);n=normalize(n+fn*.5+fn2*.18);
    float lit=step(.45,h31(vec3(w.z*13.,3.,7.)));base=mix(uC1,uC2,w.z);spec=1.;pat=w.z;
    emis=uEmisCol*uEmis*((1.-smoothstep(.0,.05,w.y-w.x))*(.25+.75*lit)+(1.-smoothstep(.0,.03,w2.y-w2.x))*.25);
  }else if(k<5.5){     // 5 night-side cities
    float h=fbm(o*3.+uSeed),land=smoothstep(.45,.52,h);base=mix(uC1,mix(uC2,uC3,h),land);pat=h;
    float lights=step(.78,h31(floor(o*58.)))*smoothstep(.5,.62,fbm(o*5.+uSeed*3.))*land;
    emis=uEmisCol*lights*uEmis*smoothstep(.05,-.25,dot(n,L));
  }else{               // 6 icy moon
    vec3 w=worley(o*3.+uSeed);float cr=1.-smoothstep(.0,.05,w.y-w.x);base=mix(uC1,uC2,fbm(o*4.+uSeed));base=mix(base,uC3,cr*.7);pat=w.z;
  }
  if(uMapK>.5&&(k<3.5||k>4.5)){
    vec3 tc=texEq(uMap,o).rgb;if(uHue!=0.){vec3 hv=rgb2hsv(tc);tc=hsv(hv.x+uHue,hv.y,hv.z);}tc*=uTint;
    if(k>1.5&&k<2.5){float sea=smoothstep(.03,.18,tc.b-max(tc.r,tc.g));spec=sea;base=mix(tc,vec3(.92,.95,.98),smoothstep(.86,.96,abs(o.z))*.55);}
    else if(k>4.5&&k<5.5){float land=smoothstep(.0,.1,tc.g-tc.b*.8);base=tc;
      if(uCityK>.5){float cl=smoothstep(.5,.74,fbm(o*3.4+vec3(t*.012,0.,0.)+uSeed*2.));base=mix(base,vec3(.93,.95,.97),cl*.75);emis=dot(n,L)<.1?uEmisCol*cityLights(o,land)*uEmis*(1.-.7*cl)*smoothstep(.1,-.2,dot(n,L)):vec3(0.);}
      else emis=uEmisCol*step(.78,h31(floor(o*58.)))*smoothstep(.5,.62,fbm(o*5.+uSeed*3.))*land*uEmis*smoothstep(.05,-.25,dot(n,L));}
    else base=tc;
    pat=dot(tc,vec3(.3,.5,.2));
  }
  if(uHK>0.){float hh=texEq(uHMap,o).r;vec3 dpx=dFdx(vP),dpy=dFdy(vP),r1=cross(dpy,n),r2=cross(n,dpx);float det=dot(dpx,r1);
    n=normalize(abs(det)*n-sign(det)*(dFdx(hh)*r1+dFdy(hh)*r2)*(uBumpFix>0.?uHK*.05*uBumpFix:uHK));}
  float nl=dot(n,L),diff=max(0.,nl*.92+.08)*(1.+.15*bump);if(uBumpFix>0.)diff*=smoothstep(-.1,.12,dot(normalize(vN),L));if(uCityK>.5)diff=sphLight(nl,min(.92,uStarR/length(uStarC-vP)));
  if(uShadW>0.){float sd=0.;for(int i=0;i<4;i++){vec4 Rs=uShad[i];if(Rs.w<=0.)continue;float den=dot(L,Rs.xyz);if(abs(den)<1e-3)continue;float tt=dot(uShadC-vP,Rs.xyz)/den;if(tt<=0.)continue;float rr=length(vP+L*tt-uShadC);sd=max(sd,1.-smoothstep(uShadW*.6,uShadW*2.2,abs(rr-Rs.w)));}diff*=1.-.55*sd;}
  if(uRingN.w>.5){float den=dot(L,uRingN.xyz);if(abs(den)>1e-3){float tt=dot(uRingC-vP,uRingN.xyz)/den;if(tt>0.){float rr=length(vP+L*tt-uRingC);
    if(rr>uRingR.x&&rr<uRingR.y){float xr=(rr-uRingR.x)/(uRingR.y-uRingR.x);diff*=1.-.72*(uRingTexK>.5?texture2D(uRingTex,vec2(xr,.5)).a:ringDen(xr));}}}}
  vec3 col=base*(uAmb+diff*uLight*1.08)+emis;
  vec3 H=normalize(L+V);col+=uLight*pow(max(0.,dot(n,H)),k>3.5&&k<4.5?90.:48.)*spec*(k>3.5&&k<4.5?1.2:.55)*step(0.,nl);
  col+=uAtmo*uAtmoK*pow(1.-mu,2.6)*smoothstep(-.25,.45,nl);if(uCityK>.5)col+=uAtmo*uAtmoK*.7*pow(1.-mu,4.)*smoothstep(0.,.9,-dot(V,L));
  vec3 neg=vec3(.008,.007,.01)+uGold*.05*(1.-pat);
  col=mix(col,neg,uNeg);
  col+=uGold*uRimG*(pow(1.-mu,7.)*1.25+smoothstep(.09,.0,mu)*.85);
  gl_FragColor=vec4(col,1.);}