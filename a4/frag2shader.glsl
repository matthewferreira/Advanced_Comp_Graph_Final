#version 430

in vec3 varyingNormal, varyingLightDir, varyingVertPos, varyingHalfVec, varyingTangent;
in vec4 shadow_coord;
in vec2 tc;
in vec3 originalPosition, vertEyeSpacePos;
out vec4 fragColor;
 
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

vec3 calcNewNormal()
{
	vec3 normal = normalize(varyingNormal);
	vec3 tangent = normalize(varyingTangent);
	tangent = normalize(tangent - dot(tangent, normal) * normal);
	vec3 bitangent = cross(tangent, normal);
	mat3 tbn = mat3(tangent, bitangent, normal);
	vec3 retrievedNormal = texture(normMap,tc).xyz;
	retrievedNormal = retrievedNormal * 2.0 - 1.0;
	vec3 newNormal = tbn * retrievedNormal;
	newNormal = normalize(newNormal);
	return newNormal;
}

void main(void)
{	vec3 L = normalize(varyingLightDir);
	vec3 N = normalize(varyingNormal);
	vec3 V = normalize(-varyingVertPos);
	vec3 H = normalize(varyingHalfVec);
	
	float notInShadow = textureProj(shadowTex, shadow_coord);
	vec4 lightColor;
	
	if(envMapped)
	{
		vec3 r = -reflect(normalize(-varyingVertPos), normalize(varyingNormal));
		fragColor = texture(tex_map, r);
	}
	else if(normalMapped)
	{		
		N = calcNewNormal();
		
		lightColor = globalAmbient * material.ambient +
				light.ambient * material.ambient;
				
		if (notInShadow == 1.0)
		{	lightColor += light.diffuse * material.diffuse * max(dot(L,N),0.0)
					+ light.specular * material.specular
					* pow(max(dot(H,N),0.0),material.shininess*3.0);
		}
		fragColor = 0.5*texture(t, tc) + 0.5 *lightColor;
	}
	else if(noise)
	{
		vec4 texColor = texture(s, originalPosition/3.0 + 0.5);
		fragColor = 0.7 * texColor * (globalAmbient + light.ambient + light.diffuse * max(dot(L,N),0.0))
			+ 0.5 * light.specular * pow(max(dot(V, H),0.0), material.shininess);
		if (notInShadow == 1.0)
		{	fragColor += light.diffuse * material.diffuse * max(dot(L,N),0.0)
					+ light.specular * material.specular
					* pow(max(dot(H,N),0.0),material.shininess*3.0);
		}
		 
	}
	else if(fogBool)
	{
		vec4 fogColor = vec4(0.4, 0.8, 0.9, 1.0);	// bluish gray
		float fogStart = 0.8;
		float fogEnd = 0.9;

		// the distance from the camera to the vertex in eye space is simply the length of a
		// vector to that vertex, because the camera is at (0,0,0) in eye space.
		float dist = length(vertEyeSpacePos.xyz);
		float fogFactor = clamp(((fogEnd-dist)/(fogEnd-fogStart)), 0.0, 1.0);
	
		fragColor = globalAmbient * material.ambient
				+ light.ambient * material.ambient;

		if (notInShadow == 1.0)
		{	fragColor += light.diffuse * material.diffuse * max(dot(L,N),0.0)
					+ light.specular * material.specular
					* pow(max(dot(H,N),0.0),material.shininess*3.0);
		}
		fragColor = mix(fogColor, fragColor, fogFactor);
	}
	else
	{
		fragColor = globalAmbient * material.ambient
				+ light.ambient * material.ambient;

		if (notInShadow == 1.0)
		{	fragColor += light.diffuse * material.diffuse * max(dot(L,N),0.0)
					+ light.specular * material.specular
					* pow(max(dot(H,N),0.0),material.shininess*3.0);
		}
		fragColor = vec4(fragColor.xyz, alpha);
	}
}
