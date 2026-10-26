#version 430
layout(local_size_x=8,local_size_y=4,local_size_z=4) in;
layout(rg16f,binding=0) uniform writeonly image3D FieldOutput;
uniform int FirstCell;
uniform int CellCount;
uniform ivec3 FieldSize;
#moj_import <gtladditions:light_hunter_fusion_noise.glsl>
void main(){
    ivec3 cell=ivec3(gl_GlobalInvocationID);
    if(cell.x>=FieldSize.x || cell.y>=FieldSize.y || cell.z>=CellCount)return;
    float angle=float(cell.x)/float(FieldSize.x)*6.28318530718;
    float radial=float(cell.y)/float(FieldSize.y-1)*1.80-.90;
    int absoluteCell=FirstCell+cell.z;
    int slot=(absoluteCell%FieldSize.z+FieldSize.z)%FieldSize.z;
    float baseZ=float(absoluteCell)*.078125;
    imageStore(FieldOutput,ivec3(cell.xy,slot),vec4(fusionWispAtBase(angle,radial,baseZ),0,0));
}
