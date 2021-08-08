#version 430

layout (location=0) in vec3 vertPos;
layout (location=1) in vec3 vertNormal;
layout (location=2) in vec2 texCoord;
layout (location=3) in vec3 vertTangent;

out vec3 varyingNormal, varyingLightDir, varyingVertPos, varyingHalfVec, varyingTangent, originalPosition; 
out vec4 shadow_coord;
out vec2 tc;
out vec3 vertEyeSpacePos;

struct PositionalLight
{	vec4 ambient, diffuse, specular;
	vec3 position;
};
struct Material
{	vec4 ambient, diffuse, specular;   
	float shininess;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
uniform mat4 norm_matrix;
uniform mat4 shadowMVP;
uniform bool envMapped, noise, normalMapped, fogBool;
uniform float alpha, flipNormal;
layout (binding=0) uniform sampler2DShadow shadowTex;
layout (binding=1) uniform samplerCube tex_map;
layout (binding=2) uniform sampler2D t;
layout (binding=3) uniform sampler2D normMap;
layout (binding=4) uniform sampler3D s;
layout (binding=5) uniform sampler2D h;

void main(void)
{	
	tc = texCoord;
	originalPosition = vertPos;
	vec4 p = vec4(vertPos,1.0) + vec4((vertNormal*((texture2D(h,texCoord).r)/5.0f)),1.0f);
	vertEyeSpacePos = (mv_matrix * p).xyz;
	
	//output the vertex position to the rasterizer for interpolation
	varyingVertPos = (mv_matrix * vec4(vertPos,1.0)).xyz;
        
	//get a vector from the vertex to the light and output it to the rasterizer for interpolation
	varyingLightDir = light.position - varyingVertPos;

	//get a vertex normal vector in eye space and output it to the rasterizer for interpolation
	varyingNormal = (norm_matrix * vec4(vertNormal,1.0)).xyz;
	
	varyingTangent = (norm_matrix * vec4(vertTangent,1.0)).xyz;
	
	// calculate the half vector (L+V)
	varyingHalfVec = (varyingLightDir-varyingVertPos).xyz;
	
	if(flipNormal < 0) varyingNormal = -varyingNormal;
	
	shadow_coord = shadowMVP * vec4(vertPos,1.0);
	
	gl_Position = proj_matrix * mv_matrix * p;
}
