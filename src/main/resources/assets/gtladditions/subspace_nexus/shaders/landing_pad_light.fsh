uniform vec2 uC;uniform vec2 uRot;uniform vec3 uCol;uniform vec3 uHot;uniform float uA;uniform float uB;uniform float uR1;uniform float uMode;uniform float uGlow;
varying vec3 vP;varying vec3 vN;varying float vC;
float arc(float u,float n,float gap,float rot,float soft){float s=fract(u*n+rot);return smoothstep(0.,soft,s)*(1.-smoothstep(1.-gap-soft,1.-gap,s));}
float hair(float x,float w,float px){float e=max(w,px*.9);return exp(-x*x/(e*e))*pow(w/e,.7);}
float sq(float x){return x*x;}
void main(){
  vec2 q=vP.xy-uC;
  float r=length(q),px=max(fwidth(r),1e-4),u=fract((r>1e-6?atan(q.y,q.x):0.)/6.2831853+1.);
  float x=clamp(uB,0.,1.),on=step(0.,uB)*step(uB,1.);
  float W=on*smoothstep(0.,.08,x)*(1.-smoothstep(.18,1.,x)),F=on*smoothstep(0.,.05,x)*pow(1.-x,3.);
  float deck=1.-smoothstep(28.3,28.9,vP.z),spill=sq(1.-smoothstep(9.,15.5,r));
  vec3 ice=mix(uCol,uHot,.55),c=vec3(0.);
  if(uMode<4.5){
    float a1=arc(u,6.,.12,uRot.x,.07),a2=arc(u,3.,.2,uRot.y,.05),d1=r-8.4,d2=r-3.1;
    float l1=exp(-d1*d1/.2),w1=exp(-d1*d1/2.2),l2=exp(-d2*d2/.07),w2=exp(-d2*d2/.7);
    float up=mix(.08,1.,smoothstep(.3,.9,vN.z)),ring=(a1*(l1+w1*.2)+.6*a2*(l2+w2*.16))*up;
    if(uMode<1.5){
      c=mix(uCol,uHot,.9*W)*ring*(uA*1.7+1.6*W);
      c+=ice*(exp(-r*r/40.)*.13+spill*deck*.24)*F;
    }else if(uMode<2.5){
      c=ice*(exp(-r*r/40.)*.1+spill*deck*.2)*F;
    }else{
      if(uMode<3.5)c=uCol*ring*uA*1.7;
      c+=mix(ice,uHot,.6)*vC*(.2+.16*clamp(vN.z,0.,1.))*W;
      c+=ice*(1.-vC)*(exp(-r*r/70.)*.1+spill*deck*.2)*F;
    }
    c*=mix(.74,1.,clamp(vN.z,0.,1.));
  }else{
    float a1=arc(u,6.,.12,uRot.x,.015),a2=arc(u,3.,.2,uRot.y,.012),w=.06;
    float h1=hair(r-7.76,w,px)+hair(r-8.24,w,px),fill=(1.-smoothstep(.16,.26,abs(r-8.)))*.07,h2=hair(r-3.02,w*.8,px)+hair(r-3.18,w*.8,px);
    float ring=a1*(h1+fill)+.6*a2*h2,rp=mix(8.,uR1,1.-pow(1.-x,3.));
    c=mix(uCol,uHot,.9*W)*ring*(uA*1.7+1.6*W);
    c+=ice*exp(-sq((r-8.)/1.1))*a1*W*.07;
    c+=uHot*hair(r-rp,.07,px)*on*smoothstep(0.,.05,x)*sq(1.-x)*1.4;
  }
  if(dot(c,c)<1e-6)discard;
  gl_FragColor=vec4(c*uGlow,1.);}