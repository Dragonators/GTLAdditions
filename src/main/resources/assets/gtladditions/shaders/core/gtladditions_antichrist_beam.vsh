#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float SegmentQuads;
uniform float Time;
uniform float SegmentArray[33];
uniform vec3 CameraPosition;

out vec2 texCoord;
out vec2 localPosition;
out float transparency;

const float PI = 3.1415926535897;
const int MAX_SEGMENTS = 10;

vec3 segmentEndpoint(int endpointId) {
    int base = endpointId * 3;
    return vec3(SegmentArray[base], SegmentArray[base + 1], SegmentArray[base + 2]);
}

float getAngle(int quadId, int localId) {
    int idOffset = (localId > 1 && localId < 5) ? 0 : 1;
    return (PI * float(quadId + idOffset)) / SegmentQuads;
}

void main() {
    int id = gl_VertexID;
    int quads = int(SegmentQuads);
    int localId = id % 6;
    int quadId = (id / 6) % quads;
    int segmentId = id / (quads * 6);
    segmentId = min(segmentId, MAX_SEGMENTS - 1);

    vec3 startSegment = segmentEndpoint(segmentId);
    vec3 endSegment = segmentEndpoint(segmentId + 1);

    float radius0 = startSegment.x;
    float radius1 = endSegment.x;
    float offset0 = startSegment.y;
    float offset1 = endSegment.y;
    float trans0 = startSegment.z;
    float trans1 = endSegment.z;

    float cameraAngle = atan(CameraPosition.y, CameraPosition.x);
    float staticAngle = getAngle(quadId, localId);
    float angle = staticAngle + (cameraAngle - PI / 2.0);

    float offset = (localId > 0 && localId < 4) ? offset0 : offset1;
    float radius = (localId > 0 && localId < 4) ? radius0 : radius1;
    transparency = (localId > 0 && localId < 4) ? trans0 : trans1;

    vec3 localPos = vec3(cos(angle) * radius, sin(angle) * radius, offset);
    gl_Position = ProjMat * ModelViewMat * vec4(localPos, 1.0);

    localPosition = localPos.xy;

    float timer = Time / 240.0;
    float heightOffset = (offset / 256.0) + timer;
    texCoord = vec2(heightOffset, angle / (2.0 * PI) + heightOffset / 3.0 + timer);
}
