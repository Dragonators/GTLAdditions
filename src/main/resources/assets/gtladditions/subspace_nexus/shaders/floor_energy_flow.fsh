uniform vec2 uAx;uniform vec3 uCol;uniform vec3 uHot;uniform float uA;uniform float uL;uniform float uMode;uniform vec2 uR;uniform vec3 uPk;uniform float uPool;uniform float uGlow;uniform float uFlowPhase;
varying vec3 vP;varying vec3 vN;
void main(){
  float u=dot(vP.xy,uAx),v=dot(vP.xy,vec2(-uAx.y,uAx.x)),s=u+vP.z,ab=abs(v),fv=fwidth(v);
  float en=smoothstep(uR.x,uR.x+3.,u)*(1.-smoothstep(uR.y-1.5,uR.y+1.,u));
  vec2 dq=vec2(u-uR.y,v);float pool=exp(-dot(dq,dq)/5.)+1.5*exp(-dot(dq,dq)/.8);   // uR.y is the window's centre and projection origin.
  if(en<=.001&&pool<.004)discard;
  float x=fract(s/uL-uFlowPhase),pu=x*x*x*(1.-smoothstep(.9,1.,x));
  float line,wash;
  if(uMode<1.5){line=exp(-v*v/.5);wash=exp(-v*v/3.2);}
  else if(uMode<2.5){line=exp(-pow((ab-.62)/.17,2.));wash=(1.-smoothstep(.55,.8,ab))*.22+exp(-v*v/3.)*.45;}
  else{line=exp(-v*v/.035);wash=exp(-v*v/2.4);}
  float far=smoothstep(.12,.42,fv);
  line=mix(line,exp(-v*v/1.1)*.6,far);
  float I=(line*.95+wash*.32)*(.3+pu*1.15);
  float dh=uPk.x-s,tail=dh>0.?exp(-dh/9.):exp(dh/.7),P=uPk.y*tail*(line*1.05+wash*.42);
  float face=mix(.74,1.,clamp(vN.z,0.,1.));
  vec3 c=uCol*I*uA+mix(uCol,uHot,.75)*P;
  c=mix(c,c+uHot*uA*line*pu*.25,1.-far);
  gl_FragColor=vec4((c*face*en+mix(uCol,uHot,.5)*pool*uA*(.85+2.4*uPool))*uGlow,1.);}