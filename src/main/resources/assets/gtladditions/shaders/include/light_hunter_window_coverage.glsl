// The aperture and its coplanar star chart share one coverage function, including signal dropouts.
float paradoxHash(vec2 p) {
    return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);
}
vec2 lightHunterWindowCoord(vec3 position) {
    return vec2(-(position.z-12.0),position.y-6.0)/12.2;
}
float lightHunterWindowCoverage(vec2 q,vec3 signal) {
    if (length(q)>=1.0) return 0.0;
    vec2 v=q*0.5+0.5;
    float row=floor(v.y*11.0),frame=signal.z;
    float cell=paradoxHash(floor(v*vec2(6,15))+vec2(frame*7.0,frame*3.0));
    return signal.x*step(0.30*signal.y,cell)*step(0.12*signal.y,paradoxHash(vec2(row+4.0,frame)));
}
