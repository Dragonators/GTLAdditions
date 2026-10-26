uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
vec3 linearToSrgb(vec3 color) {
    return mix(color * 12.92, 1.055 * pow(color, vec3(1.0 / 2.4)) - 0.055, step(vec3(0.0031308), color));
}

in vec3 LocalPosition;
uniform float Phase;
uniform float SurfaceCoverage;
uniform float InteriorStrength;

uniform sampler2D Sampler0;
uniform vec3 CameraPosition;
float coreEntryViewDepth;
float hash(vec3 p){p=fract(p*.1031);p+=dot(p,p.yzx+33.33);return fract((p.x+p.y)*p.z);}
float noise3(vec3 p){
  vec3 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);
  return mix(mix(mix(hash(i),hash(i+vec3(1.0,0.0,0.0)),f.x),mix(hash(i+vec3(0.0,1.0,0.0)),hash(i+vec3(1.0,1.0,0.0)),f.x),f.y),
    mix(mix(hash(i+vec3(0.0,0.0,1.0)),hash(i+vec3(1.0,0.0,1.0)),f.x),mix(hash(i+vec3(0.0,1.0,1.0)),hash(i+vec3(1.0,1.0,1.0)),f.x),f.y),f.z);
}
float cloud(vec3 p){return .65*noise3(p)+.35*noise3(p*2.07+7.3);}
float max3(vec3 p){return max(p.x,max(p.y,p.z));}
mat2 rotate(float a){float c=cos(a),s=sin(a);return mat2(c,-s,s,c);}
vec3 faceNormal(vec3 p){
  vec3 a=abs(p);
  if(a.x>a.y&&a.x>a.z)return vec3(sign(p.x),0.0,0.0);
  if(a.y>a.z)return vec3(0.0,sign(p.y),0.0);
  return vec3(0.0,0.0,sign(p.z));
}
float edgeAt(vec3 p){
  vec3 a=abs(p);
  float second=max(min(a.x,a.y),min(max(a.x,a.y),a.z));
  return exp(-max(0.0,1.0-second)*115.0);
}
vec4 surface(vec3 p,vec3 rd){
  vec3 normal=faceNormal(p);
  float edge=edgeAt(p),fresnel=pow(1.0-abs(dot(normal,rd)),3.0);
  float lighting=.5+.5*dot(normal,normalize(vec3(.55,.8,.6)));
  float alpha;vec3 color;
    vec2 uv=abs(normal.x)>.5?p.zy:(abs(normal.y)>.5?p.xz:p.xy);
    uv=uv*.5+.5;
    vec2 cell=floor(clamp(uv,0.0,.99999)*16.0);
    vec4 trace=texture(Sampler0,(cell+.5)/16.0);
    vec2 local=fract(clamp(uv,0.0,.99999)*16.0)-.5;
    float dist=max(abs(local.x),abs(local.y));
    if(texture(Sampler0,(cell+vec2(-.5,.5))/16.0).r>.5)dist=min(dist,max(max(local.x,0.0),abs(local.y)));
    if(texture(Sampler0,(cell+vec2(1.5,.5))/16.0).r>.5)dist=min(dist,max(max(-local.x,0.0),abs(local.y)));
    if(texture(Sampler0,(cell+vec2(.5,-.5))/16.0).r>.5)dist=min(dist,max(abs(local.x),max(local.y,0.0)));
    if(texture(Sampler0,(cell+vec2(.5,1.5))/16.0).r>.5)dist=min(dist,max(abs(local.x),max(-local.y,0.0)));
    float track=trace.r*(1.0-smoothstep(.17,.25,dist));
    float halo=trace.r*(1.0-smoothstep(.24,.44,dist));
    float flow=pow(.5+.5*sin(Phase*2.4-trace.g*25.1327),8.0)*track;
    float rim=min(min(uv.x,uv.y),min(1.0-uv.x,1.0-uv.y));
    float border=(1.0-smoothstep(.009,.017,rim))*.36;
    alpha=clamp(mix(.36,.62,clamp(SurfaceCoverage/.85,0.0,1.0))+.37*track+.09*fresnel+edge*.42,0.0,.99);
    color=vec3(.008,.045,.10)*(.65+.7*lighting);
    color+=track*vec3(.028,.25,.44)+flow*vec3(.4,.82,1.05)+halo*vec3(.005,.025,.06);
    color+=border*vec3(.06,.2,.32);
  color+=edge*vec3(.35,.75,.95)+fresnel*vec3(.035,.09,.15);
  return vec4(color,alpha);
}
vec4 coreMaterial(){
  vec3 rd=normalize(LocalPosition-CameraPosition);
  vec3 safeRd=sign(rd+vec3(.0000001))*max(abs(rd),vec3(.00001));
  vec3 a=(-vec3(1.0)-CameraPosition)/safeRd,b=(vec3(1.0)-CameraPosition)/safeRd;
  vec3 lo=min(a,b),hi=max(a,b);
  float nearT=max(0.0,max(max(lo.x,lo.y),lo.z)),farT=min(min(hi.x,hi.y),hi.z);
  if(farT<=nearT)discard;
  vec3 front=CameraPosition+rd*nearT,back=CameraPosition+rd*farT;
  // The proxy is the exit face. Test the visible entry surface against the scene depth instead.
  coreEntryViewDepth = max(-(ModelViewMat * vec4(front, 1.0)).z, 0.0);
  vec4 entryClip = ProjMat * ModelViewMat * vec4(front, 1.0);
  gl_FragDepth = nearT > 0.0 && entryClip.w > 0.0 ? clamp(entryClip.z / entryClip.w * 0.5 + 0.5, 0.0, 1.0) : 0.0;
  float stepLen=(farT-nearT)/56.0;
  vec3 color=vec3(0.0);float trans=1.0;
  float t=Phase*1.05;
  for(int i=0;i<56;i++){
    vec3 p=CameraPosition+rd*(nearT+(float(i)+.5)*stepLen);
    float bound=1.0-smoothstep(.88,1.0,max3(abs(p)));
    float mass,sheet,secondary;
      vec3 q=p;q.xz=rotate(t*.95+p.y*.85)*q.xz;
      vec3 warp=sin(q.zxy*3.8+vec3(t*1.1,-t*.9,t*.8))+.35*sin(q.yzx*5.1+vec3(-t*.8,t*1.3,t*.65));
      float n=cloud(q*3.0+warp*.7+vec3(-t*.20,t*.17,t*.12));
      mass=smoothstep(.25,.69,n);
      sheet=1.0-smoothstep(.012,.075,abs(n-.53));
      secondary=smoothstep(.63,.75,n);
    vec3 centerFlow=p+.07*sin(p.yzx*4.3+vec3(t*.45,-t*.38,t*.32));
    vec3 centerMetric=centerFlow*vec3(1.0,.9,1.05);
    float central=exp(-dot(centerMetric,centerMetric)*6.5);
    float focus=exp(-dot(centerMetric,centerMetric)*17.0);
    float concentration=central*(.65+.50*mass+.35*sheet)+focus*.6;
    float density=((.08+mass*.18+sheet*2.3+secondary*.9)*(.24+central*1.28)+concentration*.70)*bound;
    float absorb=1.0-exp(-density*stepLen*2.0);
    vec3 base=mix(vec3(.003,.008,.04),vec3(.018,.09,.22),mass*mass);
    base+=concentration*vec3(.045,.17,.30);
    vec3 surge;
      surge=sheet*vec3(.30,1.5,2.6)+secondary*vec3(1.2,2.1,2.6)+mass*vec3(.006,.035,.10);
    surge*=.22+1.15*central;
    surge+=concentration*(vec3(.65,1.7,2.7)+sheet*vec3(1.0,2.1,2.7));
    surge+=focus*vec3(2.8,4.5,5.8)*(.70+.30*mass);
    color+=trans*(absorb*base+surge*bound*stepLen)*InteriorStrength*.65;
    trans*=1.0-absorb;
  }
  vec4 rear=surface(back,-rd);float rearAlpha=rear.a*.10;
  color+=trans*rear.rgb*rearAlpha;
  float alpha=1.0-trans*(1.0-rearAlpha);
  vec4 skin=nearT > 0.0 ? surface(front,rd) : vec4(0.0);
  color=skin.rgb*skin.a+(1.0-skin.a)*color;
  alpha=skin.a+(1.0-skin.a)*alpha;
  color/=max(alpha,.001);
  color=vec3(1.0)-exp(-color*2.1);
  vec4 fragColor=vec4(color,alpha);

  fragColor.rgb = linearToSrgb(fragColor.rgb);
  return fragColor;
}
