#define texture2D texture
#define texture2DLodEXT textureLod
#define gl_FragDepthEXT gl_FragDepth

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrix;
uniform mat3 normalMatrix;
uniform vec3 cameraPosition;

#ifdef NEXUS_VERTEX_SHADER
#define attribute in
#define varying out
in vec3 position;
in vec3 normal;
in vec3 color;
in vec2 uv;
#else
#define varying in
out vec4 nexusColor;
#define gl_FragColor nexusColor
#define main nexusMaterialMain
#endif
