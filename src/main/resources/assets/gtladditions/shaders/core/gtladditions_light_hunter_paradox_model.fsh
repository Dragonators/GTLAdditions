#version 150
uniform sampler2D ModelMap;
uniform int HasTexture;
uniform int Kind;
uniform vec3 Eye;
in vec2 uv;
in vec4 tint;
in vec3 worldPosition;
in vec3 worldNormal;
out vec4 fragColor;
void main() {
    vec4 texel=HasTexture==1?texture(ModelMap,uv):vec4(1.0);
    vec3 albedo=pow(max(texel.rgb,vec3(0.0)),vec3(2.2))*tint.rgb;
    vec3 n=normalize(worldNormal);
    vec3 view=normalize(Eye-worldPosition);
    vec3 color;
    if (Kind==1) {
        float light=max(0.0,dot(n,normalize(vec3(1.0,-0.2,-0.65))));
        vec3 base=mix(albedo,vec3(dot(albedo,vec3(0.2126,0.7152,0.0722))),0.12)*vec3(0.80,0.92,1.0);
        float limb=pow(1.0-max(0.0,dot(n,view)),5.0);
        color=base*(0.012+light*0.32)+vec3(0.025,0.065,0.09)*limb*(0.15+light);
    } else {
        vec3 key=normalize(vec3(-3.0,4.0,6.0));
        vec3 fill=normalize(vec3(4.0,-1.0,2.0));
        vec3 ambient=mix(vec3(0.10,0.14,0.20),vec3(0.44,0.48,0.53),n.y*0.5+0.5);
        color=albedo*(ambient+vec3(1.05)*max(0.0,dot(n,key))+vec3(0.16,0.20,0.26)*max(0.0,dot(n,fill)));
        color+=vec3(0.025)*pow(max(0.0,dot(n,normalize(key+view))),12.0);
    }
    fragColor=vec4(color,texel.a*tint.a);
}
