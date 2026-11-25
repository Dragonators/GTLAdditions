uniform float uForm;uniform float uZa;uniform float uR;uniform vec3 uBody;uniform vec3 uLight;uniform vec3 uMark;uniform vec3 uVeinCol;uniform float uVein;
uniform vec3 uRim;uniform float uRimGain;uniform float uRimPow;uniform vec2 uAxis;uniform vec2 uProjZ;
uniform float uAmb;uniform float uOn;uniform float uSweepZ;uniform vec4 uShip;uniform vec4 uPulse;uniform vec2 uBurst;uniform float uTopZ;uniform float uSky;
uniform vec3 uSkyCol;uniform float uFog;uniform vec2 uE1;uniform vec4 uP1;uniform vec4 uP2;uniform vec4 uP3;uniform float uSpec;uniform float uTime;uniform float uGlow;uniform vec3 uCam;
varying float vZ;varying float vS;varying float vCov;varying vec2 vSide;varying vec2 vView;varying vec3 vP;varying float vPx;
float h11(float x){return fract(sin(x*127.1)*43758.5453);}
float vn2(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);float k=i.x+i.y*57.;return mix(mix(h11(k),h11(k+1.),f.x),mix(h11(k+57.),h11(k+58.),f.x),f.y);}

void main(){
  float cov=min(vCov,1.),det=smoothstep(.3,.8,vCov),R=uR,y0=vS*R,fz=fwidth(vZ);
  vec2 e1=uE1,e2=vec2(-uE1.y,uE1.x);vec3 L=normalize(vec3(.35,-.45,.82));
  float hz=max(0.,vZ-uTopZ)/uSky,skyK=pow(max(0.,1.-hz),1.3),lit=uOn*step(vZ,uSweepZ);
  float c=sqrt(max(0.,1.-vS*vS));vec2 nw=normalize(vS*vSide+c*vView);vec3 n=vec3(nw,0.);
  float xh=c*R,ao=1.,far=1.-det,hi=0.,kk=0.,blK=1.,ridge=.5;vec3 Wd=vec3(0.,0.,1.),body=uBody,em=vec3(0.),own=vec3(0.),ownAvg=vec3(0.);float specK=uSpec;
  float th=atan(dot(nw,e2),dot(nw,e1));
  if(uForm<1.5){
    body*=mix(1.,.86+.14*sin(th*22.),det);
    float g=(vZ-uZa)/uP1.x,w=uP1.y/uP1.x,mk=step(fract(g),w);mk=mix(mk,w,clamp(fwidth(g)*2.,0.,1.));
    body=mix(body,uMark,mk*.85);own=uMark*mk*.12*lit+uLight*uP1.z*lit*(.55+.45*c);ownAvg=own;
    float bz=(vZ-uZa)/48.,he=max(.45,.8*fz),bb=exp(-pow((fract(bz)-.5)*48./he,2.))*.45/he;
    em+=uLight*bb*step(fract(uTime*.45-floor(bz)*.09),.16)*lit*2.4;
  }else if(uForm<2.5){
    int ns=int(uP3.x+.5);float rs=uP1.x*R,dd=uP1.y*R,lay=uP1.z,rc=uP1.w*R,Ro=dd+rs,ph0=-6.2831853*vZ/lay,best=-1e9,bphi=0.;vec2 bc=vec2(0.);int bk=-1;
    for(int k=0;k<6;k++){if(k>=ns)break;float phi=float(k)*6.2831853/float(ns)+ph0;vec2 cw=dd*(cos(phi)*e1+sin(phi)*e2),cv=vec2(dot(cw,vView),dot(cw,vSide));float dy=y0-cv.y;
      if(abs(dy)<rs){float x=cv.x+sqrt(rs*rs-dy*dy);if(x>best){best=x;bc=cv;bk=k;bphi=phi;}}}
    bool core=false;if(abs(y0)<rc){float x=sqrt(rc*rc-y0*y0);if(x>best){best=x;bc=vec2(0.);bk=9;core=true;}}
    if(bk<0&&det>.7)discard;
    float depth=0.;
    if(bk>=0){vec2 hp=vec2(best,y0);xh=best;depth=clamp((Ro-length(hp))/(Ro-rc),0.,1.);
      vec2 n2=(hp-bc)/(core?rc:rs),nw2=normalize(n2.x*vView+n2.y*vSide);
      if(!core){vec2 t2=-sin(bphi)*e1+cos(bphi)*e2,ur=cos(bphi)*e1+sin(bphi)*e2;vec3 T=normalize(vec3(-t2*(dd*6.2831853/lay),1.));
        n=normalize(vec3(nw2,0.)-T*dot(vec3(nw2,0.),T));
        float wq=uP2.x*atan(dot(nw2,t2),dot(nw2,ur))/6.2831853+vZ/uP2.y,aaw=clamp(fwidth(wq)*1.5,0.,1.);
        ridge=mix(.5+.5*cos(6.2831853*wq),.5,aaw);vec3 Bt=normalize(cross(T,n));
        n=normalize(n+Bt*sin(6.2831853*wq)*uP3.w*(1.-aaw));Wd=normalize(T*cos(uP2.z)+Bt*sin(uP2.z));
      }else n=vec3(nw2,0.);
      ao=1.-.68*pow(depth,1.15);
    }
    far=max(max(far,clamp(fz*3.*float(ns)/lay,0.,1.)),1.-smoothstep(3.,10.,vPx));
    body*=.8+.2*ridge;blK=.25;
    float flowR=.55+.45*(.5+.5*sin((vZ-uTime*uP2.w)*.42));
    own=uLight*lit*flowR*(core?1.7:uP3.z*smoothstep(uP3.y,uP3.y+.3,depth));ownAvg=uLight*lit*flowR*.45;
  }else if(uForm>4.5){
    float nc=uP1.x,lay=uP1.y,cw=uP1.z,dep=uP1.w,tex=uP2.x,ts=uP2.y,rel=uP2.z,Ri=R-dep,bev=uP3.x;
    float aaz=clamp(fwidth(vZ/ts)*2.,0.,1.),frz=fract(vZ/ts),Rz=tex<.5?R-.012*rel*(1.-smoothstep(.012,.03,min(frz,1.-frz)*ts))*(1.-aaz):R;
    if(abs(y0)>Rz&&det>.7)discard;
    float xo=sqrt(max(Rz*Rz-y0*y0,0.));vec2 ho=xo*vView+y0*vSide;float tho=atan(dot(ho,e2),dot(ho,e1));
    float co=nc*(tho/6.2831853+vZ/lay),ao0=abs(fract(co)-.5)*6.2831853*R/nc;      // arc distance from the channel centre line
    vec2 tg=-sin(tho)*e1+cos(tho)*e2;vec3 tg3=vec3(tg,0.),rad3=vec3(ho/max(length(ho),1e-4),0.);
    float aac=clamp(fwidth(co)*6.2831853*R/nc/cw*1.5,0.,1.);                       // channel narrower than ~1.5 px: average it
    float flowE=.55+.45*(.5+.5*sin((vZ-uTime*uP2.w)*.42-co*.8));
    xh=xo;n=rad3;
    if(ao0<cw*.5&&aac<.95){
      float xi=-1.,wallT=1.;bool flo=false;
      if(abs(y0)<Ri){xi=sqrt(Ri*Ri-y0*y0);vec2 hi2=xi*vView+y0*vSide;float thi=atan(dot(hi2,e2),dot(hi2,e1)),ci=nc*(thi/6.2831853+vZ/lay),ai=abs(fract(ci)-.5)*6.2831853*Ri/nc;
        if(ai<cw*.5){flo=true;xh=xi;n=vec3(hi2/Ri,0.);}
        else{wallT=clamp((cw*.5-ao0)/max(ai-ao0,1e-4),0.,1.);xh=mix(xo,xi,wallT);float sg=fract(ci)>.5?-1.:1.;n=normalize(sg*tg3+rad3*.15);}
      }else{if(det>.7)discard;wallT=.5;n=tg3*(fract(co)>.5?-1.:1.);}
      if(flo){body*=.35;own=uLight*lit*flowE*uP3.w;}
      else{body*=.45;own=uLight*lit*flowE*uP3.z*pow(wallT,1.4);}
      ao=flo?1.:.75;specK*=.3;
    }else{
      float a=tho*R,du=0.;vec3 bump=vec3(0.);float tao=1.;
      if(tex<.5){float fr=fract(vZ/ts),dg=min(fr,1.-fr)*ts,gr=1.-smoothstep(.012,.03,dg),ed=(1.-smoothstep(.03,.06,dg))*(1.-gr);
        bump=vec3(0.,0.,fr<.5?1.:-1.)*ed*.8*rel*(1.-aaz);tao=mix(1.-.65*gr,.92,aaz);}
      else if(tex<1.5){float row=floor(vZ/ts),ap=a+mod(row,2.)*3.14159265*R/6.,fz2=fract(vZ/ts),fa=fract(ap/(6.2831853*R/6.));
        float dz=min(fz2,1.-fz2)*ts,da=min(fa,1.-fa)*6.2831853*R/6.,d=min(dz,da),aap=clamp(fwidth(vZ/ts)*8.,0.,1.);
        float bv=(1.-smoothstep(.03,.11,d))*(1.-aap);
        bump=(dz<da?vec3(0.,0.,fz2<.5?1.:-1.):tg3*(fa<.5?1.:-1.))*bv*.7*rel;
        tao=mix(1.-.7*(1.-smoothstep(.022,.045,d)),.9,aap);body*=1.+.06*(h11(row*7.3+floor(ap/(6.2831853*R/6.)))-.5)*(1.-aap);}
      else{float bw=ts,u=(a+vZ)/bw,v=(a-vZ)/bw,pA=sin(3.14159265*fract(u)),pB=sin(3.14159265*fract(v)),chk=mod(floor(u)+floor(v),2.);
        float hA=pA*mix(.55,1.,chk),hB=pB*mix(1.,.55,chk),aab=clamp(max(fwidth(u),fwidth(v))*1.5,0.,1.);
        float g=3.14159265/bw;vec2 dh=hA>hB?vec2(g*cos(3.14159265*fract(u))*mix(.55,1.,chk)):vec2(1.,-1.)*g*cos(3.14159265*fract(v))*mix(1.,.55,chk);
        bump=-(tg3*dh.x+vec3(0.,0.,1.)*dh.y)*.06*rel*(1.-aab);tao=mix(.55+.45*max(hA,hB),.8,aab);}
      float lip=1.-smoothstep(cw*.5,cw*.5+bev,ao0);
      vec3 toCh=tg3*(fract(co)>.5?-1.:1.);
      n=normalize(rad3+bump-toCh*lip*.9*(1.-aac));ao=tao;
      own=uLight*lit*flowE*(uP3.y*lip*(1.-aac)+.23*aac);                     // channels too thin to resolve: their average light only
    }
    far=max(far,1.-smoothstep(3.,10.,vPx));
    ownAvg=uLight*lit*flowE*.4;
  }else{
    float g=(vZ-uZa)/uP1.x,fg=fract(g),jt=step(uP1.y,fg),aa=clamp(fwidth(g)*2.,0.,1.);
    if(jt>.5&&abs(vS)>uP1.z&&det>.9)discard;
    float jc=(fg-(1.+uP1.y)*.5)/(1.-uP1.y)*2.,ring=jt*exp(-jc*jc*9.),edge=min(fg,uP1.y-fg)/uP1.y;
    body*=mix(jt>.5?.25:.72+.28*smoothstep(0.,.07,edge),.6,aa);
    float wave=.5+.5*sin(floor(g)*.6-uTime*uP1.w),near=exp(-pow((vZ-uShip.x)/9.,2.))*uShip.y;
    own=uLight*mix(ring,.3,aa)*lit*(.45+.55*wave+1.6*near)*1.3;ownAvg=own;
  }
  vec3 Ph=vec3(uAxis+xh*vView+y0*vSide,vZ),V=normalize(uCam-Ph),H=normalize(L+V);
  float dif=uForm>4.5?.42+.58*max(dot(n,normalize(vec3(.35,-.45,.35))),0.):.42+.58*max(dot(n.xy,normalize(L.xy)),0.),spec=pow(max(dot(reflect(-L,n),V),0.),28.);
  if(uForm>1.5&&uForm<2.5){float tw=dot(Wd,H);kk=pow(sqrt(max(0.,1.-tw*tw)),36.)*(.35+.65*ridge);}
  vec3 col=body*dif*ao*uAmb+vec3(spec*specK*blK*uAmb)+vec3(kk*specK*.42*ao)*(.25+.75*uAmb)+mix(vec3(1.),uBody*2.,.4)*hi*(.25+.75*uAmb);
  col=mix(col,uBody*(.42+.1*specK)*uAmb,far);
  em+=mix(own,ownAvg,far)+uRim*pow(1.-abs(dot(n,V)),uRimPow)*uRimGain*mix(ao,.5,far);
  em+=uLight*uShip.y*exp(-pow((vZ-uShip.x)/uShip.z,2.))*1.6;
  float pd=(uPulse.x-vZ)/uPulse.y;if(pd>0.&&pd<1.)em+=mix(uLight,vec3(1.),.5)*(1.-pd)*(1.-pd)*uPulse.z*2.;
  float bd=(uBurst.x-vZ)/40.;if(bd>0.&&bd<1.)em+=vec3(1.)*(1.-bd)*uBurst.y*1.5;
  col=mix(col,uSkyCol,uFog*pow(hz,.7));em*=skyK*uGlow;
  float a=cov*smoothstep(uTopZ+uSky,uTopZ+uSky-60.,vZ);
  vec4 vp=viewMatrix*vec4(Ph,1.);gl_FragDepth=det>.5?(uProjZ.x*vp.z+uProjZ.y)/(-vp.z)*.5+.5:gl_FragCoord.z;
  gl_FragColor=vec4(col+em,a);}