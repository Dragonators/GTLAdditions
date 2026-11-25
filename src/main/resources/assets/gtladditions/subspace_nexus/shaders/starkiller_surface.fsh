uniform vec3 uCam;uniform float uPix;uniform float uTime;uniform mat3 uW2L;uniform vec3 uC;uniform float uS;uniform vec3 uStarC;uniform float uStarR;uniform vec3 uLight;uniform float uAmb;uniform float uNeg;uniform float uRimG;uniform vec3 uGold;uniform vec3 uFire;uniform vec3 uHot;uniform vec3 uAtmo;
uniform sampler2D uMap;uniform float uMapK;uniform vec3 uTint;uniform vec4 uShad[4];uniform float uShadW;
varying vec3 vP;
float hh(float n){return fract(sin(n*127.1+.3)*43758.5453);}
float h31(vec3 p){p=fract(p*vec3(.1031,.1030,.0973));p+=dot(p,p.yxz+33.33);return fract((p.x+p.y)*p.z);}
float vnoise(vec3 p){vec3 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);
  return mix(mix(mix(h31(i),h31(i+vec3(1,0,0)),f.x),mix(h31(i+vec3(0,1,0)),h31(i+vec3(1,1,0)),f.x),f.y),
             mix(mix(h31(i+vec3(0,0,1)),h31(i+vec3(1,0,1)),f.x),mix(h31(i+vec3(0,1,1)),h31(i+vec3(1,1,1)),f.x),f.y),f.z);}
float fbm3(vec3 p){float a=.5,s=0.;for(int i=0;i<3;i++){s+=a*vnoise(p);p=p*2.07+vec3(5.3,1.1,7.7);a*=.5;}return s/.875;}
const float RF1=.945,RF2=.885,RP=.2,X0=.4,CW=.05;
float wid(float lam){return mix(.085,.31,exp(-lam*lam/.46));}
float ribs(float x){return .5+.5*cos(x*75.4);}
float map(vec3 p){
  float r=length(p),q=length(p.yz),lam=atan(p.y,p.x),w=wid(lam),z=abs(p.z),d=r-1.;
  d=max(d,-max(z-w,RF1-r));d=max(d,-max(z-w*.66,RF2-r));
  if(abs(lam)>.42)d=min(d,max(abs(mod(lam+.16,.32)-.16)*r-.011,max(z-w*.68,r-RF2-.04)));
  if(p.x>0.){d=max(d,-max(q-RP+.012*ribs(p.x),X0-p.x));d=min(d,max(abs(q-RP-CW)-CW,abs(r-RF2-.03)-.03));}
  return d;}
float sphLight(float nc,float sa){float ca=sqrt(1.-sa*sa),a=ca*nc,b=sa*sqrt(max(0.,1.-nc*nc));return (a>=b?a:a<=-b?0.:(a*acos(clamp(-a/max(b,1e-5),-1.,1.))+sqrt(max(b*b-a*a,0.)))/3.14159265)/ca;}
vec3 calcN(vec3 p){const vec2 e=vec2(.0016,-.0016);return normalize(e.xyy*map(p+e.xyy)+e.yyx*map(p+e.yyx)+e.yxy*map(p+e.yxy)+e.xxx*map(p+e.xxx));}
float softSh(vec3 p,vec3 l){float t=.012,res=1.;for(int i=0;i<10;i++){float h=map(p+l*t);res=min(res,10.*h/t);t+=clamp(h,.012,.08);if(res<.02||t>.5)break;}return clamp(res,0.,1.);}
vec4 texEq(sampler2D m,vec3 o){vec2 uv=vec2(atan(o.y,o.x)/6.2831853+.5,asin(clamp(o.z,-1.,1.))/3.14159265+.5);float u2=fract(uv.x+.5);
  vec2 dx=vec2(dFdx(uv.x),dFdx(uv.y)),dy=vec2(dFdy(uv.x),dFdy(uv.y));float dx2=dFdx(u2),dy2=dFdy(u2);if(abs(dx2)<abs(dx.x))dx.x=dx2;if(abs(dy2)<abs(dy.x))dy.x=dy2;return textureGrad(m,uv,dx,dy);}
