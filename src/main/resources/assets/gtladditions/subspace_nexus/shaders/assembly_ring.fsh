uniform vec3 uCam;uniform vec3 uStarC;uniform vec4 uRingPhase;uniform vec4 uDetailPhase;uniform float uGlow;uniform float uStyle;uniform float uBuildA;uniform float uGlitch;uniform float uHolo;uniform float uHoloA;uniform float uSeed;
uniform vec2 uAB;uniform vec3 uGold;uniform vec3 uIce;uniform vec3 uSteel;   // uSteel: body; uGold: trim (gold, silver-blue or graphite); uIce: lights.
uniform float uLit;uniform float uLiner;uniform float uMat;uniform float uGlyph;uniform float uTick;uniform float uRelK;uniform float uHoloMode;uniform float uStarR;uniform float uSegN;uniform float uKeyK;uniform float uAmbK;
uniform float uBodyK;uniform float uColl;uniform float uCollW;   // uColl: 0 mixed metal, 1 darker body colour, 2 body finish; uCollW: collar width.
uniform float uLv;uniform float uBeamR;   // uLv scales idle illumination; uBeamR scales beam reflection.
varying vec3 vP;varying vec3 vN;varying vec3 vL;varying float vU;varying float vRole;varying float vZ;varying float vRo;varying float vRad;varying vec3 vT;
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
float hh(float n){return fract(sin(n*127.1+.3)*43758.5453);}
float hexD(vec2 p){p=abs(p);return max(dot(p,vec2(.8660254,.5)),p.y);}
float sq(float x){return x*x;}
float dss(float e0,float e1,float x){float t=clamp((x-e0)/(e1-e0),0.,1.);return 6.*t*(1.-t)/(e1-e0);}
vec3 safeNorm(vec3 v,vec3 fb){float l=dot(v,v);return l>1e-12?v*inversesqrt(l):fb;}
vec2 segV(vec2 p,vec2 a,vec2 b){vec2 pa=p-a,ba=b-a;return pa-ba*clamp(dot(pa,ba)/dot(ba,ba),0.,1.);}
vec3 envR2(vec3 P,vec3 R,vec3 ice,float bk,float bw){   // bk: beam gain; bw: beam width multiplier.
  float up=R.z,az=dot(R.xy,R.xy)>1e-12?atan(R.y,R.x):0.;vec3 e=mix(vec3(.006,.009,.018),vec3(.05,.07,.13),smoothstep(-.1,.6,up));
  e+=ice*exp(-sq((up-.03)/.15))*(.18+.4*pow(.5+.5*cos(az*6.),4.))+vec3(.006,.007,.009)*(1.0 - smoothstep(-.5,.05,up));
  float rr=dot(R.xy,R.xy);if(rr>1e-4){float t=-dot(P.xy,R.xy)/rr,z=P.z+t*R.z;vec2 q=P.xy+t*R.xy;
    if(t>0.&&z>10.&&z<980.){float d2=dot(q,q);e+=vec3(.78,.8,1.)*bk*(exp(-d2/(6.*bw))*.9+exp(-d2/(60.*bw))*.22)*(1.0 - smoothstep(600.,980.,z));}}
  return e;}
