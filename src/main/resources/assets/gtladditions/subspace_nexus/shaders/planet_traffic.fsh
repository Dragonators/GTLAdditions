uniform vec3 uCam;uniform float uPix;uniform float uLinePhase;uniform float uCloudPhase[3];uniform vec3 uStarC;uniform vec3 uLight;uniform float uAmb;uniform float uNeg;uniform float uRimG;uniform vec3 uGold;uniform vec3 uTech;uniform vec3 uPC;uniform float uPR;
uniform float uW;uniform float uSeed;uniform float uStyle;uniform float uSegN;uniform vec3 uSteel;uniform float uLineK;uniform float uHalfB;uniform float uGlyphH;
varying vec3 vP;varying vec3 vN;varying float vU;varying float vY;varying float vRole;varying float vRad;
float hh(float n){return fract(sin(n*127.1+.3)*43758.5453);}
float h31(vec3 p){p=fract((p-1024.*floor((p+512.)/1024.))*vec3(.1031,.1030,.0973));p+=dot(p,p.yxz+33.33);return fract((p.x+p.y)*p.z);}
float vnoise(vec3 p){vec3 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);
  return mix(mix(mix(h31(i),h31(i+vec3(1,0,0)),f.x),mix(h31(i+vec3(0,1,0)),h31(i+vec3(1,1,0)),f.x),f.y),
             mix(mix(h31(i+vec3(0,0,1)),h31(i+vec3(1,0,1)),f.x),mix(h31(i+vec3(0,1,1)),h31(i+vec3(1,1,1)),f.x),f.y),f.z);}
