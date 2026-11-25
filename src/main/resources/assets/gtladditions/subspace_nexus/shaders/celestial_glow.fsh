uniform vec3 uColor;uniform float uAlpha;uniform float uMode;uniform float uRing;uniform float uWidth;uniform float uTime;uniform float uGlow;varying vec2 vUv;
vec3 hsv(float h,float s,float v){vec3 k=clamp(abs(mod(h*6.+vec3(0.,4.,2.),6.)-3.)-1.,0.,1.);return v*mix(vec3(1.),k,s);}
void main(){float r=length(vUv);if(r>1.)discard;float a;vec3 c=uColor;
  if(uMode<.5){a=exp(-r*r*5.)*.8+.2*pow(max(0.,1.-r),2.);}
  else if(uMode<1.5){float d=(r-uRing)/uWidth;a=exp(-d*d);}
  else{float d=(r-uRing)/uWidth;a=exp(-d*d);float ang=atan(vUv.y,vUv.x)/6.2831853;c=mix(vec3(1.),hsv(ang+uTime*.05,.75,1.),.85);}
  a*=uAlpha*uGlow;gl_FragColor=vec4(c*a,a);}