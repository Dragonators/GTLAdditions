uniform vec3 uCol;uniform float uGlow;uniform float uAlpha;uniform float uMode;uniform float uN;uniform float uScroll;uniform float uSweep;uniform float uChase;uniform float uFront;uniform float uDir;uniform vec4 uM[9];uniform float uArm[9];uniform float uStr;uniform vec3 uGeo;varying float vV;varying float vAng;varying float vK;
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
const float PI=3.14159265,TAU=6.2831853;
void main(){
  float near=0.,near2=0.,act=0.,hit=0.,gap=1.,arm=0.;
  for(int i=0;i<9;i++){vec4 m=uM[i];float d=abs(mod(vAng-m.x+PI,TAU)-PI);   // m: azimuth, rising level, gap half-angle, event age (<0 none)
    gap=min(gap,smoothstep(m.z,m.z+.012,d));near=max(near,exp(-d*d/.04));near2=max(near2,exp(-d*d/.08));if(uArm[i]>.001&&d<uArm[i])arm=max(arm,1.-d/uArm[i]);
    if(m.y>.001)act+=m.y*exp(-d*d/.09)*(uFront>0.?.3+.7*pow(fract(d/.16+uChase),3.):.55);
    if(m.w>=0.){float f=d-uFront*m.w;hit+=(f>0.?exp(-f*f/.002):exp(f/.2))*(uFront>0.?1.:0.)*exp(-m.w/.9)+exp(-d*d/.012-m.w/.5);}}
  float sw=uSweep>-50.?pow(.5+.5*cos(3.*(vAng-uSweep)),12.):0.,boost=1.3*act+2.4*hit+1.1*sw;
  float x=(vAng-uScroll)*uN/TAU,fw=fwidth(x),sharp=mix(3.2,16.,vK*vK),I=0.,hot=0.;
  if(uMode<1.5){
    float duty=mix(.38,.74,near),u=(fract(x)-.5)/(duty*.5),s=abs(u),soft=max(.5,fw*2./duty),body=(1.-smoothstep(1.-soft,1.,s))*mix(1.,mix(.3,1.,smoothstep(-1.,.6,u)),uDir);body=mix(body,duty*(1.-.35*uDir),smoothstep(.4,.9,fw));
    float wk=mix(.4,1.,1.-min(1.,s*s)),v=abs(vV)/wk,c=exp(-v*v*sharp),h=exp(-vV*vV*2.6);
    I=(c*.95+h*.2)*body*mix(.5,1.,near)+exp(-vV*vV*min(46.,sharp*3.))*.1;hot=c*body;
  }else if(uMode<2.5){
    float fv=max(fwidth(vV),1e-5),edge=1.-smoothstep(.012,.012+fv*1.2,abs(abs(vV)-.95));
    float x2=x*2.,f2=max(fw*2.,1e-5),q=mod(floor(x2+.5),6.),th=q<.5?.42:(q<3.5&&q>2.5?-.12:-.52),dt=abs(fract(x2+.5)-.5),tk=(1.-smoothstep(.028,.028+f2,dt))*(1.-smoothstep(th,th+fv,vV));tk=mix(tk,.056*(th+.95)/1.9,smoothstep(.2,.5,f2));
    float u=(fract(x)-.5)/.27,s=abs(u),row=(1.-smoothstep(.45,1.,s))*mix(1.,mix(.3,1.,smoothstep(-1.,.6,u)),uDir)*(1.-smoothstep(.022,.022+fv,abs(vV-.68)));row=mix(row,.27*.05,smoothstep(.3,.7,fw));
    I=edge*.6+tk*.62+row*.72+.028*(1.-vV*.6);hot=max(edge,max(tk,row))*.5;
  }else if(uMode<3.5){
    float c=exp(-vV*vV*sharp*1.6);I=c*.62+exp(-vV*vV*3.)*.09;hot=c;
  }else{
    float ab=abs(vV),prof=exp(-pow(ab/.46,2.6)),halo=exp(-vV*vV*3.2);
    if(uMode<4.5){
      float s=abs(fract(x)-.5)/.31,body=1.-smoothstep(.55,1.,s);body=mix(body,.6,smoothstep(.4,.9,fw));I=(prof*.85+halo*.2)*body;hot=prof*body*.55;
    }else if(uMode<5.5){
      float u=fract(x)/.84;if(u<1.){float wk=max(.04,pow(u,.7)*.62*sqrt(max(0.,1.-pow(u,6.)))),v=ab/wk,c=exp(-v*v*1.5);I=c*(.18+.82*u*u)+halo*.14*u*u*u*(1.-u);hot=c*u*u*.6;}
    }else if(uMode<7.5){   // (vV+1)/2 is height above the ring.
      float up=(vV+1.)*.5,s=abs(fract(x)-.5)/.33,body=1.-smoothstep(.6,1.,s),fv=fwidth(up);I=body*(pow(max(0.,1.-up),2.4)*.62+exp(-up*up/max(.0016,fv*fv*2.))*.7);hot=body*exp(-up*up/.003)*.5;
    }else if(uMode<8.5){
      if(arm>0.){float wk=mix(.5,1.,pow(arm,.5)),c=exp(-pow(ab/(.46*wk),2.6));I=c*(.3+.7*pow(arm,.4))*smoothstep(0.,.14,arm)+halo*.18*arm;hot=c*arm*.55;}
    }else if(uMode<9.5){
      float ang=vAng-uScroll,R=uGeo.x,ca=cos(ang),sa=sin(ang),n=fbm3(vec3(ca*R*.1,sa*R*.1,vV*2.2+uScroll*2.)),m2=fbm3(vec3(ca*R*.34,sa*R*.34,vV*4.4-uScroll*3.)),kn=pow(.5+.5*cos(3.14159265*x),2.),ed=ab*(1.+.8*(n-.5)),pr=exp(-ed*ed*(5.-1.6*kn));
      I=pr*(.34+.66*kn)*(.4+.85*n)*(.72+.45*m2);hot=exp(-ed*ed*14.)*kn*.3;
    }else if(uMode<10.5){
      float P=3.14159265*uGeo.x/uN,sb=(fract(x*2.)-.5)*P,d=abs(sb-(.95-ab*uGeo.z*1.1)),c=exp(-d*d/.09)*(1.-smoothstep(.72,1.,ab));I=c+halo*.05;hot=c*.4;
    }else if(uMode<11.5){
      float fT=smoothstep(.12,.4,fw);for(float k=0.;k<uStr-.5;k+=1.){float q=TAU*(x+k/uStr),vk=.58*sin(q),fr=.5+.5*cos(q),sg=.16+.1*fr,e=(vV-vk)/sg;I+=exp(-e*e)*(.22+.78*fr);hot+=exp(-e*e*2.)*fr*.5;}
      I=mix(I,.5*exp(-vV*vV*2.2),fT)+halo*.1;hot*=1.-fT;
    }else if(uMode<12.5){
      float t=fract(x),saw=t*t*(1.-smoothstep(.84,1.,t));I=prof*(.12+.95*saw)+halo*(.08+.22*saw);hot=prof*saw*.6;
    }else if(uMode<13.5){
      float duty=mix(.34,.76,near2),s=abs(fract(x)-.5)/(duty*.5),body=1.-smoothstep(.62,1.,s),wk=mix(.62,1.,near2),c=exp(-pow(ab/(.46*wk),2.6));I=(c*.9+halo*.18*near2)*body*mix(.68,1.,near2);hot=c*body*.5;
    }else if(uMode<14.5){
      float t=fract(x),l=1.-smoothstep(.6,1.,abs(t-.5)/.27),P=TAU*uGeo.x/uN,sb=min(t,1.-t)*P,vb=ab*uGeo.z,dt=exp(-(sb*sb+vb*vb)/.2);I=(prof*.85+halo*.18)*l+dt*.95+exp(-(sb*sb+vb*vb)/.8)*.14;hot=prof*l*.5+dt*.5;
    }else if(uMode<15.5){
      float s=abs(fract(x)-.5)/.31,body=1.-smoothstep(.55,1.,s),base=exp(-pow(ab/.4,2.6));I=(prof*.85+halo*.2)*body+base*.17+halo*.05;hot=prof*body*.5;
    }else if(uMode<16.5){
      I=prof*(.5+.42*near)+halo*.16;hot=prof*(.2+.3*near);
    }else{
      float fv=fwidth(vV),d4=(ab-.44)/.135,l=exp(-d4*d4),f=(1.-smoothstep(.44,.54,ab))*.16;I=l*.9+f+halo*.05;I=mix(I,exp(-vV*vV*3.)*.55,smoothstep(.1,.3,fv));hot=l*.45*(1.-smoothstep(.1,.3,fv));
    }
  }
  float gn=uMode>3.5?1.+.6*boost:1.+boost,a=uAlpha*vK*I*gn*gap*uGlow;gl_FragColor=vec4(mix(uCol,vec3(1.),hot*min(.85,uMode>3.5?.1+.38*boost:.28+.5*boost))*a,a);}