float fbm3(vec3 p,bool moving){float a=.5,s=0.;for(int i=0;i<3;i++){s+=a*vnoise(p+vec3(moving?uCloudPhase[i]:0.,0,0));p=p*2.07+vec3(5.3,1.1,7.7);a*=.5;}return s/.875;}
float sgm(vec2 p,vec2 a,vec2 b){vec2 pa=p-a,ba=b-a;return length(pa-ba*clamp(dot(pa,ba)/dot(ba,ba),0.,1.));}
void main(){
  vec3 n=normalize(vN),V=normalize(uCam-vP);if(dot(n,V)<0.)n=-n;
  vec3 L=normalize(uStarC-vP);float role=floor(vRole+.5),u=vU,C=6.2831853*vRad*uW,px=max(uPix*length(uCam-vP),1e-5),y=vY*uW;
  bool outer=role<.5,inner=abs(role-3.)<.5,side=abs(role-2.)<.5,joint=abs(role-6.)<.5;
  vec3 alb=uSteel,emi=vec3(0.);float ks=1.;
  float g=u*uSegN,cell=mod(floor(g),uSegN),f=fract(g),sd=min(f,1.-f)*C/uSegN,var=hh(cell*7.31+uSeed*3.1),pl=smoothstep(2.5,6.,(C/uSegN)/px);
  alb*=1.+.14*(var-.5)*pl;alb*=1.-.55*(1.-smoothstep(.004,.012+px*.7,sd))*pl;
  if(joint){alb=uSteel*.68;ks=1.25;}
  if(abs(role-1.)<.5){alb=uSteel*1.18;ks=1.3;}
  if(outer&&uLineK>0.){float lw=max(.005,.5*px),la=min(1.,.005/lw),ll=1.-smoothstep(lw*.5,lw+px*.5,abs(y));
    emi+=uTech*ll*la*uLineK*(.45+.55*pow(.5+.5*sin(u*18.85-uLinePhase+uSeed),4.));}
  if((inner||joint)&&uStyle<2.5){float g3=u*uSegN*5.,c3=mod(floor(g3),uSegN*5.),f3=fract(g3),wid=(C/uSegN)/5.,on=step(.55,hh(c3*3.17+uSeed)),lod=1.-smoothstep(.006,.016,px),wy=1.-smoothstep(.004,.004+px,abs(y)),wx=1.-smoothstep(.0055,.0055+px,abs(f3-.5)*wid);
    emi+=vec3(1.,.82,.6)*on*wy*wx*lod*.85+vec3(1.,.8,.55)*.03*(1.-lod)*step(abs(y),.012);}
  if(uStyle>1.5&&uStyle<2.5&&outer){
    float gh=uGlyphH,gs=gh*1.2,nG=floor(C/gs+.5),lod=smoothstep(3.,8.,gs/px);
    if(abs(y)<gh*.5&&lod>0.){vec2 G=vec2(u*nG,(y+gh*.5)/gh),F=fract(G),sc=vec2(gs,gh),P=F*sc;float id=mod(floor(G.x),nG),st=1.;
      for(int i=0;i<3;i++)for(int j=0;j<4;j++){vec2 a=vec2(.2+.3*float(i),.13+.25*float(j))*sc;
        if(i<2&&hh(id*13.1+float(i*7+j)*1.7+uSeed)<.36)st=min(st,sgm(P,a,a+vec2(.3*sc.x,0.)));
        if(j<3&&hh(id*7.3+float(i*5+j)*2.9+uSeed)<.36)st=min(st,sgm(P,a,a+vec2(0.,.25*sc.y)));}
      float cw=max(gh*.03,px*.8);alb=mix(alb,uSteel*.2,lod*(1.-smoothstep(gh*.03,gh*.1,st)));emi+=uTech*(1.-smoothstep(cw*.3,cw,st))*min(1.,gh*.03/cw)*lod*1.1;}
    emi+=uTech*.06*(1.-lod)*(1.-smoothstep(gh*.5,gh*.5+px,abs(y)));}
  float nl=dot(n,L);
  if(uStyle>2.5&&inner){
    float th=u*6.2831853,yy=vY/uHalfB;vec3 q=vec3(cos(th)*5.,sin(th)*5.,yy*1.6)+uSeed;float h=fbm3(q,false),land=smoothstep(.47,.53,h);
    alb=mix(vec3(.05,.18,.36),mix(vec3(.19,.32,.15),vec3(.5,.46,.34),smoothstep(.53,.72,h)),land);float cl=smoothstep(.55,.72,fbm3(q*2.3,true));alb=mix(alb,vec3(.86,.9,.94),cl*.6);
    float edge=smoothstep(.8,.9,abs(yy));alb=mix(alb,uSteel*.5,edge);ks=.12+.8*edge;
    vec2 gg=vec2(u*1100.,yy*18.);float lod=1.-smoothstep(.006,.016,px),cc=step(.78,h31(vec3(floor(gg),3.)))*(1.-smoothstep(.2,.48,length(fract(gg)-.5)))*mix(.25,1.,land)*(1.-.7*cl);emi+=vec3(1.,.8,.55)*(1.0 - smoothstep(-.2,.1,nl))*(cc*lod+.09*mix(.25,1.,land)*(1.-lod))*(1.-edge);}
  vec3 oc=vP-uPC;float b=dot(oc,L),dc=sqrt(max(0.,dot(oc,oc)-b*b)),sh=b<0.?1.-smoothstep(uPR*.9,uPR*1.08,dc):0.;
  float diff=max(0.,nl*.85+.15)*(1.-sh),NV=clamp(abs(dot(n,V)),0.,1.),fm=1.-NV,fm2=fm*fm;
  vec3 key=uLight*1.05,fill=vec3(.05,.065,.1)*(.6+.4*n.z)+uAmb*.25,R=reflect(-V,n);float rA=acos(clamp(dot(R,L),-1.,1.)),hl=exp(-rA*rA/.22)*(1.-sh);
  vec3 F0=clamp(alb*.8+.04,0.,.6),F=F0+(1.-F0)*fm2*fm2*fm,env=mix(vec3(.008,.01,.018),vec3(.05,.07,.12),smoothstep(-.2,.8,R.z));
  vec3 col=alb*.62*(fill+key*diff)+F*(env+key*hl*.9*ks)*min(ks,1.)+emi;
  col=mix(col,vec3(.012,.011,.015)+emi*.5,uNeg);col+=uGold*uRimG*fm2*fm*.6;
  gl_FragColor=vec4(col,1.);}