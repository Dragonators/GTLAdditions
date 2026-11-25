uniform sampler2D uMapD;uniform float uHasTex;uniform vec3 uTintD;uniform vec3 uCam;uniform float uPix;uniform vec3 uStarC;uniform float uStarR;uniform vec3 uLight;uniform float uAmb;uniform float uNeg;uniform float uRimG;uniform vec3 uGold;uniform float uWinK;
varying vec3 vP;varying vec3 vN;varying vec2 vUv;varying vec3 vL;
float hh(float n){return fract(sin(n*127.1+.3)*43758.5453);}
float h31(vec3 p){p=fract(p*vec3(.1031,.1030,.0973));p+=dot(p,p.yxz+33.33);return fract((p.x+p.y)*p.z);}
float vnoise(vec3 p){vec3 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);
  return mix(mix(mix(h31(i),h31(i+vec3(1,0,0)),f.x),mix(h31(i+vec3(0,1,0)),h31(i+vec3(1,1,0)),f.x),f.y),
             mix(mix(h31(i+vec3(0,0,1)),h31(i+vec3(1,0,1)),f.x),mix(h31(i+vec3(0,1,1)),h31(i+vec3(1,1,1)),f.x),f.y),f.z);}
float fbm3(vec3 p){float a=.5,s=0.;for(int i=0;i<3;i++){s+=a*vnoise(p);p=p*2.07+vec3(5.3,1.1,7.7);a*=.5;}return s/.875;}
float sphLight(float nc,float sa){float ca=sqrt(1.-sa*sa),a=ca*nc,b=sa*sqrt(max(0.,1.-nc*nc));return (a>=b?a:a<=-b?0.:(a*acos(clamp(-a/max(b,1e-5),-1.,1.))+sqrt(max(b*b-a*a,0.)))/3.14159265)/ca;}
void main(){
  vec3 alb=(uHasTex>.5?texture2D(uMapD,vUv).rgb:vec3(1.))*uTintD,n=normalize(vN),V=normalize(uCam-vP),L=normalize(uStarC-vP);
  float nl=dot(n,L),mu=max(0.,dot(n,V)),diff=sphLight(nl,min(.92,uStarR/length(uStarC-vP)));
  vec3 col=alb*(uAmb*1.3+vec3(.016,.02,.03)+diff*uLight*1.05)+uLight*pow(max(0.,dot(n,normalize(L+V))),24.)*.07*step(0.,nl);
  col+=vec3(.42,.5,.62)*.35*pow(1.-mu,4.)*smoothstep(0.,.9,-dot(V,L));
  vec3 o=normalize(vL);vec2 G=vec2(atan(o.z,o.x)*64.,asin(clamp(o.y,-1.,1.))*64.),c=floor(G),f=fract(G);float fw=min(1.,max(fwidth(G.x),fwidth(G.y))),lod=1.-smoothstep(.25,.7,fw);
  float on=step(.945,h31(vec3(c,5.))),win=on*(1.-smoothstep(.2,.2+fw,abs(f.x-.5)))*(1.-smoothstep(.16,.16+fw,abs(f.y-.5)));
  col+=vec3(1.,.86,.62)*(win*lod*.9+.05*(1.-lod))*smoothstep(.12,-.15,nl)*uWinK;
  float dish=dot(o,normalize(vec3(.95,.31,0.)));col+=vec3(.42,1.,.55)*smoothstep(.9992,.9999,dish)*.9;
  col=mix(col,vec3(.008,.007,.01)+uGold*.04,uNeg)+uGold*uRimG*(pow(1.-mu,7.)*1.25+smoothstep(.09,0.,mu)*.85);
  gl_FragColor=vec4(col,1.);}