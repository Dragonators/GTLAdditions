float hash31(vec3 p){
    p=fract(p*.1031);p+=dot(p,p.yzx+33.33);return fract((p.x+p.y)*p.z);
}
float noise3(vec3 p){
    vec3 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);
    return mix(mix(mix(hash31(i),hash31(i+vec3(1,0,0)),f.x),mix(hash31(i+vec3(0,1,0)),hash31(i+vec3(1,1,0)),f.x),f.y),
               mix(mix(hash31(i+vec3(0,0,1)),hash31(i+vec3(1,0,1)),f.x),mix(hash31(i+vec3(0,1,1)),hash31(i+vec3(1,1,1)),f.x),f.y),f.z);
}
float fbm(vec3 p){return (noise3(p)*.5+noise3(p*2.03+7.7)*.25+noise3(p*4.07+19.1)*.125)/.875;}
vec2 fusionWispAtBase(float angle,float radial,float baseZ){
    float a=angle;
    // The shell texture has independent along-ring and cross-ring coordinates; it cannot collapse into parallel stripes.
    vec3 base=vec3(cos(a)*(23.0+radial*2.3),sin(a)*(23.0+radial*2.3),baseZ);
    float warp=fbm(base*.58),crossWarp=fbm(base*.63+vec3(4.7,13.1,7.2));
    vec3 q=base+vec3(warp*2.7,crossWarp*2.7,warp*4.1-crossWarp*2.8);
    return vec2(fbm(q),noise3(q*2.1+8.8));
}
vec2 fusionWispComponents(float angle,float radial,float x,float time){
    return fusionWispAtBase(angle-time*.055,radial,x*1.4-time*.28);
}
float fusionWispDensity(vec2 components){
    float body=smoothstep(.43,.68,components.x);
    float ridge=pow(1.0-abs(components.y*2.0-1.0),9.0);
    return body*.72+ridge*body*1.3;
}
float fusionWispNoise(float angle,float radial,float x,float time){
    return fusionWispDensity(fusionWispComponents(angle,radial,x,time));
}