vec3 envR(vec3 P,vec3 R,vec3 ice){return envR2(P,R,ice,1.,1.);}
void main(){
  float u=vU,built=step(u,uBuildA),C=6.2831853*vRad,s=u*C;
  vec2 du=vec2(dFdx(u),dFdy(u)),dz=vec2(dFdx(vZ),dFdy(vZ)),dro=vec2(dFdx(vRo),dFdy(vRo));
  vec2 dS=C*du+(6.2831853*u)*dro;
  float fwS=abs(dS.x)+abs(dS.y),fwZ=abs(dz.x)+abs(dz.y),fwRo=abs(dro.x)+abs(dro.y);
  vec3 dpx=dFdx(vP),dpy=dFdy(vP);
  if(uHolo<.5){if(built<.5)discard;}else{if(built>.5||uHoloA<.003)discard;}
  float tg=uRingPhase.x;if(uGlitch>.01&&hh(floor(u*64.)*3.1+tg+uSeed)<.12*uGlitch)discard;
  vec3 n=normalize(vN),V=safeNorm(uCam-vP,vec3(0.,0.,1.));if(dot(n,V)<0.)n=-n;
  float role=floor(vRole+.5);bool topf=abs(role-2.)<.5,coll=abs(role-6.)<.5;   // role is interpolated: round it first
  if(uHolo>.5){
    if(uHoloMode<.5){float f=sq(max(0.,1.-abs(dot(n,V)))),scan=.55+.45*sin(vP.z*2.4-uDetailPhase.w);float a=(.06+.55*f)*scan*uHoloA*uGlow;gl_FragColor=vec4(uIce*a,a);return;}
    float fs=max(fwS,1e-4),ln=abs(role-1.)<.5?1.-smoothstep(.02,.06+fs*.6,abs(abs(vRo)-(uAB.x-.16))*1.414):0.;
    float ph=fract(s/7.-uRingPhase.y),band=1.-smoothstep(.04,.1+fs,min(ph,1.-ph)*7.),fr=1.-smoothstep(0.,.3+fs*2.,(u-uBuildA)*C);
    float a=(.75*ln+.5*band+1.5*fr+.02)*uHoloA*uGlow*(.85+.15*sin(uRingPhase.z+u*40.));gl_FragColor=vec4(uIce*a,a);return;}
  float y=topf?vRo+uAB.x:vZ+uAB.y,Ly=topf?2.*uAB.x:2.*uAB.y,dC=topf?6.2831853:0.;   // across-face coordinate in blocks (axial on the walls, radial on top / bottom)
  vec3 wq=vL,steel=uSteel,alb=steel,emi=vec3(0.);float ks=.5,sp=30.;vec2 hGrad=vec2(0.);
  if(abs(role-1.)<.5){alb=uGold*(.85+.15*fbm3(wq*1.3));ks=1.2;sp=46.;}
  else if(abs(role-4.)<.5){alb=vec3(.02,.025,.03);ks=.3;emi=uIce*(.4+1.3*pow(.5+.5*sin((s*.06-uDetailPhase.x)*6.2831853),6.));}
  else if(abs(role-8.)<.5){float lw=max(.045,fwZ*1.5);alb=vec3(.02,.025,.03);ks=.3;emi=uIce*((1.-smoothstep(lw*.45,lw,abs(vZ)))*(.045/lw)*(.75+.25*sin(s*.09-uRingPhase.w))+.06);}
  else if(abs(role-5.)<.5){alb=mix(steel,uGold,.5)*(.9+.2*fbm3(wq*vec3(.3,.3,8.)));ks=1.1;sp=60.;}
  else if(coll){alb=uColl<.5?mix(steel*1.2,uGold,.35):steel*(uStyle>2.5&&uStyle<3.5?uBodyK:1.)*(uColl<1.5?.72:1.);ks=1.;sp=54.;}
  else if(abs(role-7.)<.5){alb=steel*(.9+.2*fbm3(wq*vec3(.25,.25,22.)));ks=.85;sp=58.;}
  else{float dim=abs(role-3.)<.5?.85:1.,fw=max(fwS,topf?fwRo:fwZ),cell=uStyle<.5?1.2:uStyle<1.5?.62:uStyle<2.5?.36:uStyle<3.5?.6:.14;
    if(uStyle<.5){              // uStyle 0: armour plates.
      float nP=floor(C/2.8+.5),rows=max(1.,floor(Ly/1.45+.5)),yy=y/Ly*rows,row=floor(yy);vec2 g=vec2(u*nP+.5*mod(row,2.),yy),c=floor(g),f=fract(g);
      vec2 q=vec2(min(f.x,1.-f.x)*C/nP,min(f.y,1.-f.y)*Ly/rows);float seam=min(q.x,q.y),var=hh(mod(c.x,nP)*12.9+c.y*78.2+uSeed);
      float dqxU=(f.x<.5?C:-C),dqxY=min(f.x,1.-f.x)*dC/nP,dqyY=(f.y<.5?1.:-1.);
      hGrad=dss(.02,.1,seam)*(q.x<q.y?vec2(dqxU,dqxY):vec2(0.,dqyY));alb=steel*(.78+.5*var)*(.92+.16*fbm3(wq*vec3(.4,.4,6.)));
      vec2 rq=q-vec2(.2,.2);float rl=length(rq),rv=1.-smoothstep(.045,.07,rl),drv=rl>1e-5?-.6*dss(.045,.07,rl)/rl:0.;
      hGrad+=drv*vec2(rq.x*dqxU,rq.x*dqxY+rq.y*dqyY);alb=mix(alb,uGold*.55,rv*.7);
      if(hh(mod(c.x,nP)*3.1+c.y*5.7+uSeed)>.88)emi+=uIce*step(abs(f.y-.5),.045)*step(.25,f.x)*step(f.x,.75)*.9;ks=.55;sp=34.;}
    else if(uStyle<1.5){        // uStyle 1: hex tiles.
      float nH=floor(C/.62+.5),sz=C/nH;vec2 p=vec2(u*nH,y/sz),r=vec2(1.,1.7320508),hr=r*.5,a=mod(p,r)-hr,b=mod(p-hr,r)-hr,gv=dot(a,a)<dot(b,b)?a:b,id=p-gv;
      vec2 ag=abs(gv);float dHex=dot(ag,vec2(.8660254,.5)),e=.5-max(dHex,ag.y),var=hh(mod(floor(id.x+.5),nH)*12.9+floor(id.y*2.+.5)*7.1+uSeed);
      vec2 gHex=dHex>ag.y?sign(gv)*vec2(.8660254,.5):vec2(0.,sign(gv.y));
      hGrad=-dss(.0,.07,e)*gHex*vec2(nH,1./sz-y*dC/(nH*sz*sz));alb=mix(steel*.9,mix(steel,uGold,.25)*1.15,var*.6);alb=mix(uGold*.75,alb,smoothstep(.015,.04,e));
      if(var>.94)emi+=uIce*(.25+.3*sin(uDetailPhase.z+var*40.))*smoothstep(.04,.18,e);ks=.8;sp=50.;}
    else if(uStyle<2.5){        // uStyle 2: circuits.
      float nC=floor(C/.36+.5),rows=max(2.,floor(Ly/.36+.5));vec2 g=vec2(u*nC,y/Ly*rows),c=floor(g),f=fract(g);c.x=mod(c.x,nC);
      float r=hh(c.x*17.3+c.y*31.7+uSeed),w=.13,tr=0.,ry=rows/Ly;vec2 gtr=vec2(0.);
      float tx=1.-smoothstep(w,w+.07,abs(f.x-.5)),ty=1.-smoothstep(w,w+.07,abs(f.y-.5));
      float dtx=-dss(w,w+.07,abs(f.x-.5))*sign(f.x-.5)*nC,dty=-dss(w,w+.07,abs(f.y-.5))*sign(f.y-.5)*ry;
      if(r<.42){tr=ty;gtr=vec2(0.,dty);}
      else if(r<.56){tr=tx;gtr=vec2(dtx,0.);}
      else if(r<.7){float m1=step(f.x,.5+w),m2=step(.5-w,f.y),a1=ty*m1,a2=tx*m2;if(a1>=a2){tr=a1;gtr=vec2(0.,dty*m1);}else{tr=a2;gtr=vec2(dtx*m2,0.);}}
      else if(r<.8){float m1=step(.5-w,f.x),m2=step(f.y,.5+w),a1=ty*m1,a2=tx*m2;if(a1>=a2){tr=a1;gtr=vec2(0.,dty*m1);}else{tr=a2;gtr=vec2(dtx*m2,0.);}}
      float vm=step(.9,hh(c.x*3.3+c.y*9.1+uSeed));vec2 vf=f-.5;float vl=length(vf),via=vm*(1.-smoothstep(.2,.26,vl)),m=max(tr,via);
      float dv=vl>1e-5?-vm*dss(.2,.26,vl)/vl:0.;
      alb=mix(steel*.32,uGold*.9,m);hGrad=.7*(via>tr?dv*vf*vec2(nC,ry):gtr);ks=mix(1.4,1.2,m);sp=mix(96.,46.,m);
      if(r<.42){float ph=fract(u*nC/26.-uDetailPhase.y+hh(c.y*7.7+uSeed)*.8);emi+=uIce*tr*exp(-sq((ph-.5)*16.))*1.3;}}
    else if(uStyle<3.5){        // uStyle 3: glyphs.
      alb=steel*(abs(role)<.5?.36:uBodyK);ks=1.35;sp=88.;float tc=y-Ly*.5,gh=.96,nG=floor((C+.9424778*(uRelK-1.))/1.15+.5);   // glyph count from the field's depth-100% radius, so deepening keeps the same glyphs
      if(abs(role)<.5&&abs(tc)<gh*.5){vec2 g=vec2(u*nG,(tc+gh*.5)/gh),f=fract(g);float id=mod(floor(g.x),nG),st2=1.;vec2 sc=vec2(1.15,gh),P=f*sc,stv=vec2(0.);
        for(int i=0;i<3;i++)for(int j=0;j<4;j++){vec2 a=vec2(.2+.3*float(i),.13+.25*float(j))*sc;
          if(i<2&&hh(id*13.1+float(i*7+j)*1.7+uSeed)<.36){vec2 v=segV(P,a,a+vec2(.3*sc.x,0.));float d2=dot(v,v);if(d2<st2){st2=d2;stv=v;}}
          if(j<3&&hh(id*7.3+float(i*5+j)*2.9+uSeed)<.36){vec2 v=segV(P,a,a+vec2(0.,.25*sc.y));float d2=dot(v,v);if(d2<st2){st2=d2;stv=v;}}
          if(i<2&&j<3&&hh(id*5.9+float(i*3+j)*4.1+uSeed)<.16){vec2 v=segV(P,a,a+vec2(.3*sc.x,.25*sc.y));float d2=dot(v,v);if(d2<st2){st2=d2;stv=v;}}}
        float st=sqrt(st2),ink=1.-smoothstep(.045,.075,st),hS=uGlyph<.5?-1.:(uGlyph<1.5?-1.3:.12);
        hGrad=(st>1e-5?-hS*dss(.045,.075,st)/st:0.)*stv*vec2(nG*sc.x,1.);
        if(uGlyph<.5){alb=mix(alb,uGold*.95,ink);emi+=uGold*ink*.22;}
        else if(uGlyph<1.5){float cw=max(.018,fw*.8);alb=mix(alb,steel*.1,ink);emi+=uIce*(1.-smoothstep(cw*.3,cw,st))*(.018/cw)*.95;}
        else{alb=mix(alb,steel*1.5+vec3(.07),ink);ks=mix(ks,1.7,ink);sp=mix(sp,150.,ink);}}
      else if(topf&&uTick>.5){   // Dial ticks at 1, 5 and 30 degrees.
        float dd=Ly-.42-y,f1=fract(u*360.+.5)-.5,f5=fract(u*72.+.5)-.5,f30=fract(u*12.+.5)-.5,d1=abs(f1)*C/360.,d5=abs(f5)*C/72.,d30=abs(f30)*C/12.,w=.022+fw*.5,ins=step(0.,dd);
        float m1=ins*step(dd,.24)*(1.-smoothstep(.04,.1,fw)),m5=ins*step(dd,.44)*(1.-smoothstep(.06,.16,fw)),m30=ins*step(dd,.8),mr=1.-smoothstep(.08,.2,fw);
        float t1=(1.-smoothstep(w,w+.02,d1))*m1,t5=(1.-smoothstep(w+.006,w+.026,d5))*m5,t30=(1.-smoothstep(w+.012,w+.032,d30))*m30,tr=(1.-smoothstep(.012,.03+fw,abs(dd-1.02)))*mr;
        float ndl=length(vec2(d30,dd-.9)),nd=1.-smoothstep(.06,.08+fw,ndl),tk=t1;
        float k1=-m1*dss(w,w+.02,d1);hGrad=vec2(k1*sign(f1)*C,k1*abs(f1)*dC/360.);
        if(t5>tk){tk=t5;float k5=-m5*dss(w+.006,w+.026,d5);hGrad=vec2(k5*sign(f5)*C,k5*abs(f5)*dC/72.);}
        if(t30>tk){tk=t30;float k30=-m30*dss(w+.012,w+.032,d30);hGrad=vec2(k30*sign(f30)*C,k30*abs(f30)*dC/12.);}
        if(tr>tk){tk=tr;hGrad=vec2(0.,mr*dss(.012,.03+fw,abs(dd-1.02))*sign(dd-1.02));}
        if(nd>tk){tk=nd;float kn=ndl>1e-5?-dss(.06,.08+fw,ndl)/ndl:0.;hGrad=kn*vec2(d30*sign(f30)*C,d30*abs(f30)*dC/12.-(dd-.9));}
        float hS=uGlyph<.5?-1.:(uGlyph<1.5?-1.3:.12);hGrad*=hS;
        if(uGlyph<.5){alb=mix(alb,uGold*.95,tk*.85);emi+=uGold*tk*.14;}
        else if(uGlyph<1.5){alb=mix(alb,steel*.1,tk);emi+=uIce*tk*(.22+.5*nd);}
        else{alb=mix(alb,steel*1.5+vec3(.07),tk);ks=mix(ks,1.7,tk);sp=mix(sp,150.,tk);}}
      else hGrad=vec2(0.,.4*dss(.01,.03,abs(tc))*sign(tc));}
    else{                       // uStyle 4: machining grooves.
      float gp=.14,gi=floor(y/gp),gf=fract(y/gp),depth=.35+.65*hh(gi*7.7+role*3.1+uSeed);
      hGrad=vec2(0.,depth*dss(.0,.35,abs(gf-.5)*2.-.45)*sign(gf-.5)*2./gp);alb=steel*1.1*(.85+.3*hh(gi*1.3+role))*(.9+.2*fbm3(wq*vec3(.25,.25,24.)));ks=.95;sp=64.;}
    float lodK=smoothstep(.25,.75,fw/cell);alb=mix(alb,steel*(uStyle>1.5&&uStyle<3.5?(uStyle>2.5&&abs(role)>.5&&uBodyK>.37?uBodyK:.6):1.),lodK);hGrad*=1.-lodK;emi*=1.-.6*lodK;   // pattern finer than ~2 px fades to its average: no sparkle far away
    if(uGlyph>.5&&uGlyph<1.5&&uStyle>2.5&&uStyle<3.5&&abs(role)<.5&&abs(y-Ly*.5)<.48)emi+=uIce*.07*lodK;
    alb*=dim;}
  hGrad*=uRelK;
  if(uLiner>.5&&(abs(role-3.)<.5||abs(role-1.)<.5&&vRo<0.)){alb=vec3(.016,.018,.022);ks=.1;sp=8.;emi=vec3(0.);}
  vec2 dy=topf?dro:dz,dh=hGrad.x*du+hGrad.y*dy;
  vec3 r1=cross(dpy,n),r2=cross(n,dpx);float det=dot(dpx,r1);
  if(abs(det)>1e-10){vec3 bn=abs(det)*n-sign(det)*(dh.x*r1+dh.y*r2)*.22;float bl=dot(bn,bn);if(bl>1e-20)n=bn*inversesqrt(bl);}
  vec3 Ld=normalize(uStarC-vP);float diff=max(0.,dot(n,Ld)*.8+.2);
  float NV=clamp(abs(dot(n,V)),0.,1.),om=1.-NV,om2=om*om,fres=om2*om,f5=om2*om2*om;
  vec3 key=vec3(1.,.76,.44)*1.15*uLv,fill=vec3(.16,.2,.32)*(.55+.45*n.z)+vec3(.045),H=safeNorm(Ld+V,n);
  vec3 Ls=normalize(vec3(.35,-.45,.82)),sky=vec3(.42,.52,.72)*.75,Hs=safeNorm(Ls+V,n);
  vec3 col;
  if(uLit<.5&&uMat<.5){vec3 env=key*pow(max(0.,dot(reflect(-V,n),Ld)),6.)*ks*.35;
    col=alb*(fill+key*diff+sky*max(0.,dot(n,Ls)))+key*pow(max(0.,dot(n,H)),sp)*ks*.55+sky*pow(max(0.,dot(n,Hs)),sp*.8)*ks*.6+env+vec3(1.,.8,.48)*fres*.16+emi;}
  else{
    float dC=length(uStarC-vP),sa=min(uStarR/dC,.999),al=asin(sa),kK=1.15*uLv;vec3 R=reflect(-V,n);float rA=acos(clamp(dot(R,Ld),-1.,1.));
    if(uLit>.5){float nc=dot(n,Ld),a=sqrt(1.-sa*sa)*nc,b=sa*length(n-nc*Ld);   // the gold rim seen from here is a ring of directions at angle asin(rs/d) round the centre
      diff=a>=b?a:a<=-b?0.:(a*acos(clamp(-a/max(b,1e-5),-1.,1.))+sqrt(max(b*b-a*a,0.)))/3.14159265;kK=uKeyK;key=vec3(1.,.76,.44)*kK;}
    float spS=uMat>1.5?10.:sp,spec=uLit>.5?exp(-sq((rA-al)/mix(.1,.018,clamp((spS-10.)/100.,0.,1.))))*.9:pow(max(0.,dot(n,H)),spS)*.55;
    vec3 amb=uIce*uAmbK*uLit*(.35+.65*(1.-abs(n.z)));
    float side=abs(role-1.)<.5||abs(role-2.)<.5||abs(role-6.)<.5?0.:1.,cav=side*(max(0.,uAB.x-vRo)*step(.3,vRo)+max(0.,vRo+uAB.x)*step(vRo,-.3)),ao=uMat>.5?1.-.6*clamp(cav/.45,0.,1.):1.;   // cav is the recess depth below the face.
    vec3 lit=fill+key*diff+sky*max(0.,dot(n,Ls))+amb,skySp=sky*pow(max(0.,dot(n,Hs)),sp*.8)*ks*.6,rim=vec3(1.,.8,.48)*fres*.16*kK/1.15;
    float lathe=coll&&uColl>.5&&uColl<1.5?1.-.08*(1.-smoothstep(.004,.014,fwS))*cos(s*157.08):1.,knurl=coll&&uColl>.5&&uColl<1.5?1.+.05*(1.-smoothstep(.004,.015,fwS))*(sin(s*104.7+y*104.7)+sin(s*104.7-y*104.7))*.5:1.;
    if(uMat<.5)col=alb*lit+key*spec*ks+skySp+rim+emi;
    else if(uMat<1.5){
      vec3 a2=alb*(1.+.08*(hh(floor(u*uSegN)*7.31+uSeed*3.1)*2.-1.))*(.8+.4*fbm3(vec3(s*.12,y*30.,role*5.+uSeed)));float k2=ks;
      if(abs(role-1.)<.5){a2*=1.2;k2*=1.35;}
      if(coll&&uColl<.5){float lt=.5+.5*cos(s*125.66);a2=mix(steel*.85,uGold,.15)*(.8+.4*mix(lt,.5,smoothstep(.01,.03,fwS)));k2=1.3;}
      a2*=lathe;
      vec3 F0=clamp(a2*1.25+.03,0.,1.),F=F0+(1.-F0)*f5,E=envR(vP,R,uIce)*(uLit>.5?smoothstep(.97,1.03,rA/max(al,1e-3)):1.);
      col=a2*.5*lit*ao+F*(E*ao+key*spec*k2*1.2)+skySp*.8+rim*.6+emi;}
    else if(uMat<2.5){
      vec3 a2=mix(alb*1.1,vec3(dot(alb,vec3(.333))),.2)*(.9+.2*fbm3(vL*9.1+uSeed))*(1.+.08*(hh(floor(vL.x*23.)+floor(vL.y*23.)*7.+floor(vL.z*23.)*13.)-.5)*(1.-smoothstep(.02,.06,fwS)));
      if(coll&&uColl<.5){float kn=.5+.25*(sin(s*48.+y*48.)+sin(s*48.-y*48.));a2=steel*.42*(.9+.2*mix(kn,.5,smoothstep(.01,.04,fwS)));}
      a2*=knurl;
      col=a2*lit*ao+key*spec*ks*.18+sky*pow(max(0.,dot(n,Hs)),10.)*.04+rim*.4+emi;}
    else if(uMat<3.5){   // Grain: 0.02 blocks, +/-3%.
      float g=(hh(floor(vL.x*50.)+floor(vL.y*50.)*7.+floor(vL.z*50.)*13.)-.5)*.06*(1.-smoothstep(.008,.025,fwS))+(fbm3(vL*1.3+uSeed)-.5)*.04;
      vec3 a2=mix(alb*1.08,vec3(dot(alb,vec3(.333))),.15)*(1.+g);
      if(coll&&uColl<.5){float kn=.5+.25*(sin(s*48.+y*48.)+sin(s*48.-y*48.));a2=steel*.42*(.9+.2*mix(kn,.5,smoothstep(.01,.04,fwS)));}
      a2*=knurl;
      vec3 litF=fill+key*(.35+.65*diff)+sky*(.3+.7*max(0.,dot(n,Ls)))+amb;
      float Fc=.04+.96*f5,cs=uLit>.5?exp(-sq((rA-al)/.02))*.9:pow(max(0.,dot(n,H)),140.)*.5;
      col=a2*litF*ao+Fc*(key*cs+envR2(vP,R,uIce,.25,2.)*.6)*ao+rim*.3+emi;}
    else{
      bool blast=uMat>4.5;float fl=1.-smoothstep(.004,.014,fwS),tex=blast?(hh(floor(vL.x*33.)+floor(vL.y*33.)*7.+floor(vL.z*33.)*13.)-.5)*.08*(1.-smoothstep(.008,.025,fwS)):(fbm3(vec3(s*.05,y*40.,role*5.+uSeed))-.5)*.1*fl;
      vec3 a2=alb*(1.+.06*(hh(floor(u*uSegN)*7.31+uSeed*3.1)*2.-1.))*(1.+tex);float k2=ks;
      if(abs(role-1.)<.5){a2*=1.15;k2*=1.3;}
      if(coll&&uColl<.5){float lt=.5+.5*cos(s*125.66);a2=mix(steel*.85,uGold,.15)*(.8+.4*mix(lt,.5,smoothstep(.01,.03,fwS)));k2=1.3;}
      a2*=lathe;
      vec3 Tg=normalize(vT),Bt=safeNorm(cross(n,Tg),vec3(0.,0.,1.));float rough=blast?.55:.25;
      vec3 F0=clamp(a2*.8+.04,0.,.55),F=F0+(1.-F0)*f5,E=(envR2(vP,R,uIce,.45*uBeamR,1.+rough*4.)+vec3(.08,.09,.11)*(blast?1.3:1.)*mix(.5,1.,uLv))*(uLit>.5?smoothstep(.97,1.03,rA/max(al,1e-3)):1.);
      lit=fill+key*(.2+.8*diff)+sky*(.2+.8*max(0.,dot(n,Ls)))+amb;
      float bh=dot(Bt,H),an=blast?pow(max(0.,dot(n,H)),24.)*.6:pow(sqrt(max(0.,1.-bh*bh)),90.)*pow(max(0.,dot(n,H)),4.)*1.1;
      if(uLit>.5)an=exp(-sq((rA-al)/(blast?.12:.05)))*.8;
      col=a2*(blast?.8:.7)*lit*ao+F*(mix(E,vec3(dot(E,vec3(.333))),blast?.3:0.)*ao*(blast?.8:1.15)+key*an*k2)+skySp*(blast?.4:.9)+rim*.6+emi;}}
  if(coll&&uColl>1.5){float ds=abs(fract(u*uSegN)-.5)*C/uSegN,eg=1.-smoothstep(.012,.04+fwS,uCollW*.5-ds);col=mix(col,col*1.35+vec3(.03,.035,.04),eg);}
  if(uBuildA<.999)col+=uIce*(1.-smoothstep(0.,uHoloMode>.5?.3/C:.012,uBuildA-u))*(uHoloMode>.5?3.:2.2);
  if(uGlitch>.01)col=mix(col,col.brg,step(hh(floor(u*64.)+tg*.7+uSeed),.06*uGlitch));
  gl_FragColor=vec4(col*uGlow,1.);}