float ringSh(vec3 wp,vec3 L){float s=0.;for(int i=0;i<4;i++){vec4 R=uShad[i];if(R.w<=0.)continue;float den=dot(L,R.xyz);if(abs(den)<1e-3)continue;float tt=dot(uC-wp,R.xyz)/den;if(tt<=0.)continue;float rr=length(wp+L*tt-uC);s=max(s,1.-smoothstep(uShadW*.6,uShadW*2.2,abs(rr-R.w)));}return s;}
void main(){
  vec3 ro=uW2L*(uCam-uC)/uS,rd=normalize(uW2L*(vP-uCam));
  float bq=dot(ro,rd),cq=dot(ro,ro)-1.,hq=bq*bq-cq;if(hq<=0.)discard;
  float sq=sqrt(hq),t=max(-bq-sq,0.),tEnd=-bq+sq;vec3 p=ro+rd*t;float d=map(p);bool hit=d<.0012;
  for(int i=0;i<52&&!hit;i++){t+=d*.85;if(t>tEnd)discard;p=ro+rd*t;d=map(p);hit=d<.0012;}
  if(!hit&&d>.012)discard;
  float r=length(p),q=length(p.yz),lam=atan(p.y,p.x),z=abs(p.z),w=wid(lam);bool ice=r>.9975;
  vec3 n=ice?normalize(p):calcN(p),wp=uC+(p*uW2L)*uS,V=-rd,Lw=normalize(uStarC-wp),Ll=normalize(uW2L*Lw);
  float nl=dot(n,Ll),mu=max(0.,dot(n,V)),px=uPix*length(uCam-wp)/uS,breath=.92+.08*sin(uTime*1.3),sa=min(.92,uStarR/length(uStarC-wp)),lit=sphLight(nl,sa);
  vec3 col,E=vec3(0.);
  if(ice){
    vec3 tc=uMapK>.5?texEq(uMap,normalize(p)).rgb:vec3(.8,.86,.94);tc=mix(vec3(dot(tc,vec3(.3,.5,.2))),tc,.3)*uTint;
    float near=1.-smoothstep(w,w+.07,z);tc*=1.-.62*near;
    float lip=1.-smoothstep(.012,.012+px,abs(z-w-.012));tc=mix(tc,vec3(.17,.18,.2),lip);
    float diff=lit*(1.-.55*ringSh(wp,Lw));
    col=tc*(uAmb+vec3(.012,.02,.034)+diff*uLight*1.02)+uLight*pow(max(0.,dot(n,normalize(Ll+V))),40.)*.1*step(0.,nl);
    col+=uAtmo*.5*pow(1.-mu,2.6)*smoothstep(-.6,.3,nl)+uAtmo*.55*pow(1.-mu,4.)*smoothstep(0.,.9,-dot(V,Ll));
    E+=uFire*.05*exp(-q*q/.09)*(1.-smoothstep(-.1,.3,nl))*breath;
  }else if(q<RP+.0025&&p.x<.93){
    float k=clamp((.9-p.x)/(.9-X0),0.,1.),hot=exp(-q*q/.0055);
    if(p.x<X0+.003){float n1=fbm3(vec3(p.yz*16.,uTime*.5));E=mix(uFire,uHot,hot)*(1.15+1.7*hot)*(.8+.4*n1)*(.74+.26*cos(q*105.));}
    else E=uFire*(.22+1.35*k*k)*(.3+.95*smoothstep(.3,.95,ribs(p.x)))+uHot*.25*k*k*k;
    E*=breath;col=vec3(0.);
  }else{
    float sh=lit>.002?softSh(p+n*.004,Ll):0.,diff=lit*sh*(1.-.55*ringSh(wp,Lw));
    vec2 G=vec2(lam/.055,p.z/.04),c0=floor(G),f0=fract(G);float hs=h31(vec3(c0,3.)),seam=min(min(f0.x,1.-f0.x)*.055,min(f0.y,1.-f0.y)*.04),lod=1.-smoothstep(.006,.022,px);
    vec3 alb=vec3(.1,.105,.115)*(.72+.56*hs*lod+.28*(1.-lod));alb*=1.-.5*lod*(1.-smoothstep(.001,.003+px,seam));
    bool wall=abs(dot(n,normalize(p)))<.6,collar=q<RP+2.*CW+.004&&p.x>0.&&r>RF2+.004&&r<RF2+.064;
    if(wall)alb*=.62;
    if(!wall&&!collar&&r>RF1-.006){
      float zc=w*.83,st=abs(fract((lam+z*1.3)/.13)-.5)*.13,lk=1.-smoothstep(.75,1.15,abs(lam));alb=mix(alb,vec3(.26,.27,.29),lk*(1.-smoothstep(.0035,.007+px,st))*lod);
      float dx=.03,ax=lam/dx,on=step(.38,h31(vec3(floor(ax),sign(p.z),7.))),ww=1.-smoothstep(.0055,.0055+px,abs(z-zc)),wx=1.-smoothstep(.0075,.0075+px,abs(fract(ax)-.5)*dx),far=exp(-lam*lam/1.8);
      E+=vec3(1.,.8,.55)*(on*ww*wx*lod*1.35+.22*ww*(1.-lod))*(.3+.7*far);}
    if(collar){alb=vec3(.2,.21,.23);float a=atan(p.z,p.y),la=abs(fract(a/6.2831853*10.)-.5)/10.*6.2831853*(RP+.07),dl=length(vec2(la,q-RP-.072));E+=uFire*(1.-smoothstep(.007,.012+px,dl))*1.8;}
    E+=uFire*(exp(-max(0.,q-RP)/.026)*.42+exp(-max(0.,q-RP)/.15)*.075)*breath*step(0.,p.x);
    E+=alb*vec3(.5,.43,.37)*(.1+.42*exp(-lam*lam/1.2));
    float H=pow(max(0.,dot(n,normalize(Ll+V))),24.)*.3*diff;
    col=alb*(uAmb*1.2+vec3(.008,.01,.016)+diff*uLight*1.25)+uLight*H;
  }
  col=mix(col,vec3(.008,.007,.01)+uGold*.05,uNeg)+uGold*uRimG*(ice?pow(1.-mu,7.)*1.25+smoothstep(.09,0.,mu)*.85:0.);
  E*=1.15;col+=E/(1.+.3*max(E.r,max(E.g,E.b)))*(1.-.55*uNeg);
  vec4 cp=projectionMatrix*viewMatrix*vec4(wp,1.);gl_FragDepth=.5*cp.z/cp.w+.5;
  gl_FragColor=vec4(col,1.);}