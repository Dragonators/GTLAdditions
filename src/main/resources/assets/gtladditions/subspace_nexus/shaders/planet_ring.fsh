uniform vec3 uStarC;uniform vec3 uPlanetC;uniform float uPlanetR;uniform vec2 uR;uniform vec3 uC1;uniform vec3 uC2;uniform vec3 uLight;
uniform float uAlpha;uniform float uSeed;uniform float uNeg;uniform float uRimG;uniform vec3 uGold;uniform float uDust;uniform float uGlow;
uniform sampler2D uRTex;uniform float uRTexK;uniform vec3 uRTint;varying vec3 vP;varying vec2 vXY;
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
void main(){float rr=length(vXY),x=(rr-uR.x)/(uR.y-uR.x);if(x<0.||x>1.)discard;
  float d=mix((.55+.45*sin(x*41.+uSeed*7.))*smoothstep(.0,.05,abs(x-.63))*(.2+.8*smoothstep(.0,.025,abs(x-.3))),.35+.5*fbm3(vec3(x*9.,uSeed,0.)),uDust);
  vec3 rc=mix(uC1,uC2,x);if(uRTexK>.5){vec4 rt=textureLod(uRTex,vec2(x,.5),0.);d=rt.a*1.15;rc=rt.rgb*uRTint;}
  vec3 L=normalize(uStarC-vP),oc=vP-uPlanetC;float b=dot(oc,L),c=dot(oc,oc)-uPlanetR*uPlanetR,sh=(b<0.&&b*b-c>0.)?1.:0.;
  vec3 col=rc*uLight*(.32+.68*(1.-sh*.88));
  float a=uAlpha*d*smoothstep(0.,.04,x)*(1.0 - smoothstep(.96,1.,x));
  col=mix(col,vec3(.02),uNeg)+uGold*uRimG*.55*d;
  gl_FragColor=vec4(col*a*uGlow,a);}