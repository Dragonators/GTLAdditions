uniform float uModules[144];
uniform float uScreenR;
// 0..9 glitch timing and frame ids, 10..12 angular phases, 13..17 noise lattice offsets.
uniform float uGlitchClock[18];
precision highp float;
uniform vec3 uCam;uniform float uTime;uniform float uGlow;uniform float uW;uniform float uH;uniform float uAlpha;uniform float uBack;
uniform vec3 uCol;uniform vec3 uHot;uniform vec3 uGold;uniform float uScan;uniform float uShow;uniform float uCount;uniform float uTear;uniform float uInv;uniform float uSeed;uniform float uStarA;uniform float uBay;
uniform float uTearK;uniform float uPage;uniform vec2 uHi;uniform float uShips[6];uniform float uShipU[6];uniform float uMk[9];uniform vec4 uPl[4];uniform vec4 uPlB[4];uniform vec3 uPk;uniform float uRot;uniform float uBelt;uniform float uOrbA;uniform vec3 uNum;
uniform float uScar;uniform vec4 uMC;uniform float uStarR;uniform vec3 uStarW;uniform vec3 uAxisW;uniform float uDwell;uniform float uInterval;uniform float uT;uniform float uSide;uniform float uProf[40];uniform float uGlitch;uniform float uGar;uniform sampler2D uGarble;
uniform sampler2D uText;uniform sampler2D uGlyph;uniform sampler2D uMap0;uniform sampler2D uMap1;uniform sampler2D uMap2;uniform sampler2D uMap3;
varying vec2 vUv;varying vec3 vP;
const float PW=47.124,PH=36.,PI=3.14159265,TAU=6.2831853,GN=25.;
float AA;vec2 PDX,PDY;
float h1(float n){return fract(sin(n*127.1)*43758.5453);}
float h2(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
float h3(vec3 p){return fract(sin(dot(p,vec3(12.9898,78.233,37.719)))*43758.5453);}
float stars(vec3 d,float k){vec3 p=d*k,i=floor(p),f=fract(p)-.5;float s=step(.972,h3(i));vec3 o=vec3(h3(i+1.),h3(i+2.),h3(i+3.))-.5;return s*(1.0 - smoothstep(0.,.16,length(f-o*.6)))*(.4+.6*h3(i+4.));}
float periodicNoise(float x,float period){
  float i=floor(x),f=fract(x);f=f*f*(3.-2.*f);
  float mid=floor(period*.5);
  return mix(h1(mod(i+mid,period)-mid),h1(mod(i+1.+mid,period)-mid),f);
}
float vn(float x){return periodicNoise(x,1024.);}
float latticeHash(vec2 cell,vec2 period){
  vec2 mid=floor(period*.5);
  return h2(mod(cell+mid,period)-mid);
}
float periodicNoise2(vec2 x,vec2 period){
  vec2 i=floor(x),f=fract(x);f=f*f*(3.-2.*f);
  return mix(mix(latticeHash(i,period),latticeHash(i+vec2(1,0),period),f.x),
             mix(latticeHash(i+vec2(0,1),period),latticeHash(i+vec2(1),period),f.x),f.y);
}
float vn2(vec2 x){return periodicNoise2(x,vec2(1024));}
float planetNoise(float lon,float lat,float scale,float seed){
  float cells=floor(TAU*1.6*scale+.5);
  return periodicNoise2(vec2(lon*cells/TAU,lat*4.*scale)+seed,vec2(cells,1024));
}
vec3 subspace(vec3 dir){vec3 w=vec3(.008,.01,.024)+vec3(.09,.06,.02)*smoothstep(.2,1.,sin(dir.x*6.+uGlitchClock[12])*sin(dir.z*4.)+.3);
  return w+vec3(.95,.9,.8)*(stars(dir,95.)+stars(dir,190.)*.7+stars(dir,360.)*.5);}
float inRect(vec2 p,vec2 a,vec2 b){vec2 s=step(a,p)*step(p,b);return s.x*s.y;}
// d and half-width w are in page units; subpixel lines keep one-pixel coverage and fade.
float ln(float d,float w){float e=max(w,.5*AA);return clamp((e+.5*AA-d)/AA,0.,1.)*min(1.,w/(.5*AA));}
float rect(vec2 p,vec2 a,vec2 b){vec2 e=max(a-p,p-b);return clamp(.5-max(e.x,e.y)/AA,0.,1.);}
float tile(vec2 q,float i,float n,float h){vec2 u=vec2((i+clamp(q.x,0.,1.)*n)/GN,clamp(q.y,0.,1.));vec2 k=vec2(1./(h*.6*GN),1./h);return textureGrad(uGlyph,u,PDX*k,PDY*k).r;}
float glyph(vec2 p,vec2 o,float h,float i,float n){vec2 q=(p-o)/vec2(h*.6*n,h);if(q.x<0.||q.y<0.||q.x>1.||q.y>1.)return 0.;return tile(q,i,n,h);}
// live number: nI integer digits, nF decimals
float num(vec2 p,vec2 o,float h,float v,float nI,float nF){
  float w=h*.6,x=(p.x-o.x)/w,y=(p.y-o.y)/h;if(y<0.||y>1.||x<0.)return 0.;
  float dot_=nF>0.?.5:0.;if(x>nI+nF+dot_)return 0.;
  if(nF>0.&&x>=nI&&x<nI+dot_){float q=(x-nI)/dot_+0.;return textureGrad(uGlyph,vec2((10.25+q*.5)/GN,y),PDX*vec2(1./(w*GN),1./h),PDY*vec2(1./(w*GN),1./h)).r;}
  float ix=x>=nI+dot_?x-dot_:x,idx=floor(ix),S=floor(v*pow(10.,nF)+.0001),N=nI+nF,d=mod(floor(S/pow(10.,N-1.-idx)),10.);
  return tile(vec2(fract(ix),y),d,1.,h);}
vec3 ptint(int k){return k==0?vec3(.84,.9,.96):k==1?vec3(.55,.82,1.):k==2?vec3(.74,.66,1.):vec3(.45,.96,1.);}
float seg(vec2 p,vec2 a,vec2 b){vec2 pa=p-a,ba=b-a;return length(pa-ba*clamp(dot(pa,ba)/dot(ba,ba),0.,1.));}
float gly(vec2 P,float id){float st=1.;
  for(int i=0;i<3;i++)for(int j=0;j<4;j++){vec2 n=vec2(.12+.24*float(i),.13+.25*float(j));
    if(i<2&&h1(id*13.1+float(i*7+j)*1.7)<.36)st=min(st,seg(P,n,n+vec2(.24,0.)));
    if(j<3&&h1(id*7.3+float(i*5+j)*2.9)<.36)st=min(st,seg(P,n,n+vec2(0.,.25)));
    if(i<2&&j<3&&h1(id*5.9+float(i*3+j)*4.1)<.16)st=min(st,seg(P,n,n+vec2(.24,.25)));}
  return st;}
// n glyphs; o is lower left, h is height.
float gstr(vec2 p,vec2 o,float h,float n,float seed){vec2 q=(p-o)/h;if(q.y<0.||q.y>1.||q.x<0.||q.x>n*.72)return 0.;float c=floor(q.x/.72);return ln(gly(vec2(q.x-c*.72,q.y),seed+c*3.7)*h,.045*h);}
vec3 pmap(int k,vec2 uv,vec2 dx,vec2 dy){return k==0?textureGrad(uMap0,uv,dx,dy).rgb:k==1?textureGrad(uMap1,uv,dx,dy).rgb:k==2?textureGrad(uMap2,uv,dx,dy).rgb:textureGrad(uMap3,uv,dx,dy).rgb;}
vec2 sphUV(vec2 n,float c,float s,float spin){float l=dot(n,n);if(l>.995)n*=sqrt(.995/l);float z=sqrt(max(0.,1.-dot(n,n)));vec2 t=vec2(c*n.x+s*n.y,-s*n.x+c*n.y);return vec2((atan(t.x,z)+spin)/TAU,.5+asin(clamp(t.y,-1.,1.))/PI);}
// q is in 0..1 inside an atlas cell of size cs.
float gtile(vec2 q,float ch,vec2 cs){vec2 t=vec2(mod(ch,16.),floor(ch/16.)),m=clamp(q,.03,.97),k=vec2(1./(cs.x*16.),1./(cs.y*8.));return textureGrad(uGarble,vec2((t.x+m.x)/16.,1.-(t.y+1.-m.y)/8.),PDX*k,PDY*k).r;}
// dens: active row density; cls: row kind.
float garbage(vec2 p,float dens,out float cls){
  cls=.5;vec2 cs=vec2(.62,.94);float right=p.x<23.562?0.:1.,d=right<.5?p.x-.9:46.22-p.x;if(d<0.||p.y<.2||p.y>35.9)return 0.;
  float row=floor(p.y/cs.y),hr=h1(row*7.31+right*3.1+uSeed*2.),life=floor(uTime/(1.4+3.6*hr)+hr*11.),rl=h1(row*3.77+life*1.31+right+uSeed);
  if(rl>dens)return 0.;
  float rl2=fract(rl*37.7),L=2.+floor(13.*rl2*fract(rl2*3.3+.4)),sh=step(.74,fract(hr*5.3))*floor(uTime*(2.+5.*fract(hr*9.1)));
  if(d/cs.x>=L)return 0.;
  float gx=p.x/cs.x,gi=floor(gx),ch=floor(fract(h2(vec2(gi+sh,row))+floor(uTime*(.4+4.*h1(gi*1.7+row*2.3)))*.6180339)*128.);
  cls=fract(rl2*13.7);vec2 q=vec2(fract(gx),fract(p.y/cs.y));
  return uGar<1.5?gtile(q,ch,cs):ln(gly(vec2(q.x*.72,q.y),ch*1.7)*cs.y,.05*cs.y);}
vec3 skyChart(vec2 pp,vec3 cc,vec3 hot){
  vec3 add=vec3(0.);float S=uMC.z;vec2 m=(pp-uMC.xy)/vec2(S,S*uMC.w);float r=length(m),a=atan(m.y,m.x),mkb=smoothstep(16.6,18.2,r);
  if(mkb>0.){
    float fa=abs(fract(a*12./TAU+.5)-.5)/12.*TAU*r*S,rr=abs(fract(r/2.4+.5)-.5)*2.4*S;add+=cc*(ln(fa,.014)*.13+ln(rr,.012)*.09)*mkb;
    vec2 gc=pp/5.2,gi=floor(gc);
    for(int j=-1;j<=1;j++)for(int i=-1;i<=1;i++){vec2 c0=gi+vec2(float(i),float(j));float hs=h2(c0+7.);if(hs<.16)continue;vec2 pa=(c0+.2+.6*vec2(h2(c0+1.3),h2(c0+2.7)))*5.2;float dp=length(pp-pa),mg=.05+.1*h1(hs*9.),br=.4+.6*h1(hs*5.);
      add+=hot*clamp((mg-dp)/AA+.5,0.,1.)*br*mkb;add+=cc*exp(-dp*dp/.2)*.13*br*mkb;
      for(int e=0;e<2;e++){vec2 c1=c0+(e==0?vec2(1.,0.):vec2(0.,1.));if(h2(c1+7.)<.16||h2(c0+c1*3.1)<.5)continue;vec2 pb=(c1+.2+.6*vec2(h2(c1+1.3),h2(c1+2.7)))*5.2;add+=cc*ln(seg(pp,pa,pb),.014)*.36*mkb;}}
  }
  {vec2 ed=min(pp-vec2(.0,-.5),vec2(47.124,36.5)-pp);float em=min(ed.x,ed.y);if(em<1.25&&em>.55){float along=ed.x<ed.y?pp.y:pp.x,tk=abs(fract(along/1.2+.5)-.5)*1.2,mj=abs(fract(along/6.+.5)-.5)*6.;add+=cc*(ln(tk,.014)*step(em,.86)+ln(mj,.02))*.42;}}
  return add;}
float gS0,gK=0.,gA=0.,gB=0.,gC=0.;   // Page scale; shared state written by the shift and read by the overlay.
float fbm2(vec2 q){return vn2(q)*.55+vn2(q*2.13+7.)*.28+vn2(q*4.7+3.)*.17;}
vec2 shift4(vec2 uv,float G,float b){
  float T=uTime+uSeed*9.,y=uv.y,x=.0028*(sin(y*13.+T*1.1)+.6*sin(y*31.-T*2.3)),band=0.;
  for(int k=0;k<2;k++){float fk=float(k),yb=fract(T*(.043+.031*fk)+.37*fk),hb=(.02+.022*fk)*(1.+1.6*b),dt=abs(fract(y-yb+.5)-.5),e=exp(-dt*dt/(hb*hb));band=max(band,e);
    x+=(h1(floor(y*150.)+floor(uTime*(11.+5.*fk))*1.7+fk*9.)-.5)*.1*e;}
  float fl=exp(-y/.03);x+=fl*(.05+.035*sin(y*150.+T*3.)+.04*(h1(floor(y*260.)+floor(uTime*9.))-.5));
  gK=band;return vec2(x*G,0.);}
void over4(inout vec3 add,vec2 pp,vec2 uv,float G,float b,float live,float sd,vec3 cc,vec3 hot){
  vec3 white=vec3(.85,.82,.72);float band=gK,rw=floor(pp.y/.1),T=floor(uTime*15.),hr=h1(rw*1.91+sd*3.),len=.7+7.*hr*hr,gx=pp.x/len+hr*53.+T*.37*(hr-.5)*8.,xc=floor(gx),xf=1.-fract(gx),n=h1(xc*1.7+rw*3.3+T*.71),gg=min(1.,G);
  if(uSide>7.5&&uSide<8.5){float dc=.16+.3*band+.3*b;add+=(skyChart(pp+vec2(dc,0.),cc,hot)*vec3(.55,.08,.02)+skyChart(pp-vec2(dc,0.),cc,hot)*vec3(.02,.2,.3))*live;}
  add=mix(add,vec3(dot(add,vec3(.33))),.5*band);
  add+=white*(.035*band+step(1.-.6*band,n)*band*(.2+.8*n)*xf*xf)*live*gg;
  float hsw=step(uv.y,.03+.014*vn(uTime*2.+uSeed))*step(.008,uv.y);add+=white*hsw*step(.45,n)*(.2+.7*n)*xf*live*gg;
  float rc=floor(pp.y/.24),hc=h1(rc*2.3+sd*5.+uSeed),lc=.2+.5*hc,nc=floor(uTime/lc+hc*9.),ac=h1(nc*1.3+rc*.7+uSeed);
  if(ac>.972-.022*b){float x0=1.+44.*h1(ac*91.),L=3.+11.*h1(ac*37.),xt=pp.x-x0;if(xt>=0.&&xt<L){float k=exp(-xt/(L*.3));add+=white*(.25+1.1*k)*(1.-xt/L)*live*gg;}}}
void events5(vec2 uv,float sd,float G){
  for(int i=0;i<5;i++){float fi=float(i),per=1.5+2.3*h1(fi*3.7+uSeed),ph=uTime/per+h1(fi*9.1+uSeed*3.),n=floor(ph),f=fract(ph)*per,hs=h1(n*1.37+fi*5.1+uSeed*7.);
    if(hs>.3+.5*min(1.,G))continue;float hn=hs/.8,dur=.45+1.3*h1(hn*31.);if(f>dur)continue;
    if(abs((h1(hn*17.)<.5?-1.:1.)-sd)>.5)continue;
    float y0=.12+.86*h1(hn*43.),hh=(.07+.5*pow(h1(hn*59.),1.6))*min(1.,f/.09+.35),yy=(y0-uv.y)/hh;if(yy<0.||yy>1.)continue;
    gK=1.;gA=yy;gB=hn;gC=(h1(hn*71.)-.5)*1.6;}}
void over5(inout vec3 add,vec2 pp,vec2 uv,float live){
  if(gK<.5)return;
  float hn=gB,yw=uv.y*uH/gS0,sl=floor(gA*(3.+5.*fract(hn*3.3))),xb=pp.x+yw*gC+h1(sl+hn*5.)*9.,c0=floor(xb/.33),b0=h1(c0+hn*91.),b1=h1(floor(xb/1.4)+hn*37.),v=step(.35,b0)*(.25+.75*b0*b0)*(.4+.6*b1)+step(.93,h1(floor(xb/.09)+hn*13.))*.55;
  float yEnd=.6+.4*h1(floor(pp.x/.7)+hn*9.),m=step(gA,yEnd),fade=exp(-gA*1.6)*(.7+.3*h1(floor(uTime*20.)+c0));
  float pick=fract(hn*7.7);vec3 tint=pick<.5?vec3(.74,.74,.66):pick<.8?vec3(.22,.62,.56):vec3(.66,.24,.56);
  add=add*(1.-.82*m)+tint*v*fade*m*.68*live;
  add+=tint*1.3*step(gA,.012+.004/max(.05,gS0))*live;}
void over6(inout vec3 add,vec2 uv,float ed,float G,float b,float live,float sd,vec3 gold){
  if(ed<0.)return;
  float rh=.17*pow(2.,max(0.,ceil(log2(max(AA,.001)*1.1/.17)))),yw=uv.y*uH/gS0,rid=floor(yw/rh),hr=h1(rid*1.37+sd*3.1+uSeed*5.);if(hr<.12)return;
  float c1=vn(yw*.42+uSeed*3.+sd*5.+uTime*.03),c2=vn(yw*1.7+uSeed+sd+uTime*.05);
  float L=(1.5+15.*pow(c1,2.6)*(.45+.55*c2))*(.3+.7*hr)*G*(1.+.6*b);L*=.75+.5*h1(rid*3.3+floor(uTime*(.25+1.2*hr+6.*b)));
  if(ed>L)return;
  float tt=(ed/L)*(.55+.45*fract(hr*7.3));
  vec3 c=tt<.1?mix(vec3(1.,.95,.82),gold*1.25,tt/.1):tt<.5?mix(gold*1.25,vec3(.62,.2,.07),(tt-.1)/.4):mix(vec3(.62,.2,.07),vec3(.14,.03,.09),(tt-.5)/.5);
  c*=.9+.2*h1(floor(ed/.4)+rid);
  add=mix(add,c*(.55+.45*hr)*live,.92);}
float gD=0.,gE=0.;
vec2 shift7(vec2 uv,vec2 RS,float S0,float sd,float G){
  vec2 pp=(uv*RS-.5*RS)/S0+.5*vec2(47.124,36.);float best=1e9,oid=0.,xr=23.562+sd*(RS.x*.345/S0);vec2 o0=vec2(0.);
  for(int i=0;i<3;i++){float fi=float(i);vec2 o=vec2(xr+sd*(h1(fi+uSeed)-.5)*2.,4.+28.*(fi+h1(fi*3.1+uSeed*2.+sd*5.))/3.);float dd=distance(pp,o);if(dd<best){best=dd;o0=o;oid=fi;}}
  vec2 dv=pp-o0;float rho=length(dv),ph=atan(dv.y,dv.x+1e-5),id=oid*7.+uSeed*3.+sd*1.3,k=15.+8.*h1(id),jt=(vn(rho*.17+id*7.)-.5)*.42+(vn(rho*.8+id)-.5)*.05,fa=(ph+jt)*k/TAU,ci=floor(fa),fu=fract(fa);
  float hw=h1(ci*3.7+id),rk=3.+10.*hw,rq=rho/rk+h1(ci*1.3+id*2.),ring=floor(rq),rf=fract(rq);
  gA=ci+id*13.+ring*57.;gB=min(fu,1.-fu)/k*TAU*rho;gC=rho;gD=min(rf,1.-rf)*rk;gE=fa*.55+id;
  float reach=step(rho,5.+19.*min(1.,G));gK=reach;
  return reach*(vec2(h1(gA*1.71),h1(gA*2.93))-.5)*vec2(.62/RS.x,.62/RS.y)*S0*min(1.,G);}
void over7(inout vec3 add,inout float inkK,vec2 pp,vec2 uv,float G,float b,float live,float sd){
  float gg=min(1.,G),hs=h1(gA*1.37),rho=gC,reach=gK;
  add+=vec3(.19,.14,.085)*exp(-rho*.11)*reach*gg*(.55+.45*fbm2(pp*.12+uSeed))*(.8+.4*fract(hs*17.))*live;
  float ink=smoothstep(.57,.575,fbm2(vec2(gE,rho*.075+uSeed+sd))+.05*exp(-rho*.09))*reach*gg;
  if(hs<.08){float g=exp(-rho*.14);add=add*.45+vec3(.74,.7,.6)*(.1+.55*g)*(.65+.35*fbm2(pp*.45))*reach*(.9+.1*step(.2,h1(floor(uTime*1.3)+hs*91.)))*live;}
  float crk=ln(min(gB,gD*step(.32,fract(hs*29.))+1e3*step(fract(hs*29.),.32)),.012+.035*exp(-rho*.35))*(.3+.7*exp(-rho*.07))*reach*gg;
  add+=vec3(.86,.83,.74)*crk*.85*live;
  float xn=pp.x/.16,cc=floor(xn),hc=h1(cc*1.73+uSeed*3.),xb=23.562+sd*(uW*.345/gS0+3.+6.*h1(uSeed*9.+sd)),xq=(pp.x-xb)/1.2,dn=(exp(-xq*xq)*.72+.016)*gg;
  if(hc<dn){float fr=fract(hc*91.),y0=h1(hc*37.)*.3,y1=1.-h1(hc*53.)*.3;vec3 col=fr<.5?vec3(.9,.9,.85):fr<.67?vec3(1.,.15,.1):fr<.84?vec3(.1,1.,.3):vec3(.2,.4,1.);add+=col*ln(abs(fract(xn)-.5)*.16,.02)*step(y0,uv.y)*step(uv.y,y1)*(.35+.65*fract(hc*13.))*(.85+.15*step(.3,h1(floor(uTime*.9)+hc*17.)))*live;}
  add*=1.-ink;inkK=ink;}
// Large sine arguments collapse the hash on some GPUs. These ids are decorative noise, not world time.
float glitchHash(float n){return h1(mod(n,128.));}
vec2 shift8(vec2 uv,float G){
  float bi=uGlitchClock[1],bf=uGlitchClock[0],hb=glitchHash(bi*1.7+uSeed),s=.2+2.2*fract(hb*7.),dur=.14+.5*fract(hb*13.)*fract(hb*29.),inB=step(hb,.2+.6*G)*step(s,bf)*step(bf,s+dur),tick=uGlitchClock[2],x=0.;
  gK=inB;gA=0.;
  if(inB>.5){for(int i=0;i<7;i++){float fi=float(i),hy=glitchHash(fi*3.1+tick*.73+uSeed),hh=glitchHash(fi*7.7+tick*1.31),th=.005+.16*hh*hh*hh;if(abs(uv.y-hy)<th){x=(glitchHash(fi*11.3+tick*.37)-.5)*.62;gA=1.+mod(fi,3.);}}}
  else{for(int i=0;i<2;i++){float fi=float(i),tl=uGlitchClock[3+i],hy=glitchHash(fi*13.+tl*1.9+uSeed*2.),th=.0025+.006*glitchHash(tl*3.+fi);if(abs(uv.y-hy)<th){x=(glitchHash(tl*7.1+fi)-.5)*.1;gA=1.;}}}
  return vec2(x*G,0.);}
void over8(inout vec3 add,vec2 pp,float live,vec3 cc,vec3 hot){
  if(gA>1.5)add*=gA<2.5?vec3(1.25,.45,.3):gA<3.5?vec3(.35,.85,1.2):vec3(1.);
  if(gK>.5&&uSide>7.5&&uSide<8.5){float dc=.42;add+=(skyChart(pp+vec2(dc,0.),cc,hot)*vec3(.9,.1,.05)+skyChart(pp-vec2(dc,0.),cc,hot)*vec3(.05,.45,.6))*live;}}
float gauge(vec2 p,vec2 c,float R,float f){vec2 d=p-c;float l=length(d);if(l>R+.3)return 0.;float fa=fract(.25-atan(d.y,d.x)/TAU);return ln(abs(l-R),.018)*.4+ln(abs(l-R*.76),.08)*step(fa,f)*.9;}
vec3 windowStarCenter(){
  // One centre per panel side, independent of the fragment on its curved surface.
  float back=step(uScreenR,dot(uCam,uAxisW));
  return uStarW-uAxisW*(2.*back*(dot(uStarW,uAxisW)-uScreenR));
}
vec4 windowStarRay(vec3 rd){
  vec3 oc=windowStarCenter()-uCam;float tc=dot(oc,rd);
  return vec4(rd*tc-oc,tc);
}
void main(){
  vec2 uv=vUv;if(!gl_FrontFacing)uv.x=1.-uv.x;
  float W=uW,H=uH,S0=min(W/PW,H/PH);vec2 RS=vec2(W,H);
  vec2 fw=fwidth(uv);AA=max(fw.x*W,fw.y*H)/S0;PDX=dFdx(uv)*RS/S0;PDY=dFdy(uv)*RS/S0;gS0=S0;
  float on=step(.012,uTear),ins=0.,edgeT=0.,burn=0.,sd=uv.x<.5?-1.:1.,gl=4.*uTear*(1.-uTear),tw=0.,dx=1.;
  float G=uGlitch*uTear*step(1.5,uTearK),burst=smoothstep(.6,.7,vn(uGlitchClock[13]+uSeed*5.))*G;   // G: failure strength; burst: failure peaks on this screen's clock.
  vec2 sh=vec2(0.);   // sh moves the page and torn edge; the window contents stay fixed.
  if(G>.003&&uGar>3.5){
    if(uGar<4.5)sh=shift4(uv,G,burst);
    else if(uGar<5.5){events5(uv,sd,G);if(gK>.5)sh.x=(gA-.5)*gC*.08;}
    else if(uGar>6.5&&uGar<7.5)sh=shift7(uv,RS,S0,sd,G);
    else if(uGar>7.5)sh=shift8(uv,G);}
  float ux=uGar>7.5&&sh.x!=0.?fract(uv.x+sh.x):uv.x+sh.x;
  if(uTearK<1.5){
    float ax=abs(uv.x-.5);tw=uTear*(.35+.025*sin(uTime*.7+uv.y*5.))+((vn(uv.y*9.+uSeed*13.)-.5)*.11+(vn(uv.y*31.+uSeed)-.5)*.035)*uTear;
    ins=step(ax,tw)*on;edgeT=(1.0 - smoothstep(0.,.026,abs(ax-tw)))*on;dx=ax;
  }else{
    float cx=.5+.016*sin(uv.y*2.6+uSeed*5.)*uTear,yc=.5+.1*sin(uSeed*3.);sd=ux<cx?-1.:1.;
    float yy=abs(uv.y-yc)/(uv.y>yc?1.-yc:yc),open=clamp(uTear*1.55-yy*(.5+.1*sd),0.,1.);open=open*open*(3.-2.*open);
    tw=open*(.345+.011*sin(uGlitchClock[10]+uv.y*4.7+sd*2.))+sqrt(open)*((vn(uv.y*6.+uSeed*13.+sd*7.)-.5)*.135+(vn(uv.y*17.+sd*3.+uSeed)-.5)*.055+(vn(uv.y*47.+sd+uSeed)-.5)*.02);
    dx=(ux-cx)*sd;ins=step(dx,tw)*on;
    float e=max(0.,(dx-tw)*W);edgeT=((1.0 - smoothstep(0.,.16,e))*1.2+exp(-e/.5)*.3)*on*(1.-ins);burn=exp(-e/1.5)*on*(1.-ins);
  }
  vec2 gu=uv;
  if(uTearK<1.5)gu.x+=(h1(floor(uv.y*26.)+floor(uTime*7.))-.5)*.025*uTear;
  else{float bands=floor(uv.y*22.+glitchHash(uGlitchClock[5])*3.);gu.x+=(glitchHash(bands+uGlitchClock[6])-.5)*.045*gl*step(.45,glitchHash(bands*3.1+uGlitchClock[7]));gu.x-=sd*.012*uTear;
    if(uGar<3.5){float rowsG=floor(uv.y*38.),bandG=floor(uv.y*7.+uSeed);gu.x+=(h1(rowsG+floor(uTime*9.))-.5)*(.005+.042*burst)*G*step(.6-.35*burst,h1(rowsG*3.1+floor(uTime*4.+uSeed)));gu.x+=(h1(bandG*5.7+floor(uTime*3.))-.5)*.05*burst*step(.55,h1(bandG+floor(uTime*2.)));}
    else if(uGar>7.5){if(sh.x!=0.)gu.x=fract(gu.x+sh.x);}
    else gu+=sh;}
  vec2 p=gu*RS,pp=(p-.5*RS)/S0+.5*vec2(PW,PH);
  vec3 cc=mix(uCol,uGold,uInv),hot=mix(uHot,vec3(1.,.93,.78),uInv);
  float flick=(1.-gl*.45*step(.6,glitchHash(uGlitchClock[8]+uSeed))*step(1.5,uTearK))*(uGar<3.5?1.-.38*burst*step(.55,h1(floor(uTime*19.)+uSeed*3.)):uGar>6.5&&uGar<7.5?1.:1.-.22*burst*step(.6,glitchHash(uGlitchClock[9]+uSeed*3.)));
  vec3 add=vec3(0.);float live=(1.-ins)*mix(1.,.6,uTear)*(1.-.75*burn)*flick,page=(1.-uShow)*live,over=0.;
  float frK=uGar>7.5&&gA>.5?1.-ins:1.;
  float d=min(min(p.x,W-p.x),min(p.y,H-p.y));add+=cc*(1.0 - smoothstep(0.,.16,abs(d-.45)))*.9*frK;
  vec2 q=min(p,vec2(W,H)-p);if(q.x<3.2&&q.y<3.2)add+=cc*(1.0 - smoothstep(0.,.32,abs(min(q.x,q.y)-.45)))*1.2*frK;
  add+=cc*.05*(.5+.5*sin(p.y*12.566))*live*(1.-smoothstep(.1,.28,fw.y*H));
  // uText channels: R normal, G paradox, B unlock page.
  if(uSide<.5){float hb=pp.y>33.2?(H-3.-.5*H)/S0+.5*PH-33.15:0.;vec3 T=texture2D(uText,(pp-vec2(0.,hb))/vec2(PW,PH)).rgb;add+=cc*(T.r*(1.-uInv)*(1.-uShow)+T.g*uInv*(1.-uShow)+T.b*uShow*(.75+.25*step(.5,fract(uTime*2.))))*live;
    add+=cc*(glyph(pp-vec2(0.,hb),vec2(9.85,33.68),1.28,15.,1.)+num(pp-vec2(0.,hb),vec2(10.62,33.68),1.28,uBay+1.,1.,0.))*live*.95;}
  // uMC: xy page centre, z chart scale, w foreshortening.
  vec2 MC=uMC.xy;float S=uMC.z,K=uMC.w,plan=step(.99,K);
  vec2 m=(pp-MC)/vec2(S,S*K);float r=length(m),a=atan(m.y,m.x),gr=length(vec2(m.x,m.y/K))/max(r,1e-3),tp=S/gr,az=a+uRot;   // tp: chart radius difference to page distance
  float sr=length(pp-MC)/S;
  if(live>.001&&r<16.4){
    float dep=mix(1.,1.+.28*(-m.y/max(r,.01)),1.-plan);
    if(r>2.3&&r<11.){float fa=abs(fract(a*12./TAU+.5)-.5)/12.*TAU*r*S;add+=cc*ln(fa,.022)*.16*live*smoothstep(2.3,3.2,r)*(1.0 - smoothstep(10.,11.,r))*uOrbA;}
    for(int k=0;k<4;k++){float R=uPlB[k].y,g=fract((uPlB[k].x-a)/TAU),tail=exp(-g*3.6),I=.2+.8*tail;
      add+=cc*ln(abs(r-R)*tp,.042)*I*.85*uOrbA*dep*live;
      add+=cc*ln(abs(r-R)*tp,.2)*tail*.1*uOrbA*dep*live;}
    {float bd=(r-10.35)/.44;if(abs(bd)<2.2){float ab=fract((a-uBelt)/TAU)*TAU,dust=exp(-bd*bd)*(.06+.11*periodicNoise(ab*(107./TAU)+7.,107.));add+=cc*dust*live*uOrbA*(1.-smoothstep(.0,.5,uInv));
       vec2 cq=vec2(ab*46./TAU,(r-9.65)/.36),ci=floor(cq),cf=fract(cq)-.5;float hh=h2(vec2(mod(ci.x,46.),ci.y)+3.);if(hh>.62){vec2 o=(vec2(h1(hh*91.),h1(hh*37.))-.5)*.55;float dd=length((cf-o)*vec2(TAU/46.*r,.36))*tp;add+=cc*ln(dd,.03+.05*fract(hh*13.))*(.35+.5*fract(hh*7.))*live*uOrbA*(1.-smoothstep(.0,.5,uInv));}}}
    float rd=11.35,i6=floor(az/(PI/3.)+.5),ai=i6*PI/3.,ri=mod(i6,6.),j3=floor((az-PI*.5)/(TAU/3.)+.5),aj=j3*TAU/3.+PI*.5,ej=mod(j3,3.);
    float ca=cos(az-ai),sa=sin(az-ai),cb=cos(az-aj),sb=sin(az-aj);
    add+=cc*(ln(abs(r-rd-.1)*tp,.034)+ln(abs(r-rd+.1)*tp,.034))*.72*(1.-uShow*.6)*live;
    {float mk=uMk[int(ri)],dm=(abs(r*ca-rd)/.36+abs(r*sa)/.26-1.)*.26*tp;add+=mix(cc,hot,clamp(mk-.4,0.,1.))*clamp(.5-dm/AA,0.,1.)*(.3+.7*min(1.,mk))*live;}
    {float mk=uMk[6+int(ej)],al=r*cb-rd,ac=abs(r*sb),dm=max(ac*1.1+al*.7-.27,-al-.27)*tp;add+=mix(cc,hot,clamp(mk-.4,0.,1.))*clamp(.5-dm/AA,0.,1.)*(.3+.7*min(1.,mk))*live;}
    if(plan>.5){
      // 3 x 48 modules: 24 columns x 2 rows per bay, with a central trench gap.
      float pa=a-PI*.5,bb=floor(pa/(TAU/3.)+.5),da=pa-bb*TAU/3.,deg=abs(da)*57.29578;
      if(r>12.16&&r<13.48&&deg>10.&&deg<42.64&&page>.001){float fk=(deg-10.)/2.72,kk=floor(fk),rw=floor((r-12.16)/.66),al=2.72/57.29578*r*S;vec2 ql=vec2(fract(fk)*al,fract((r-12.16)/.66)*.66*S);
        float sdc=da<0.?-1.:1.,own=step(abs(bb),.5),cell=rect(ql,vec2(.07,.07),vec2(al-.07,.66*S-.07)),seedc=kk+rw*17.+sdc*31.+bb*53.+uSeed*7.,act=uModules[int(mod(bb+3.,3.))*48+(sdc<0.?0:24)+int(kk)*2+int(rw)],fq=(uScan*9.-kk)/1.4,fl=uScan<1.6?exp(-fq*fq)*own:0.;
        add+=mix(cc,hot,fl)*cell*(.08+.46*act+fl)*(.38+.62*own)*page;}
      {vec2 c=15.*vec2(cos(ai-uRot),sin(ai-uRot)),dc=m-c;float ld=length(dc);if(ld<1.05){float f=clamp(uShips[int(ri)],0.,1.),hi=(abs(ri-uHi.x)<.5||abs(ri-uHi.y)<.5)?1.:0.,fa=fract(.25-atan(dc.y,dc.x)/TAU);
        add+=mix(cc,hot,hi*.6)*(ln(abs(ld-.82)*S,.032)*.55+ln(abs(ld-.66)*S,.075)*step(fa,f)*(.55+.45*hi))*(1.-uShow*.5)*live;
        if(uSide<.5)add+=mix(cc,hot,hi)*glyph(pp,MC+c*S-vec2(.27,.45),.9,11.+ri,1.)*(.55+.45*hi)*live;
        else if(uSide>2.5&&uSide<3.5)add+=mix(cc,hot,hi)*gstr(pp,MC+c*S-vec2(.29,.4),.8,1.,61.+ri)*(.55+.45*hi)*live;
        else add+=mix(cc,hot,hi)*clamp((.13-ld*S)/AA+.5,0.,1.)*(.4+.6*hi)*live;}}
      {vec2 er=vec2(cos(aj-uRot),sin(aj-uRot)),et=vec2(-er.y,er.x);float al=dot(m,er),ac=abs(dot(m,et)),own=abs(mod(aj-uRot+.001,TAU)-PI*.5-.001)<.05?1.:0.;
        if(al>11.5&&al<14.05&&ac<.6){float pk=uPk[int(ej)],hd=11.5+(14.05-11.5)*pk;add+=cc*ln(ac*S,.03)*(.3+.3*own)*live;if(pk>=0.){float b=hd-al;add+=hot*ln(ac*S,.06)*(b>0.?exp(-b/.7):exp(b/.12))*live*1.3;}}
        float c0=al-14.75,dm=max(ac*1.05+c0*.72-.42,-c0-.42)*S;add+=mix(cc,hot,own)*clamp(.5-dm/AA,0.,1.)*(.32+.68*own)*live;
        if(own>.5){float ld=length(vec2(c0,ac));add+=hot*(ln(abs(ld-.82)*S,.04)+ln(abs(ld-.98)*S,.02)*.6)*live;}}
      add+=cc*ln(abs(r-14.75)*S,.018)*.2*live;
    }
    if(uShow>.01){float a01=fract(.25-a/TAU),sg=floor(a01*6.),inSeg=step(.03,fract(a01*6.))*step(fract(a01*6.),.97),lit=clamp(uCount-sg,0.,1.);
      add+=mix(cc,hot,lit)*ln(abs(r-rd)*tp,.2)*inSeg*(.25+1.1*lit)*uShow*live;}
  }
  if(live>.001&&sr<4.2){
    float full=step(5.99,uCount)*uShow,Rs=1.28*(1.+.12*sin(uGlitchClock[11])*full);
    vec2 sn=(pp-MC)/(S*Rs);float mu=sqrt(max(0.,1.-dot(sn,sn))),gran=.9+.2*vn2(sn*7.+uGlitchClock[14])*vn2(sn*15.+uGlitchClock[15]+4.);
    vec3 cn=mix(vec3(.3,.38,.98),vec3(.96,.98,1.),pow(mu,.62))*gran,gw=vec3(.36,.44,1.);
    float sa=atan(pp.y-MC.y,pp.x-MC.x),th=sa*36./TAU,ci=floor(th),raysN=(1.0 - smoothstep(.12,.5,abs(fract(th)-.5)))*step(sr,Rs+.55+.75*h1(ci+3.))*step(Rs,sr);
    add+=gw*(exp(-sr*sr/1.9)*.34+raysN*.34*exp(-(sr-Rs)*1.6))*(1.-uInv)*live;
    float mk=clamp((Rs-sr)*S/AA+.5,0.,1.);add=add*(1.-mk*.9);over=max(over,mk*.9*live);
    add+=mix(cn*1.08,vec3(0.),uInv)*mk*live;
    add+=mix(vec3(.62,.72,1.),uGold*1.5,uInv)*ln(abs(sr-Rs)*S,.035)*live;
  }
  if(live>.001)for(int k=0;k<4;k++){
    float vis=uPl[k].w;if(vis<=.003)continue;vec2 c=uPl[k].xy,dc=pp-c;float Rp=uPl[k].z,dl=length(dc);if(dl>Rp*3.4+1.2)continue;
    vec3 tint=k==0?vec3(.84,.9,.96):k==1?vec3(.55,.82,1.):k==2?vec3(.74,.66,1.):vec3(.45,.96,1.);tint=mix(tint,uGold,uInv);
    if(k==1){for(int j=0;j<3;j++){float fj=float(j),rl=Rp*(1.32+.2*fj),th=.5+1.5*fj,inc=j==0?.5:j==1?.72:.3,ct=cos(th),st=sin(th);vec2 e=vec2(ct*dc.x+st*dc.y,(-st*dc.x+ct*dc.y)/inc);float el=length(e),gg=length(vec2(e.x,e.y/inc))/max(el,.001);
        float ang=atan(e.y,e.x),ph=fract((uPlB[1].z*(1.-.25*fj)+fj*2.-ang)/TAU),ta=exp(-ph*5.);add+=cc*ln(abs(el-rl)/gg,.016)*(.1+.75*ta)*vis*live;}}
    if(k==3){float th=uPlB[3].w;vec2 cs=c+Rp*2.05*vec2(cos(th),sin(th)*.8),ds=pp-cs;float ls=length(ds),Rm=max(.2,Rp*.2);add+=cc*ln(abs(length(vec2(dc.x,dc.y/.8))-Rp*2.05)*.9,.012)*.32*vis*live;
      float mm=clamp((Rm-ls)/AA+.5,0.,1.);add=add*(1.-mm);add+=tint*(.28+.9*max(dot(normalize(MC-cs+.001),ds/Rm),0.))*mm*vis*live;add+=vec3(.9,1.,1.)*(1.0 - smoothstep(.02,.09,length(ds/Rm-vec2(.36,.36))))*mm*vis*live;}
    float mk=clamp((Rp-dl)/AA+.5,0.,1.);
    if(mk>0.){vec2 nn=dc/Rp;float nz=sqrt(max(0.,1.-dot(nn,nn)));vec2 L2=normalize(MC-c+.0001);vec3 L=normalize(vec3(L2,.5));float lit=max(dot(vec3(nn,nz),L),0.),spin=uPlB[k].z,lat=nn.y*.94+nn.x*.34,lon=atan(nn.x,nz)+spin,alb=.6;
      if(k==0)alb=.42+.4*planetNoise(lon,lat,2.4,3.);else if(k==1)alb=.34+.75*smoothstep(.42,.62,planetNoise(lon,lat,1.7,9.)+.35*planetNoise(lon,lat,5.,0.));else if(k==2)alb=.4+.45*(.5+.5*sin(lat*12.+1.6*planetNoise(lon,lat,1.2,5.)));else alb=.42+.4*(.5+.5*sin(lat*17.+planetNoise(lon,lat,1.4,0.)));
      vec3 col=tint*alb*(.1+1.08*lit);
      if(k>=2)col+=mix(vec3(.55,.97,1.),uGold,uInv)*ln(abs(lat)*Rp,.018+.012*Rp)*(.5+.5*lit)*1.1;
      if(k==0){vec2 hd=nn+L2*.6;float rh=length(hd);col+=mix(vec3(1.,.26,.1),uGold,uInv)*((1.0 - smoothstep(.08,.3,rh))*1.3+ln(abs(rh-.36)*Rp,.012)*.5);}
      add=add*(1.-mk)+col*mk*vis*live;over=max(over,mk*vis*live);}
    add+=mix(tint,hot,.35)*ln(abs(dl-Rp),.03)*(.25+.75*max(dot(dc/max(dl,.001),normalize(MC-c+.0001)),0.))*vis*live;
    if(uTear<.02){vec2 o=c+vec2(Rp+.18,Rp*.28);if(uSide>2.5&&uSide<3.5)add+=cc*gstr(pp,o,.72,1.,31.+float(k))*.85*vis*live*(1.-uShow*.5);else add+=cc*glyph(pp,o,.82,17.+float(k)*2.,2.)*.85*vis*live*(1.-uShow*.5);}
  }
  if(uPage<1.5){
    if(pp.y>29.62&&pp.y<32.5&&page>.001){float dxs=abs(pp.x-23.562)-1.6,cw=1.713,kk=floor(dxs/cw);if(dxs>0.&&kk<12.){float rw=floor((pp.y-29.62)/1.44),sdc=pp.x<23.562?-1.:1.;vec2 ql=vec2(fract(dxs/cw)*cw,fract((pp.y-29.62)/1.44)*1.44);
        float cell=rect(ql,vec2(.09,.1),vec2(cw-.09,1.34)),seedc=kk+rw*17.+sdc*31.+uSeed*7.,act=step(.32,h1(seedc+floor(uTime/(3.+h1(seedc)*5.)))),fq=(uScan*9.-kk)/1.4,fl=uScan<1.6?exp(-fq*fq):0.;
        add+=mix(cc,hot,fl)*cell*(.07+.36*act+fl)*page;}}
    if(pp.x>39.5&&pp.y>12.&&pp.y<26.&&live>.001){for(int k=0;k<6;k++){float y=24.85-float(k)*2.18,f=clamp(uShips[k],0.,1.),hi=(abs(float(k)-uHi.x)<.5||abs(float(k)-uHi.y)<.5)?1.:0.;
        add+=mix(cc,hot,hi)*num(pp,vec2(40.95,y-.15),.84,69.+291.*f,3.,0.)*(.62+.38*hi)*live;
        add+=cc*ln(abs(pp.y-y-.26),.016)*step(43.,pp.x)*step(pp.x,45.72)*.35*live;add+=mix(cc,hot,hi)*rect(pp,vec2(43.+2.6*f-.09,y+.07),vec2(43.+2.6*f+.09,y+.45))*(.6+.4*hi)*live;}}
    if(pp.y>2.1&&pp.y<9.3&&pp.x>1.35&&pp.x<45.75&&page>.001){
      float xN=(pp.x-23.562)/22.2,yL=2.5,yH=8.9;
      add+=cc*(ln(abs(pp.y-yL),.012)+ln(abs(pp.y-yH),.012))*.22*page;add+=cc*ln(abs(pp.x-23.562),.022)*.7*page;
      {float tick=abs(fract(xN*4.+.5)-.5)/4.*22.2;add+=cc*ln(tick,.012)*.2*page;}
      for(int k=0;k<6;k++){float u0=uShipU[k];if(u0<0.)continue;float u=fract(u0+xN*.5),dw=uDwell,LL=.5-dw,f,df;
        if(u<dw){f=0.;df=0.;}else if(u<.5){float x=(u-dw)/LL;f=.5-.5*cos(PI*x);df=PI/2.*sin(PI*x)/LL;}else if(u<.5+dw){f=1.;df=0.;}else{float x=(u-.5-dw)/LL;f=.5+.5*cos(PI*x);df=-PI/2.*sin(PI*x)/LL;}
        float y=mix(yL,yH,f),sl=df*(yH-yL)*.5/22.2,dd=abs(pp.y-y)/sqrt(1.+sl*sl),hi=(abs(float(k)-uHi.x)<.5||abs(float(k)-uHi.y)<.5)?1.:0.,past=step(xN,0.);
        add+=mix(cc,hot,hi*.75)*ln(dd,hi>.5?.048:.03)*(past>.5?(.34+.5*hi)*(.45+.55*(1.+xN)):(.16+.22*hi))*page;
        vec2 cn=pp-vec2(23.562,y);if(abs(xN)<.03)add+=mix(cc,hot,hi)*clamp((.2-length(vec2(pp.x-23.562,pp.y-mix(yL,yH,clamp(uShips[k],0.,1.)))))/AA+.5,0.,1.)*page*(.7+.3*hi);}
    }
    if(uShow>.01&&pp.y>1.6&&pp.y<9.3){float a01=(pp.x-1.4)/44.3,sg=floor(a01*6.),lit=clamp(uCount-sg,0.,1.),inS=step(.012,fract(a01*6.))*step(fract(a01*6.),.988)*step(0.,a01)*step(a01,1.);add+=mix(cc*.22,hot*.8,lit)*rect(pp,vec2(1.4,4.9),vec2(45.7,6.1))*inS*uShow*live;}
  }else if(uSide>5.5){
    if(uSide<6.5&&live>.001){
      for(int k=0;k<4;k++){
        float fk=float(k),vis=uPl[k].w,ph=fract(uPlB[k].x/TAU),F=4.5,Rk=k==0?3.0:k==1?2.45:k==2?3.6:2.75;
        vec2 c=vec2(k==0||k==2?5.75:41.374,k<2?30.9:5.1),dirC=normalize(MC-c),tag=c+dirC*F;
        vec3 tint=mix(ptint(k),uGold,uInv),pc=vec3(uPl[k].xy,uPl[k].z);
        if(vis>.003){vec2 dp=normalize(pc.xy-tag);add+=tint*ln(seg(pp,tag+dp*.75,pc.xy-dp*(pc.z+.1)),.014)*.3*vis*live*(1.-uShow*.5);}
        vec2 dv=pp-c;float dl=length(dv);if(dl>F+1.2)continue;
        float boxT=rect(pp,tag-vec2(.62,.56),tag+vec2(.62,.56)),fa=fract(.25-atan(dv.y,dv.x)/TAU),tk=abs(fract(fa*24.+.5)-.5)/24.*TAU*F;
        add+=cc*(ln(abs(dl-F),.022)*.5+ln(tk,.014)*step(F,dl)*step(dl,F+.24)*.5)*(1.-boxT)*live;
        add+=tint*ln(abs(dl-F-.42),.05)*step(fa,ph)*(.3+.5*vis)*(1.-boxT)*live;
        add=add*(1.-boxT*.85);add+=tint*glyph(pp,tag-vec2(.6,.5),1.,17.+fk*2.,2.)*live;
        float R=Rk*(.3+.7*vis);
        if(vis>.003&&dl<R+.2){
          vec2 nn=dv/R;float mk=clamp((R-dl)/AA+.5,0.,1.),l2=min(1.,dot(nn,nn)),nz=sqrt(1.-l2),tilt=k==0?.2:k==1?.41:k==2?.32:.48,ct=cos(tilt),st=sin(tilt),spin=uPlB[k].z;
          vec2 L2=normalize(MC-pc.xy+.0001);vec3 L=normalize(vec3(L2,.4));float lit=max(dot(vec3(nn,nz),L),0.);
          vec2 uvm=sphUV(nn,ct,st,spin),dxm=sphUV(nn+PDX/R,ct,st,spin)-uvm,dym=sphUV(nn+PDY/R,ct,st,spin)-uvm;dxm.x-=floor(dxm.x+.5);dym.x-=floor(dym.x+.5);
          vec3 mp=pmap(k,uvm,dxm,dym);float lat=-st*nn.x+ct*nn.y,lum=dot(mp,vec3(.33));
          if(k==2)mp=mix(mp,lum*vec3(.72,.56,1.15)*1.15,.78);
          vec3 col=mix(mix(mp,mp*tint*1.25,.3),lum*uGold*1.5,uInv)*(.03+1.18*lit);
          if(k==0){vec2 hd=nn+L2*.58;float rh=length(hd);col*=1.-.55*(1.0 - smoothstep(.015,.05,abs(lat-.06)))*step(.26,rh);col*=1.-.6*(1.0 - smoothstep(.2,.27,rh));col+=mix(vec3(1.,.26,.1),uGold,uInv)*((1.0 - smoothstep(.03,.17,rh))*1.5+exp(-rh*rh/.05)*.42+ln(abs(rh-.23)*R,.012)*.45);}
          if(k==1)col+=mix(vec3(1.,.72,.42),uGold,uInv)*smoothstep(.5,.85,lum)*(1.-smoothstep(.0,.22,lit))*1.1;
          if(k>=2)col+=mix(vec3(.55,.97,1.),uGold,uInv)*(ln(abs(lat)*R,.026+.008*R)*(.35+.65*lit)*1.2);
          col+=mix(tint,hot,.3)*pow(1.-nz,3.)*(.15+.85*lit)*(k==0?.3:.75);
          add=add*(1.-mk)+col*mk*vis*live;over=max(over,mk*vis*live);}
        if(k==1&&vis>.003&&dl<R*1.9+.2)for(int j=0;j<3;j++){float fj=float(j),rl=R*(1.32+.17*fj),th=.5+1.5*fj,inc=j==0?.5:j==1?.72:.3,ca2=cos(th),sa2=sin(th);vec2 e=vec2(ca2*dv.x+sa2*dv.y,(-sa2*dv.x+ca2*dv.y)/inc);float el=length(e),gg=length(vec2(e.x,e.y/inc))/max(el,.001),ang=atan(e.y,e.x),pj=fract((uPlB[1].z*(1.-.25*fj)+fj*2.-ang)/TAU),behind=step(0.,-sa2*dv.x+ca2*dv.y)*step(dl,R);
            add+=cc*ln(abs(el-rl)/gg,.016)*(.12+.8*exp(-pj*5.))*(1.-behind)*vis*live;}
        if(k==3&&vis>.003){float th=uPlB[3].w;vec2 cs=c+R*1.5*vec2(cos(th),sin(th)*.72),ds=pp-cs;float ls=length(ds),Rm=.52,mm=clamp((Rm-ls)/AA+.5,0.,1.);add+=cc*ln(abs(length(vec2(dv.x,dv.y/.72))-R*1.5)*.85,.012)*.3*vis*step(R,dl)*live;
          if(mm>0.&&(sin(th)<0.||dl>R)){vec2 ns=ds/Rm;float zs=sqrt(max(0.,1.-dot(ns,ns))),ls2=max(dot(vec3(ns,zs),normalize(vec3(normalize(MC-pc.xy+.0001),.4))),0.);vec3 cm=mix(vec3(.62,.66,.72),uGold,uInv)*(.08+1.05*ls2);cm*=1.-.6*(1.0 - smoothstep(.02,.06,abs(ns.y)));cm*=1.-.8*(1.0 - smoothstep(.24,.3,length(ns-vec2(.32,.38))));add=add*(1.-mm)+cm*mm*vis*live;over=max(over,mm*vis*live);}}
        float pr=smoothstep(.6,1.,uInv)*(1.-vis);
        if(pr>.002){if(k<3){float phi=uTime*.768*(k==0?1.:k==1?-.78:.61)+fk*1.1,psi=.5+fk*1.9,ry=max(.1,abs(cos(phi))),cp=cos(psi),sp=sin(psi);vec2 e=vec2(cp*dv.x+sp*dv.y,(-sp*dv.x+cp*dv.y)/ry);float el=length(e);add+=uGold*ln(abs(el-3.4)*max(ry,.12)*1.2,.05)*pr*live;}
          else{float dk=clamp((2.3-dl)/AA+.5,0.,1.);add=add*(1.-dk*pr);add+=uGold*ln(abs(dl-2.3),.05)*1.3*pr*live;over=max(over,dk*pr*live);}}
      }
    }else if(uSide<7.5&&live>.001){
      float rb0=16.5,rb1=18.1,a01=fract((a-uTime*.012)/TAU);
      if(r>15.9&&r<18.7){
        add+=cc*(ln(abs(r-rb0)*S,.03)+ln(abs(r-rb1)*S,.03))*.8*live;
        if(r>rb0+.2&&r<rb1-.2){float cN=floor(a01*84.),hg=rb1-rb0-.4;vec2 q=vec2(fract(a01*84.)*.72,(r-rb0-.2)/hg);add+=cc*ln(gly(q,mod(cN,84.)*1.7+5.)*hg*S,.055)*.82*live;}
        if(r<rb0){float t5=abs(fract(a01*72.+.5)-.5)/72.*TAU*r*S,t30=abs(fract(a01*12.+.5)-.5)/12.*TAU*r*S;add+=cc*(ln(t5,.016)*step(rb0-.2,r)+ln(t30,.032)*step(rb0-.42,r))*.6*live;}
        if(r>rb1)for(int k=0;k<6;k++){float u0=uShipU[k],hi=(abs(float(k)-uHi.x)<.5||abs(float(k)-uHi.y)<.5)?1.:0.,ang=TAU*max(u0,0.)-PI*.5,dA=abs(fract((a-ang)/TAU+.5)-.5)*TAU*r*S,h=(r-rb1)*S;add+=mix(cc,hot,hi)*clamp((.3*(1.-h/.5)-dA)/AA+.5,0.,1.)*step(h,.5)*(.55+.45*hi)*live;}
      }
      if(r>18.6){float t3=abs(fract(a*120./TAU+.5)-.5)/120.*TAU*r*S,t30=abs(fract(a*12./TAU+.5)-.5)/12.*TAU*r*S,ra=abs(fract(r/1.3+.5)-.5)*1.3*S,mkc=smoothstep(18.6,19.4,r);
        add+=cc*(ln(t3,.012)*.16*exp(-(r-18.6)*.12)+ln(t30,.02)*.36+ln(ra,.012)*.1)*mkc*live;}
    }else if(uSide<8.5&&live>.001){
      add+=skyChart(pp,cc,hot)*live;
    }else if(uSide<9.5&&live>.001){
      // The dashed horizon is at z364.
      float mkb=smoothstep(16.5,17.3,r),kx=23./156.,y0=.5,kz=35./470.,hx=abs(pp.x-23.562),rr=hx/kx,bb=clamp(rr/4.,0.,39.),fb=fract(bb);int i0=int(floor(bb));float z0=uProf[i0],z1=uProf[i0>0?i0-1:0],yT=y0+z0*kz,yL=y0+z1*kz,bw=4.*kx;
      {vec2 gq=abs(fract(pp/2.4+.5)-.5)*2.4,gm=abs(fract(pp/12.+.5)-.5)*12.;add+=cc*(ln(min(gq.x,gq.y),.01)*.09+ln(min(gm.x,gm.y),.014)*.16)*mkb*live;}
      if(mkb>0.&&hx<23.4){add+=cc*step(pp.y,yT)*.022*mkb*live;add+=cc*(ln(abs(pp.y-yT),.018)+ln(fb*bw,.018)*step(min(yT,yL),pp.y)*step(pp.y,max(yT,yL)))*.62*mkb*live;add+=cc*ln(abs(pp.y-y0),.02)*.5*mkb*live;
        add+=cc*ln(abs(pp.y-(y0+364.*kz)),.014)*step(fract(hx*1.4),.55)*.42*mkb*live;}
      add+=mix(vec3(.62,.72,1.),uGold,uInv)*ln(hx,.03)*step(y0+10.*kz,pp.y)*step(pp.y,y0+470.*kz)*mkb*.7*live;
      for(int k=0;k<6;k++){float fk=float(k),pk=fk*PI/3.-uRot,hi=(abs(fk-uHi.x)<.5||abs(fk-uHi.y)<.5)?1.:0.,xr=23.562+134.*cos(pk)*kx+(sin(pk)>0.?.16:-.16)*step(abs(cos(pk)),.7),f=clamp(uShips[k],0.,1.),dxk=abs(pp.x-xr),ys=y0+(69.+291.*f)*kz;
        add+=cc*ln(dxk,.016)*step(y0+29.*kz,pp.y)*step(pp.y,y0+450.*kz)*.42*mkb*live;add+=mix(cc,hot,hi)*ln(dxk,.08)*step(y0+69.*kz,pp.y)*step(pp.y,ys)*(.14+.3*hi)*mkb*live;add+=mix(cc,hot,hi)*clamp(.5-(dxk/.32+abs(pp.y-ys)/.5-1.)*.32/AA,0.,1.)*(.75+.25*hi)*mkb*live;}
    }
  }else if(uSide>.5){
    if(uSide<1.5&&live>.001){
      float Am=.84,ax=m.x>=0.?a:(a>0.?PI-a:-PI-a);
      if(abs(ax)<Am+.02&&r>16.9&&r<23.3){
        float y01=(ax+Am)/(2.*Am),ends=smoothstep(0.,.01,y01)*(1.0 - smoothstep(.99,1.,y01));
        {float R=23.05,tk=abs(fract(y01*12.+.5)-.5)/12.*2.*Am*R*S;add+=cc*ln(tk,.014)*smoothstep(22.72,22.78,r)*(1.0 - smoothstep(23.24,23.3,r))*.42*ends*live;}
        if(m.x>=0.){float ki=floor((r-17.25)/1.02+.5);
          if(ki>=0.&&ki<5.5){float R=17.25+ki*1.02,dr=abs(r-R)*S,f=clamp(uShips[int(ki)],0.,1.),hi=(abs(ki-uHi.x)<.5||abs(ki-uHi.y)<.5)?1.:0.,hd=(y01-f)*2.*Am*R*S;
            add+=cc*ln(dr,.016)*.32*ends*live;
            add+=mix(cc,hot,hi*.7)*ln(dr,.09)*step(y01,f)*ends*(.18+.42*hi)*(.3+.7*y01/max(f,.02))*live;
            add+=mix(cc,hot,hi)*clamp((.2-length(vec2(hd,dr)))/AA+.5,0.,1.)*(.72+.28*hi)*live;}}
        else{float ki=floor((r-17.7)/1.5+.5);
          if(ki>=0.&&ki<3.5){int k=int(ki);float R=17.7+ki*1.5,dr=abs(r-R)*S,vis=uPl[k].w,ringK=step(ki,2.5)*smoothstep(.6,1.,uInv),ph=ringK>0.?fract(uTime*.1222*(ki<.5?1.:ki<1.5?-.78:.61)):fract(uPlB[k].x/TAU),g=fract(ph-y01),hd=(y01-ph)*2.*Am*R*S,rm=.2+.075*ki;
            vec3 tc=mix(ptint(k),uGold,uInv);float sh=max(vis,ringK);
            add+=cc*ln(dr,.016)*.32*ends*live;
            add+=tc*ln(dr,.07)*exp(-g*7.)*.5*sh*ends*live;
            add+=tc*clamp((rm-length(vec2(hd,dr)))/AA+.5,0.,1.)*sh*live;}}
      }
    }else if(uSide<2.5&&live>.001){
      float y0=2.1,sp=29.9,yn=(pp.y-y0)/sp,inY=step(0.,yn)*step(yn,1.);
      if(inY>.5){
        float tk=abs(fract(yn*24.+.5)-.5)/24.*sp,maj=abs(fract(yn*4.+.5)-.5)/4.*sp;
        if(pp.x>40.6){float ki=floor((pp.x-41.25)/.86+.5);
          if(ki>=0.&&ki<5.5){float xc=41.25+ki*.86,dxk=abs(pp.x-xc),f=clamp(uShips[int(ki)],0.,1.),hi=(abs(ki-uHi.x)<.5||abs(ki-uHi.y)<.5)?1.:0.,dy=pp.y-(y0+sp*f);
            add+=cc*ln(dxk,.014)*.3*live;
            add+=mix(cc,hot,hi*.7)*ln(dxk,.1)*step(yn,f)*(.14+.36*hi)*(.3+.7*yn/max(f,.02))*live;
            add+=mix(cc,hot,hi)*clamp(.5-(dxk/.27+abs(dy)/.42-1.)*.27/AA,0.,1.)*(.75+.25*hi)*live;}
          add+=cc*(ln(tk,.012)*step(46.,pp.x)*step(pp.x,46.32)+ln(maj,.018)*step(45.75,pp.x)*step(pp.x,46.32))*.42*live;}
        if(pp.x<6.5){float ki=floor((pp.x-1.95)/1.15+.5);
          if(ki>=0.&&ki<3.5){int k=int(ki);float xc=1.95+ki*1.15,dxk=abs(pp.x-xc),vis=uPl[k].w,ringK=step(ki,2.5)*smoothstep(.6,1.,uInv),ph=ringK>0.?fract(uTime*.1222*(ki<.5?1.:ki<1.5?-.78:.61)):fract(uPlB[k].x/TAU),g=fract(ph-yn),rm=.2+.075*ki,sh=max(vis,ringK);
            vec3 tc=mix(ptint(k),uGold,uInv);
            add+=cc*ln(dxk,.014)*.3*live;add+=tc*ln(dxk,.07)*exp(-g*7.)*.5*sh*live;
            add+=tc*clamp((rm-length(vec2(dxk,pp.y-(y0+sp*ph))))/AA+.5,0.,1.)*sh*live;}
          add+=cc*(ln(tk,.012)*step(.8,pp.x)*step(pp.x,1.12)+ln(maj,.018)*step(.8,pp.x)*step(pp.x,1.37))*.42*live;}
      }
    }else if(uSide<3.5&&live>.001){
      add+=cc*gstr(pp,vec2(7.5,34.95),1.05,6.,uBay*3.+11.)*.9*live;add+=cc*gstr(pp,vec2(38.9,35.1),.7,14.,71.)*.5*live;
      if(pp.x<7.2&&pp.y>6.){add+=cc*gstr(pp,vec2(1.3,30.9),.92,3.,5.)*.95*live;add+=cc*ln(abs(pp.y-30.55),.012)*step(1.3,pp.x)*step(pp.x,6.6)*.4*live;
        for(int k=0;k<4;k++){float y=28.75-float(k)*3.05,fk=float(k),ph=fract(uPlB[k].x/TAU);vec3 tc=mix(ptint(k),uGold,uInv);
          add+=tc*gstr(pp,vec2(1.3,y),.78,1.,31.+fk)*.95*live;add+=cc*gstr(pp,vec2(2.4,y+.08),.6,5.,37.+fk*7.)*.72*live;add+=cc*gstr(pp,vec2(2.4,y-.72),.48,6.,53.+fk*11.+floor(ph*6.))*.46*live;}}
      if(pp.x>40.2&&pp.y>6.){add+=cc*gstr(pp,vec2(40.65,30.9),.92,3.,17.)*.95*live;add+=cc*ln(abs(pp.y-30.55),.012)*step(40.65,pp.x)*step(pp.x,45.9)*.4*live;
        for(int k=0;k<6;k++){float y=29.-float(k)*1.72,fk=float(k),f=clamp(uShips[k],0.,1.),hi=(abs(fk-uHi.x)<.5||abs(fk-uHi.y)<.5)?1.:0.;
          add+=mix(cc,hot,hi)*(gstr(pp,vec2(40.65,y-.1),.76,1.,61.+fk)+gstr(pp,vec2(41.5,y-.1),.76,2.,73.+fk*5.+floor(f*9.)))*(.62+.38*hi)*live;
          add+=cc*ln(abs(pp.y-y-.26),.014)*step(43.,pp.x)*step(pp.x,45.9)*.32*live;add+=mix(cc,hot,hi)*rect(pp,vec2(43.+2.9*f-.08,y+.04),vec2(43.+2.9*f+.08,y+.48))*(.6+.4*hi)*live;}}
      if(pp.y<7.&&pp.y>4.9)add+=cc*(gstr(pp,vec2(1.45,5.2),.6,4.,91.)+gstr(pp,vec2(41.95,5.2),.6,4.,95.))*.62*live;
    }else if(uSide<4.5&&live>.001){
      float mid=1.-smoothstep(.07,.17,AA);
      if(pp.x>1.3&&pp.x<6.5){
        if(pp.y>23.6&&pp.y<32.3){float rf=(pp.y-23.6)/1.45,rw=floor(rf),fy=fract(rf),cx=floor((pp.x-1.3)/.18),fx=fract((pp.x-1.3)/.18),on=step(.42,h2(vec2(cx,rw+floor(uTime/(5.+rw*1.7))))),hh=.3+.7*h1(rw*3.1+cx*.07);
          add+=cc*mix(.1,on*step(fx,.6)*step(.1,fy)*step(fy,.1+.62*hh)*.42,mid)*live;}
        if(pp.y>7.4&&pp.y<22.6){float rf=(pp.y-7.4)/3.8,rw=floor(rf);int k=3-int(rw);if(k>=0&&k<4){float yc=7.4+3.8*(rw+.5),w=2.1+float(k)*.85,ph=uPlB[k].x*(float(k)+1.),xx=(pp.x-1.3)*w+ph,A=.95,y=yc+A*sin(xx),sl=A*w*cos(xx);vec3 tc=mix(ptint(k),uGold,uInv);
            add+=cc*ln(abs(pp.y-yc),.012)*.2*live;add+=tc*ln(abs(pp.y-y)/sqrt(1.+sl*sl),.022)*.55*max(uPl[k].w,.25)*live;}}
      }
      if(pp.x>40.7&&pp.x<45.9){float xn=(pp.x-40.7)/5.2;
        if(pp.y>21.1&&pp.y<32.3){float rf=(32.3-pp.y)/1.866,rw=floor(rf);int k=int(rw);if(k>=0&&k<6){float fk=float(k),hi=(abs(fk-uHi.x)<.5||abs(fk-uHi.y)<.5)?1.:0.,u0=uShipU[k],yb=32.3-1.866*(rw+1.)+.28,u=fract(max(u0,0.)-(1.-xn)*.5),dw=uDwell,LL=.5-dw,f,df;
            if(u<dw){f=0.;df=0.;}else if(u<.5){float x=(u-dw)/LL;f=.5-.5*cos(PI*x);df=PI/2.*sin(PI*x)/LL;}else if(u<.5+dw){f=1.;df=0.;}else{float x=(u-.5-dw)/LL;f=.5+.5*cos(PI*x);df=-PI/2.*sin(PI*x)/LL;}
            if(u0<0.){f=0.;df=0.;}
            float y=yb+f*1.28,sl=df*1.28*.5/5.2;
            add+=mix(cc,hot,hi*.7)*ln(abs(pp.y-y)/sqrt(1.+sl*sl),.024)*(.34+.5*hi)*(.35+.65*xn)*live;add+=cc*step(yb,pp.y)*step(pp.y,y)*(.03+.05*hi)*live;
            add+=mix(cc,hot,hi)*clamp((.13-length(vec2(pp.x-45.9,pp.y-y)))/AA+.5,0.,1.)*live;}}
        if(pp.y>7.4&&pp.y<19.6){float cx=floor(xn*14.),fx=fract(xn*14.),hh=.12+.8*vn(cx*3.1+uTime*.1+uSeed)*vn(cx*1.3-uTime*.07+9.);
          add+=cc*mix(.06,step(fx,.62)*step(pp.y,7.4+12.2*hh)*.26,mid)*live;add+=cc*ln(abs(pp.y-(7.4+12.2*hh)),.02)*step(fx,.62)*.5*live;}
      }
    }
    if(uSide<4.5&&(uSide<1.5||uSide>2.5)&&pp.y<5.&&live>.001){
      add+=cc*gauge(pp,vec2(3.3,2.75),1.75,1.-uNum.x/uInterval)*live;add+=cc*gauge(pp,vec2(43.82,2.75),1.75,uNum.y/uT)*live;
      if(pp.x>41.)for(int k=0;k<6;k++){float u0=uShipU[k];if(u0<0.)continue;float tk=TAU*fract(uNum.y/uT-u0);vec2 c=vec2(43.82,2.75)+1.75*vec2(sin(tk),cos(tk));add+=hot*clamp((.07-length(pp-c))/AA+.5,0.,1.)*.8*live;}}
  }else{
    if(pp.x>40.4&&pp.y>19.&&pp.y<30.6&&live>.001){for(int k=0;k<6;k++){float y=29.25-float(k)*1.72,f=clamp(uShips[k],0.,1.),hi=(abs(float(k)-uHi.x)<.5||abs(float(k)-uHi.y)<.5)?1.:0.;
        add+=mix(cc,hot,hi)*num(pp,vec2(41.72,y-.14),.82,69.+291.*f,3.,0.)*(.62+.38*hi)*live;
        add+=cc*ln(abs(pp.y-y-.23),.014)*step(43.45,pp.x)*step(pp.x,45.72)*.32*live;add+=mix(cc,hot,hi)*rect(pp,vec2(43.45+2.27*f-.08,y+.05),vec2(43.45+2.27*f+.08,y+.41))*(.6+.4*hi)*live;}}
    if(pp.y<6.&&pp.y>1.2&&live>.001){
      add+=cc*num(pp,vec2(1.35,3.05),1.75,uNum.x,1.,1.)*.95*live;add+=cc*ln(abs(pp.y-2.28),.02)*step(1.35,pp.x)*step(pp.x,6.5)*.3*live;add+=hot*rect(pp,vec2(1.35,2.2),vec2(1.35+5.15*(1.-uNum.x/uInterval),2.36))*.85*live;
      add+=cc*num(pp,vec2(40.65,3.05),1.75,uNum.y,2.,1.)*.95*live;add+=cc*ln(abs(pp.y-2.28),.02)*step(40.65,pp.x)*step(pp.x,45.72)*.3*live;add+=hot*rect(pp,vec2(40.65,2.2),vec2(40.65+5.07*uNum.y/uT,2.36))*.85*live;}
  }
  float inkK=0.;
  if(G>.003&&live>.001){
    if(uGar<3.5){
      vec2 bq=pp/vec2(2.4,1.18),bi=floor(bq),bf=fract(bq);float hb=h2(bi+floor(uTime*(1.1+2.4*h2(bi+5.)))*.37+uSeed);
      if(hb>(uGar>2.5?.9:.955)-.03*burst){float hc=fract(hb*57.);add+=mix(cc,hot,step(.86,hc))*step(.06,bf.x)*step(bf.x,.94)*step(.1,bf.y)*step(bf.y,.9)*(hc<.35?step(.5,fract(pp.y*5.)):1.)*(.22+.5*hc)*min(1.,G)*live;}
      if(uGar<2.5){float dens=.62*min(1.,G)+.25*burst,cls=.5,c2=.5,ink=garbage(pp,dens,cls),dd=.11*burst;vec3 ic=vec3(ink);
        if(dd>.001){ic.r=garbage(pp+vec2(dd,0.),dens,c2);ic.b=garbage(pp-vec2(dd,0.),dens,c2);}
        add+=(cls<.07?vec3(1.,.28,.14):cls<.2?hot:cc)*ic*(.5+.5*fract(cls*9.))*live;}
    }else{float ed=(dx-tw)*W/S0;
      if(uGar<4.5)over4(add,pp,uv,G,burst,live,sd,cc,hot);
      else if(uGar<5.5)over5(add,pp,uv,live);
      else if(uGar<6.5)over6(add,uv,ed,G,burst,live,sd,uGold);
      else if(uGar<7.5)over7(add,inkK,pp,uv,G,burst,live,sd);
      else{if(uGar>8.5)over6(add,uv,ed,G,burst,live,sd,uGold);over8(add,pp,live,cc,hot);}}
  }
  if(uScan<.9){float ys=uScan/.9*H;add+=hot*((1.0 - smoothstep(0.,.5,abs(p.y-ys)))*1.1+step(p.y,ys)*(1.0 - smoothstep(0.,4.,ys-p.y))*.12)*page;}
  vec3 win=vec3(0.);float eDisc=0.;
  if(ins>.5){vec3 rd=normalize(vP-uCam);win=subspace(rd);
    if(uTearK<1.5){vec2 cs=vec2(.5*W,.53*H);float rs=.2*H,l=length(uv*vec2(W,H)-cs);win=mix(win,vec3(.006),(1.0 - smoothstep(rs-.4,rs,l))*uStarA)+uGold*((1.0 - smoothstep(0.,.9,abs(l-rs)))*1.4+(1.0 - smoothstep(rs,rs*1.6,l))*step(rs,l)*.3)*uStarA;}
    else{
      // Reflect once about the panel's fixed centre plane. Reflecting about vP
      // gives every fragment of the curved screen a different sphere centre.
      vec4 hit=windowStarRay(rd);vec3 dv=hit.xyz;float tc=hit.w,ds=length(dv),Rs=uStarR;
      vec3 oc=windowStarCenter()-uCam,view=-oc/max(length(oc),1e-5),e1=vec3(-view.y,view.x,0.);
      e1=dot(e1,e1)>1e-12?normalize(e1):vec3(1.,0.,0.);
      vec3 e2=cross(view,e1);float ang=atan(dot(dv,e2),dot(dv,e1));
      float rays=smoothstep(.72,1.,sin(ang*44.+h1(floor((ang+PI)*7.))*5.))*(1.0 - smoothstep(Rs*1.04,Rs*2.5,ds))*step(Rs,ds);
      float starA=tc>0.?uStarA:0.;
      win=mix(win,vec3(.006),(1.0 - smoothstep(Rs-.3,Rs,ds))*starA);win+=uGold*((1.0 - smoothstep(0.,.55,abs(ds-Rs)))*1.5+(1.0 - smoothstep(Rs,Rs*1.5,ds))*step(Rs,ds)*.22+rays*.42)*starA;
      float ei=max(0.,(tw-dx)*W);win*=1.-.55*exp(-ei/2.2);win+=uGold*exp(-ei/.9)*.3;
      if(G>.003&&ei<9.&&uGar<3.5){vec2 fq=vec2(pp.x/2.1,(pp.y+uTime*(.2+.2*sd))/1.05),fi=floor(fq),ff=fract(fq);float hf=h2(fi+uSeed*3.+sd*7.);
        if(hf>.84){float ik=uGar<2.5?gtile(vec2(fract(ff.x*3.),ff.y),floor(fract(hf*37.+fi.x*.13+floor(ff.x*3.)*.37)*128.),vec2(.7,1.05)):1.;win+=uGold*(.1+.75*ik)*step(.06,ff.x)*step(ff.x,.94)*step(.1,ff.y)*step(ff.y,.9)*exp(-ei/3.2)*min(1.,G)*.8;}}}}
  add+=uGold*edgeT*1.4*(1.+G*(.55*vn(uv.y*31.+uGlitchClock[16])-.22));
  if(uTearK<1.5)add+=uGold*1.5*step(.985,h2(floor(vec2(uv.y*60.,uTime*8.))))*(1.0 - smoothstep(0.,.06,abs(dx-tw-.01*fract(uTime*3.))))*on;
  else{add+=uGold*burn*.16;
    if(uGar<3.5)add+=uGold*1.5*step(.985-.01*burst,h2(floor(vec2(uv.y*60.,uTime*8.))+sd*3.))*(1.0 - smoothstep(0.,.028,abs(dx-tw-.01*fract(uTime*3.))))*on*min(1.,G);
    vec2 eg=vec2((dx-tw)*W/.7,uv.y*H/.7-uGlitchClock[17]),ei=floor(eg),ef=fract(eg)-.5;float he=h2(vec2(ei.x,mod(ei.y,1024.))+uSeed);if(he>.93&&ei.x>=0.&&ei.x<4.)add+=uGold*clamp((.08-length(ef-(vec2(h1(he*7.),h1(he*11.))-.5)*.6))*.7/AA+.5,0.,1.)*(1.-ei.x/4.)*(1.-ins)*on*1.2;}
  // uBack controls backing opacity; above 0.3 the backing uses the panel colour.
  add+=mix(vec3(.01,.024,.042),vec3(.03,.021,.01),uInv)*smoothstep(.3,1.,uBack)*(1.-ins)*(1.-inkK);
  float aa=max(mix(uBack,max(.93,uBack),ins),over*.92);
  float sK=uScar*(1.-uAlpha),sW=min(1.,uScar*2.5)*(1.-uAlpha);   // The off screen keeps an opaque window; translucency reads as smoke in daylight.
  gl_FragColor=vec4((add*uGlow+win*ins)*uAlpha+uGold*(edgeT*1.4+burn*.16)*uGlow*sK+win*ins*sW,aa*uAlpha+max(ins*.93*sW,min(1.,edgeT)*.93*sK));}
