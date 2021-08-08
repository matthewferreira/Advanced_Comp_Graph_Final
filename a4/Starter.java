package a4;

import java.nio.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.Color;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLContext;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.texture.*;
import javax.swing.JPanel;
import java.lang.Math;
import org.joml.*;

public class Starter extends JFrame implements GLEventListener, MouseMotionListener, MouseWheelListener
{	private GLCanvas myCanvas;
	private int shadowProgram, objectProgram, cubeMapProgram;
	private int vao[] = new int[1];
	private int vbo[] = new int[14];
	
	private Camera cam = new Camera();
	private float cameraX, cameraY, cameraZ;
	
	// booleans
	private boolean envMapped, normalMapped, noiseBool;
	private int normalMapLoc, envMapLoc, noiseBoolLoc, alphaLoc, flipLoc, fogBoolLoc;
	private boolean fogBool = false;
	
	// for light movement
	private int lastMouseX, lastMouseY, mouseClickX, mouseClickY;
	private int wheelClicked = 5;
	
	// models
	private ImportedModel shuttle, pyramid;
	private Torus myTorus;
	private int numshuttleVertices, numSphereVertices, numTorusVertices, numTorusIndices, numPyramidVertices;
	private Sphere mySphere = new Sphere(48);
	
	//textures and normal maps
	private int skyboxTexture, marsTexture, marsNormalMap;
	
	//3d texturing
	private int noiseTexture;
	private int noiseHeight= 200;
	private int noiseWidth = 200;
	private int noiseDepth = 200;
	private double[][][] noise = new double[noiseWidth][noiseHeight][noiseDepth];
	private java.util.Random random = new java.util.Random();
	
	// time factors
	private double elapsedTime, startTime;
	
	// location of torus, shuttle, light, and camera
	private Vector3f torusLoc = new Vector3f(0.0f, 0.2f, 6.0f); 
	private Vector3f shuttleLoc = new Vector3f(0.0f, 0.3f, 1.5f); 
	private Vector3f cameraLoc = new Vector3f(0.0f, 0.2f, 6.0f);
	private Vector3f lightLoc = new Vector3f(-7.02f, 5.0f, 8.56f);
	private Vector3f sphereLoc = new Vector3f(0.0f, 0.1f, 0.3f);
	private Vector3f pyrLoc = new Vector3f(0.0f, 0.0f, 0.0f); 
	
	// white light properties
	private float[] globalAmbient = new float[] { 0.7f, 0.7f, 0.7f, 1.0f };
	private float[] lightAmbient = new float[] { 0.0f, 0.0f, 0.0f, 1.0f };
	private float[] lightDiffuse = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
	private float[] lightSpecular = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
		
	// gold material
	private float[] GmatAmb = Utils.goldAmbient();
	private float[] GmatDif = Utils.goldDiffuse();
	private float[] GmatSpe = Utils.goldSpecular();
	private float GmatShi = Utils.goldShininess();
	
	// bronze material
	private float[] BmatAmb = Utils.bronzeAmbient();
	private float[] BmatDif = Utils.bronzeDiffuse();
	private float[] BmatSpe = Utils.bronzeSpecular();
	private float BmatShi = Utils.bronzeShininess();
	
	private float[] thisAmb, thisDif, thisSpe, matAmb, matDif, matSpe;
	private float thisShi, matShi;
	
	// shadow stuff
	private int scSizeX, scSizeY;
	private int [] shadowTex = new int[1];
	private int [] shadowBuffer = new int[1];
	private Matrix4f lightVmat = new Matrix4f();
	private Matrix4f lightPmat = new Matrix4f();
	private Matrix4f shadowMVP1 = new Matrix4f();
	private Matrix4f shadowMVP2 = new Matrix4f();
	private Matrix4f b = new Matrix4f();

	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4f pMat = new Matrix4f();  // perspective matrix
	private Matrix4f vMat = new Matrix4f();  // view matrix
	private Matrix4f mMat = new Matrix4f();  // model matrix
	private Matrix4f mvMat = new Matrix4f(); // model-view matrix
	private Matrix4f rMat = new Matrix4f(); // rotation 
	private Matrix4f invTrMat = new Matrix4f(); // inverse-transpose
	private int mvLoc, projLoc, nLoc, sLoc, vLoc;
	private int globalAmbLoc, ambLoc, diffLoc, specLoc, posLoc, mambLoc, mdiffLoc, mspecLoc, mshiLoc;
	private float aspect;
	private Vector3f currentLightPos = new Vector3f();
	private float[] lightPos = new float[3];
	private Vector3f origin = new Vector3f(0.0f, 0.0f, 0.0f);
	private Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
	
	public Starter()
	{	setTitle("Assignment 4 - Matthew Ferreira");
		setSize(1280, 800);
		initKeyCommands();
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		myCanvas.addMouseMotionListener(this);
		addMouseMotionListener(this);
		this.addMouseWheelListener(this);
		this.add(myCanvas);
		this.setVisible(true);
		Animator animator = new Animator(myCanvas);
		animator.start();
	}

	public void display(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		
		vMat.identity().setTranslation(-cameraLoc.x(), -cameraLoc.y(), -cameraLoc.z());
		vMat.rotation(cam.getRotationVec().x, 1.0f, 0.0f, 0.0f);
		vMat.mul(rMat.identity().rotation(cam.getRotationVec().y, 0.0f, 1.0f, 0.0f));
		vMat.translate(-cam.getCVec().x - cameraX, -cam.getCVec().y - cameraY, -cam.getCVec().z - cameraZ);
		// drawing cube map
		gl.glUseProgram(cubeMapProgram);
		vLoc = gl.glGetUniformLocation(cubeMapProgram, "v_matrix");
		projLoc = gl.glGetUniformLocation(cubeMapProgram, "proj_matrix");
		
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
				
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, skyboxTexture);
		
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);	     // cube is CW, but we are viewing the inside
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		gl.glEnable(GL_DEPTH_TEST);
		
		if(lastMouseX > 0){
			lightLoc.set(((lastMouseX - mouseClickX)/50.0f) - 10.0f, (float)wheelClicked, ((lastMouseY - mouseClickY)/50.0f) - 10.0f);
		}
		else
		{
			lightLoc.set(-7.02f, 5.0f, 8.56f);
		}
		
		currentLightPos.set(lightLoc);
		
		lightVmat.identity().setLookAt(currentLightPos, origin, up);	// vector from light to origin
		lightPmat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);
		
		gl.glBindFramebuffer(GL_FRAMEBUFFER, shadowBuffer[0]);
		gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadowTex[0], 0);
		
		gl.glDrawBuffer(GL_NONE);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_POLYGON_OFFSET_FILL);	//  for reducing
		gl.glPolygonOffset(3.0f, 5.0f);		//  shadow artifacts
	
		passOne();
		
		gl.glDisable(GL_POLYGON_OFFSET_FILL);	// artifact reduction, continued
		gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);
	
		gl.glDrawBuffer(GL_FRONT);
		
		passTwo();
	}

	public void passOne()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glUseProgram(shadowProgram);

		// draw the shuttle
		mMat.identity();
		mMat.translate((float)Math.cos(elapsedTime * 10), shuttleLoc.y(), (float)Math.sin(elapsedTime* 10));
		mMat.rotateXYZ(0.0f, (float)elapsedTime * -7.4f , 0.0f);

		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat);
		shadowMVP1.mul(lightVmat);
		shadowMVP1.mul(mMat);
		sLoc = gl.glGetUniformLocation(shadowProgram, "shadowMVP");
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
	
		gl.glDrawArrays(GL_TRIANGLES, 0, numshuttleVertices);
		
		// draw the pyramid
		mMat.identity();
		mMat.translate((float)Math.sin(elapsedTime * 10) * 4.0f, pyrLoc.y(), (float)Math.cos(elapsedTime* 10) * 4.0f);
		mMat.rotateX((float)Math.toRadians(30.0f));
		mMat.rotateY((float)Math.toRadians(40.0f));

		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat);
		shadowMVP1.mul(lightVmat);
		shadowMVP1.mul(mMat);
		sLoc = gl.glGetUniformLocation(shadowProgram, "shadowMVP");
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
	
		gl.glDrawArrays(GL_TRIANGLES, 0, numPyramidVertices);
		
		//draw sphere
		mMat.identity();
		mMat.translate(sphereLoc.x(), sphereLoc.y(), sphereLoc.z());
		mMat.rotateXYZ(0.0f, (float)elapsedTime, 0.0f);
		
		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat);
		shadowMVP1.mul(lightVmat);
		shadowMVP1.mul(mMat);
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);	
	
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, numSphereVertices);
		
		// draw the torus
		mMat.identity();
		mMat.translate(torusLoc.x(), torusLoc.y(), torusLoc.z());
		mMat.rotateXYZ(-45.0f, (float)elapsedTime, 45.0f);
		
		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat);
		shadowMVP1.mul(lightVmat);
		shadowMVP1.mul(mMat);
		
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);	
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_ALWAYS);

		gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[4]);
		gl.glDrawElements(GL_TRIANGLES, numTorusIndices, GL_UNSIGNED_INT, 0);
	}

	public void passTwo()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
		gl.glUseProgram(objectProgram);
		
		mvLoc = gl.glGetUniformLocation(objectProgram, "mv_matrix");
		projLoc = gl.glGetUniformLocation(objectProgram, "proj_matrix");
		nLoc = gl.glGetUniformLocation(objectProgram, "norm_matrix");
		sLoc = gl.glGetUniformLocation(objectProgram, "shadowMVP");
		
		elapsedTime = System.currentTimeMillis() - startTime;
		elapsedTime = elapsedTime / 10000.0;
		
		// draw the shuttle
		thisAmb = GmatAmb; // the shuttle is gold
		thisDif = GmatDif;
		thisSpe = GmatSpe;
		thisShi = GmatShi;
		
		mMat.identity();
		mMat.translate((float)Math.cos(elapsedTime * 10), shuttleLoc.y(), (float)Math.sin(elapsedTime* 10));
		mMat.rotateXYZ(0.0f, (float)elapsedTime * -7.4f , 0.0f);

		currentLightPos.set(lightLoc);
		installLights(objectProgram, vMat);
		
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		
		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);
		
		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));
		normalMapped = false;	// shuttle is not normal mapped
		normalMapLoc = gl.glGetUniformLocation(objectProgram, "normalMapped");
		gl.glProgramUniform1i(objectProgram, normalMapLoc, normalMapped ? 1 : 0);
		envMapped = false;	// shuttle is not env mapped
		envMapLoc = gl.glGetUniformLocation(objectProgram, "envMapped");
		gl.glProgramUniform1i(objectProgram, envMapLoc, envMapped ? 1 : 0);
		noiseBool = true;	// shuttle is textured with noise
		noiseBoolLoc = gl.glGetUniformLocation(objectProgram, "noise");
		gl.glProgramUniform1i(objectProgram, noiseBoolLoc, noiseBool ? 1 : 0);
		
		alphaLoc = gl.glGetUniformLocation(objectProgram, "alpha");
		flipLoc = gl.glGetUniformLocation(objectProgram, "flipNormal");
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[12]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		
		gl.glActiveTexture(gl.GL_TEXTURE4);
		gl.glBindTexture(gl.GL_TEXTURE_3D, noiseTexture);
		
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDepthMask(true);
		gl.glDrawArrays(GL_TRIANGLES, 0, numshuttleVertices);
		
		// draw the pyramid
		thisAmb = GmatAmb; // the pyramid is gold
		thisDif = GmatDif;
		thisSpe = GmatSpe;
		thisShi = GmatShi;
		
		mMat.identity();
		//mMat.translate(pyrLoc.x(), pyrLoc.y(), pyrLoc.z());
		mMat.translate((float)Math.sin(elapsedTime * 10) * 4.0f, pyrLoc.y(), (float)Math.cos(elapsedTime* 10) * 4.0f);
		mMat.rotateX((float)Math.toRadians(30.0f));
		mMat.rotateY((float)Math.toRadians(40.0f));
		
		currentLightPos.set(lightLoc);
		installLights(objectProgram, vMat);
		
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		
		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);
		
		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));
		normalMapped = false;	// pyramid is not normal mapped
		normalMapLoc = gl.glGetUniformLocation(objectProgram, "normalMapped");
		gl.glProgramUniform1i(objectProgram, normalMapLoc, normalMapped ? 1 : 0);
		envMapped = false;	// pyramid is not env mapped
		envMapLoc = gl.glGetUniformLocation(objectProgram, "envMapped");
		gl.glProgramUniform1i(objectProgram, envMapLoc, envMapped ? 1 : 0);
		noiseBool = false;	// pyramid is textured with noise
		noiseBoolLoc = gl.glGetUniformLocation(objectProgram, "noise");
		gl.glProgramUniform1i(objectProgram, noiseBoolLoc, noiseBool ? 1 : 0);
		
		fogBoolLoc = gl.glGetUniformLocation(objectProgram, "fogBool"); // can toggle fog on and off.
		gl.glProgramUniform1i(objectProgram, fogBoolLoc, fogBool ? 1 : 0);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDepthMask(true);
		// blending transparency
		gl.glEnable(GL_BLEND);
		gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		gl.glBlendEquation(GL_FUNC_ADD);
		gl.glCullFace(GL_FRONT);
		gl.glProgramUniform1f(objectProgram, alphaLoc, 0.1f);
		gl.glProgramUniform1f(objectProgram, flipLoc, -1.0f);
		gl.glDrawArrays(GL_TRIANGLES, 0, numPyramidVertices);
		
		gl.glCullFace(GL_BACK);
		gl.glProgramUniform1f(objectProgram, alphaLoc, 0.2f);
		gl.glProgramUniform1f(objectProgram, flipLoc, 1.0f);
		gl.glDrawArrays(GL_TRIANGLES, 0, numPyramidVertices);
		gl.glDisable(GL_BLEND);
		
		
		// draw sphere
		currentLightPos.set(lightLoc);
		installLights(objectProgram, vMat);
		
		mMat.identity();
		mMat.translate(sphereLoc.x(), sphereLoc.y(), sphereLoc.z());
		mMat.rotateXYZ(0.0f, (float)elapsedTime, 0.0f);
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);
		
		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);
		
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));
		normalMapped = true;	// sphere is normal mapped
		normalMapLoc = gl.glGetUniformLocation(objectProgram, "normalMapped");
		gl.glProgramUniform1i(objectProgram, normalMapLoc, normalMapped ? 1 : 0);
		envMapped = false;	// sphere is not env mapped
		envMapLoc = gl.glGetUniformLocation(objectProgram, "envMapped");
		gl.glProgramUniform1i(objectProgram, envMapLoc, envMapped ? 1 : 0);
		noiseBool = false;	// sphere is not textured with noise
		noiseBoolLoc = gl.glGetUniformLocation(objectProgram, "noise");
		gl.glProgramUniform1i(objectProgram, noiseBoolLoc, noiseBool ? 1 : 0);
		// sphere vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		
		//sphere texture coords
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);
		
		// sphere normals
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		
		//sphere tangents
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		gl.glVertexAttribPointer(3, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(3);
		
		gl.glActiveTexture(gl.GL_TEXTURE2);
		gl.glBindTexture(gl.GL_TEXTURE_2D, marsTexture);
		
		gl.glActiveTexture(gl.GL_TEXTURE3);
		gl.glBindTexture(gl.GL_TEXTURE_2D, marsNormalMap);
	
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDepthMask(true);
	
		gl.glDrawArrays(GL_TRIANGLES, 0, numSphereVertices);
		
		// draw the torus
		thisAmb = BmatAmb; // the torus is bronze
		thisDif = BmatDif;
		thisSpe = BmatSpe;
		thisShi = BmatShi;
		
		vMat.identity().rotation(cam.getRotationVec().x, 1.0f, 0.0f, 0.0f);
		vMat.mul(rMat.identity().rotation(cam.getRotationVec().y, 0.0f, 1.0f, 0.0f));
		vMat.translate(-cam.getCVec().x - cameraLoc.x(), -cam.getCVec().y - cameraLoc.y(), -cam.getCVec().z - cameraLoc.z());
		
		currentLightPos.set(lightLoc);
		installLights(objectProgram, vMat);
		mMat.identity();
		mMat.translate(torusLoc.x(), torusLoc.y(), torusLoc.z());
		mMat.rotateXYZ(-45.0f, (float)elapsedTime, 45.0f);
		
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);
		
		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);
		
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));
		normalMapped = false;	// torus is not normal mapped
		normalMapLoc = gl.glGetUniformLocation(objectProgram, "normalMapped");
		gl.glProgramUniform1i(objectProgram, normalMapLoc, normalMapped ? 1 : 0);
		envMapped = true;	// torus is env mapped
		envMapLoc = gl.glGetUniformLocation(objectProgram, "envMapped");
		gl.glProgramUniform1i(objectProgram, envMapLoc, envMapped ? 1 : 0);
		noiseBool = false;	// torus is not textured with noise
		noiseBoolLoc = gl.glGetUniformLocation(objectProgram, "noise");
		gl.glProgramUniform1i(objectProgram, noiseBoolLoc, noiseBool ? 1 : 0);
	
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, skyboxTexture);
		
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDepthMask(true);
	
		gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[4]);
		gl.glDrawElements(GL_TRIANGLES, numTorusIndices, GL_UNSIGNED_INT, 0);
	}

	public void init(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		shadowProgram = Utils.createShaderProgram("a4/vert1shader.glsl", "a4/frag1shader.glsl");
		objectProgram = Utils.createShaderProgram("a4/vert2shader.glsl","a4/frag2shader.glsl");
		cubeMapProgram = Utils.createShaderProgram("a4/vertCshader.glsl", "a4/fragCshader.glsl");
		
		startTime = System.currentTimeMillis();
		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 1.0f, 1000.0f);

		setupVertices();
		setupShadowBuffers();
				
		b.set(
			0.5f, 0.0f, 0.0f, 0.0f,
			0.0f, 0.5f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.5f, 0.0f,
			0.5f, 0.5f, 0.5f, 1.0f);
			
		skyboxTexture = Utils.loadCubeMap("cubeMap");
		marsTexture = Utils.loadTexture("./textures/mars4kcolor.jpg");
		marsNormalMap = Utils.loadTexture("./textures/mars4knormal.jpg");
		gl.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
		generateNoise();	
		noiseTexture = buildNoiseTexture();
	}
	
	private void setupShadowBuffers()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		scSizeX = myCanvas.getWidth();
		scSizeY = myCanvas.getHeight();
	
		gl.glGenFramebuffers(1, shadowBuffer, 0);
	
		gl.glGenTextures(1, shadowTex, 0);
		gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32,
						scSizeX, scSizeY, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
		
		// may reduce shadow border artifacts
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	}

	private void setupVertices()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
		//sphere definition
		numSphereVertices = mySphere.getIndices().length;
		int[] indices = mySphere.getIndices();
		Vector3f[] vertices = mySphere.getVertices();
		Vector2f[] texCoords = mySphere.getTexCoords();
		Vector3f[] normals = mySphere.getNormals();
		Vector3f[] tangents = mySphere.getTangents();
		
		float[] spherePvalues = new float[indices.length*3];
		float[] sphereTvalues = new float[indices.length*2];
		float[] sphereNvalues = new float[indices.length*3];
		float[] sphereTanvalues = new float[indices.length*3];

		for (int i=0; i<indices.length; i++)
		{	spherePvalues[i*3]   = (float) (vertices[indices[i]]).x();
			spherePvalues[i*3+1] = (float) (vertices[indices[i]]).y();
			spherePvalues[i*3+2] = (float) (vertices[indices[i]]).z();
			sphereTvalues[i*2]   = (float) (texCoords[indices[i]]).x();
			sphereTvalues[i*2+1] = (float) (texCoords[indices[i]]).y();
			sphereNvalues[i*3]   = (float) (normals[indices[i]]).x();
			sphereNvalues[i*3+1] = (float) (normals[indices[i]]).y();
			sphereNvalues[i*3+2] = (float) (normals[indices[i]]).z();
			sphereTanvalues[i*3] = (float) (tangents[indices[i]]).x();
			sphereTanvalues[i*3+1] = (float) (tangents[indices[i]]).y();
			sphereTanvalues[i*3+2] = (float) (tangents[indices[i]]).z();
		}
		
		// cube 
		float[] cubeVertexPositions =
		{	-10.0f,  10.0f, -10.0f, -10.0f, -10.0f, -10.0f, 10.0f, -10.0f, -10.0f,
			10.0f, -10.0f, -10.0f, 10.0f,  10.0f, -10.0f, -10.0f,  10.0f, -10.0f,
			10.0f, -10.0f, -10.0f, 10.0f, -10.0f,  10.0f, 10.0f,  10.0f, -10.0f,
			10.0f, -10.0f,  10.0f, 10.0f,  10.0f,  10.0f, 10.0f,  10.0f, -10.0f,
			10.0f, -10.0f,  10.0f, -10.0f, -10.0f,  10.0f, 10.0f,  10.0f,  10.0f,
			-10.0f, -10.0f,  10.0f, -10.0f,  10.0f,  10.0f, 10.0f,  10.0f,  10.0f,
			-10.0f, -10.0f,  10.0f, -10.0f, -10.0f, -10.0f, -10.0f,  10.0f,  10.0f,
			-10.0f, -10.0f, -10.0f, -10.0f,  10.0f, -10.0f, -10.0f,  10.0f,  10.0f,
			-10.0f, -10.0f,  10.0f,  10.0f, -10.0f,  10.0f,  10.0f, -10.0f, -10.0f,
			10.0f, -10.0f, -10.0f, -10.0f, -10.0f, -10.0f, -10.0f, -10.0f,  10.0f,
			-10.0f,  10.0f, -10.0f, 10.0f,  10.0f, -10.0f, 10.0f,  10.0f,  10.0f,
			10.0f,  10.0f,  10.0f, -10.0f,  10.0f,  10.0f, -10.0f,  10.0f, -10.0f
		};
		
		// shuttle definition
		shuttle = new ImportedModel("../models/shuttle.obj");
		numshuttleVertices = shuttle.getNumVertices();
		vertices = shuttle.getVertices();
		normals = shuttle.getNormals();
		texCoords = shuttle.getTexCoords();
		
		float[] shuttlePvalues = new float[numshuttleVertices*3];
		float[] shuttleNvalues = new float[numshuttleVertices*3];
		float[] shuttleTvalues = new float[numshuttleVertices*2];
		
		for (int i=0; i<numshuttleVertices; i++)
		{	shuttlePvalues[i*3]   = (float) (vertices[i]).x();
			shuttlePvalues[i*3+1] = (float) (vertices[i]).y();
			shuttlePvalues[i*3+2] = (float) (vertices[i]).z();
			shuttleNvalues[i*3]   = (float) (normals[i]).x();
			shuttleNvalues[i*3+1] = (float) (normals[i]).y();
			shuttleNvalues[i*3+2] = (float) (normals[i]).z();
			shuttleTvalues[i*2]   = (float) (texCoords[i]).x();
			shuttleTvalues[i*2+1] = (float) (texCoords[i]).y();
		}
		
		// pyramid definition
		pyramid = new ImportedModel("../models/pyr.obj");
		numPyramidVertices = pyramid.getNumVertices();
		vertices = pyramid.getVertices();
		normals = pyramid.getNormals();
		texCoords = pyramid.getTexCoords();
		
		float[] pyramidPvalues = new float[numPyramidVertices*3];
		float[] pyramidNvalues = new float[numPyramidVertices*3];
		float[] pyramidTvalues = new float[numPyramidVertices*2];
		
		for (int i=0; i<numPyramidVertices; i++)
		{	pyramidPvalues[i*3]   = (float) (vertices[i]).x();
			pyramidPvalues[i*3+1] = (float) (vertices[i]).y();
			pyramidPvalues[i*3+2] = (float) (vertices[i]).z();
			pyramidNvalues[i*3]   = (float) (normals[i]).x();
			pyramidNvalues[i*3+1] = (float) (normals[i]).y();
			pyramidNvalues[i*3+2] = (float) (normals[i]).z();
			pyramidTvalues[i*2]   = (float) (texCoords[i]).x();
			pyramidTvalues[i*2+1] = (float) (texCoords[i]).y();
		}

		// torus definition
		myTorus = new Torus(4.0f, 0.1f, 48);
		numTorusVertices = myTorus.getNumVertices();
		numTorusIndices = myTorus.getNumIndices();
		vertices = myTorus.getVertices();
		normals = myTorus.getNormals();
		indices = myTorus.getIndices();
		
		float[] torusPvalues = new float[vertices.length*3];
		float[] torusNvalues = new float[normals.length*3];

		for (int i=0; i<numTorusVertices; i++)
		{	torusPvalues[i*3]   = (float) vertices[i].x();
			torusPvalues[i*3+1] = (float) vertices[i].y();
			torusPvalues[i*3+2] = (float) vertices[i].z();
			torusNvalues[i*3]   = (float) normals[i].x();
			torusNvalues[i*3+1] = (float) normals[i].y();
			torusNvalues[i*3+2] = (float) normals[i].z();
		}

		// buffers definition
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);

		gl.glGenBuffers(14, vbo, 0);

		//  put the Torus vertices into the first buffer,
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(torusPvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);
		
		//  load the shuttle vertices into the second buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer shuttleVertBuf = Buffers.newDirectFloatBuffer(shuttlePvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, shuttleVertBuf.limit()*4, shuttleVertBuf, GL_STATIC_DRAW);
		
		// load the torus normal coordinates into the third buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer torusNorBuf = Buffers.newDirectFloatBuffer(torusNvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, torusNorBuf.limit()*4, torusNorBuf, GL_STATIC_DRAW);
		
		// load the shuttle normal coordinates into the fourth buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer shuttleNorBuf = Buffers.newDirectFloatBuffer(shuttleNvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, shuttleNorBuf.limit()*4, shuttleNorBuf, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[4]);
		IntBuffer idxBuf = Buffers.newDirectIntBuffer(indices);
		gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxBuf.limit()*4, idxBuf, GL_STATIC_DRAW);
		
		//  put the sphere vertices into the 6th buffer,
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		FloatBuffer sphereVertBuf = Buffers.newDirectFloatBuffer(spherePvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, sphereVertBuf.limit()*4, sphereVertBuf, GL_STATIC_DRAW);
		
		//  put the sphere texture coords into the 7th buffer,
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		FloatBuffer sphereTexBuf = Buffers.newDirectFloatBuffer(sphereTvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, sphereTexBuf.limit()*4, sphereTexBuf, GL_STATIC_DRAW);
		
		//  put the sphere normals into the 8th buffer,
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		FloatBuffer sphereNorBuf = Buffers.newDirectFloatBuffer(sphereNvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, sphereNorBuf.limit()*4, sphereNorBuf, GL_STATIC_DRAW);
		
		//  put the sphere tangents into the 9th buffer,
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		FloatBuffer sphereTanBuf = Buffers.newDirectFloatBuffer(sphereTanvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, sphereTanBuf.limit()*4, sphereTanBuf, GL_STATIC_DRAW);
		
		// cube vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		FloatBuffer cvertBuf = Buffers.newDirectFloatBuffer(cubeVertexPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, cvertBuf.limit()*4, cvertBuf, GL_STATIC_DRAW);
		
		//  load the pyramid vertices into the 11th buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		FloatBuffer pyramidVertBuf = Buffers.newDirectFloatBuffer(pyramidPvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, pyramidVertBuf.limit()*4, pyramidVertBuf, GL_STATIC_DRAW);
		
		// load the pyramid normal coordinates into the 12th buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
		FloatBuffer pyramidNorBuf = Buffers.newDirectFloatBuffer(pyramidNvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, pyramidNorBuf.limit()*4, pyramidNorBuf, GL_STATIC_DRAW);
		
		//  put the shuttle texture coords into the 13th buffer,
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[12]);
		FloatBuffer shuttleTexBuf = Buffers.newDirectFloatBuffer(shuttleTvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, shuttleTexBuf.limit()*4, shuttleTexBuf, GL_STATIC_DRAW);
		
		//  put the pyramid texture coords into the 14th buffer,
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[13]);
		FloatBuffer pyramidTexBuf = Buffers.newDirectFloatBuffer(pyramidTvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, pyramidTexBuf.limit()*4, pyramidTexBuf, GL_STATIC_DRAW);
	}
	
	private void installLights(int renderingProgram, Matrix4f vMatrix)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
		currentLightPos.mulPosition(vMatrix);
		lightPos[0]=currentLightPos.x(); lightPos[1]=currentLightPos.y(); lightPos[2]=currentLightPos.z();
		
		// set current material values
		matAmb = thisAmb;
		matDif = thisDif;
		matSpe = thisSpe;
		matShi = thisShi;
		
		// get the locations of the light and material fields in the shader
		globalAmbLoc = gl.glGetUniformLocation(renderingProgram, "globalAmbient");
		ambLoc = gl.glGetUniformLocation(renderingProgram, "light.ambient");
		diffLoc = gl.glGetUniformLocation(renderingProgram, "light.diffuse");
		specLoc = gl.glGetUniformLocation(renderingProgram, "light.specular");
		posLoc = gl.glGetUniformLocation(renderingProgram, "light.position");
		mambLoc = gl.glGetUniformLocation(renderingProgram, "material.ambient");
		mdiffLoc = gl.glGetUniformLocation(renderingProgram, "material.diffuse");
		mspecLoc = gl.glGetUniformLocation(renderingProgram, "material.specular");
		mshiLoc = gl.glGetUniformLocation(renderingProgram, "material.shininess");
	
		//  set the uniform light and material values in the shader
		gl.glProgramUniform4fv(renderingProgram, globalAmbLoc, 1, globalAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram, ambLoc, 1, lightAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram, diffLoc, 1, lightDiffuse, 0);
		gl.glProgramUniform4fv(renderingProgram, specLoc, 1, lightSpecular, 0);
		gl.glProgramUniform3fv(renderingProgram, posLoc, 1, lightPos, 0);
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, matAmb, 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, matDif, 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, matSpe, 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, matShi);
	}
	
	// method to create keyboard commands
	public void initKeyCommands()
	{
		ExitCommand exitCommand = new ExitCommand(this); 					//keyboard command
		JComponent contentPane = (JComponent) this.getContentPane(); 		//get content pane of the JFrame (this)
		int mapName = JComponent.WHEN_IN_FOCUSED_WINDOW; 					//get the "focus is in the window" input map for the content pane
		InputMap imap = contentPane.getInputMap(mapName);
		
		KeyStroke xKey = KeyStroke.getKeyStroke('x'); 						//create keystroke object to represent x key
		imap.put(xKey, "exit"); 											//put the Key object into the inputmap under the identifier "exit"
		ActionMap amap = contentPane.getActionMap(); 						//get the action map for the content pane
		amap.put("exit", exitCommand); 										//put the gradient and exit command object into the content pane's action map
		
		// use D key to move right
		RightCommand rightCommand = new RightCommand(cam);
		KeyStroke dKey = KeyStroke.getKeyStroke('d');
		imap.put(dKey, "right"); 
		amap.put("right", rightCommand); 
		
		// use A key to move left
		LeftCommand leftCommand = new LeftCommand(cam);
		KeyStroke aKey = KeyStroke.getKeyStroke('a'); 
		imap.put(aKey, "left"); 
		amap.put("left", leftCommand); 
		
		//use Q key to move up
		UpCommand upCommand = new UpCommand(cam);
		KeyStroke qKey = KeyStroke.getKeyStroke('q'); 
		imap.put(qKey, "up");
		amap.put("up", upCommand); 
		
		// use E key to move down
		DownCommand downCommand = new DownCommand(cam);
		KeyStroke eKey = KeyStroke.getKeyStroke('e'); 
		imap.put(eKey, "down"); 
		amap.put("down", downCommand); 
		
		// use W key to move forward
		ForwardCommand forwardCommand = new ForwardCommand(cam);
		KeyStroke wKey = KeyStroke.getKeyStroke('w'); 
		imap.put(wKey, "forward"); 	
		amap.put("forward", forwardCommand); 
		
		// use S key to move backward
		BackwardCommand backwardCommand = new BackwardCommand(cam);
		KeyStroke sKey = KeyStroke.getKeyStroke('s');
		imap.put(sKey, "backward");
		amap.put("backward", backwardCommand); 
		
		// use Right Arrow key to pan camera to the right
		PanRightCommand panRightCommand = new PanRightCommand(cam);
		KeyStroke rightArrowKey = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
		imap.put(rightArrowKey, "panRight"); 	
		amap.put("panRight", panRightCommand); 
		
		// use Left Arrow key to pan camera to the left
		PanLeftCommand panLeftCommand = new PanLeftCommand(cam);
		KeyStroke leftArrowKey = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0); 		
		imap.put(leftArrowKey, "panLeft"); 											
		amap.put("panLeft", panLeftCommand); 
		
		// use Up Arrow key to pan camera to up
		PanUpCommand panUpCommand = new PanUpCommand(cam);
		KeyStroke upArrowKey = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0); 				
		imap.put(upArrowKey, "panUp"); 											
		amap.put("panUp", panUpCommand); 
		
		// use Down Arrow key to pan camera down
		PanDownCommand panDownCommand = new PanDownCommand(cam);
		KeyStroke downArrowKey = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0); 	
		imap.put(downArrowKey, "panDown"); 											
		amap.put("panDown", panDownCommand); 
		
		// use "F" to toggle lights on and off
		ToggleFogCommand toggleFogCommand = new ToggleFogCommand(this);
		KeyStroke fogKey = KeyStroke.getKeyStroke('f'); 						
		imap.put(fogKey, "fog"); 											
		amap.put("fog", toggleFogCommand); 
	}
	
	//mouse movement methods
	public void mouseWheelMoved(MouseWheelEvent e){
		// counting number of mouseclicks (up to 5 so it doesn't get too big)
		wheelClicked += e.getWheelRotation();
		if(wheelClicked > 8) wheelClicked = 8;
		if(wheelClicked < -3) wheelClicked = -3; 
	}
	
	public void mouseDragged(MouseEvent e)
	{
		lastMouseX = e.getX();
		lastMouseY = e.getY();
	}
	
	public void mouseClicked(MouseEvent e) 
	{
       mouseClickX = e.getX();
	   mouseClickY = e.getY();
    }
	public void mouseMoved(MouseEvent e){}
	
	
	//methods to toggle fog on and off
	public boolean getFogBool()
	{
		return fogBool;
	}
	public void setFogBool(boolean b)
	{
		fogBool = b;
	}
	
	// 3D Texture section
	private void fillDataArray(byte data[])
	{ double veinFrequency = 1.75;
	  double turbPower = 3.0;
	  double turbSize =  32.0;
	  for (int i=0; i<noiseWidth; i++)
	  { for (int j=0; j<noiseHeight; j++)
	    { for (int k=0; k<noiseDepth; k++)
	      {	double xyzValue = (float)i/noiseWidth + (float)j/noiseHeight + (float)k/noiseDepth
							+ turbPower * turbulence(i,j,k,turbSize)/256.0;

		double sineValue = logistic(Math.abs(Math.sin(xyzValue * 3.14159 * veinFrequency)));
		sineValue = Math.max(-1.0, Math.min(sineValue*1.25-0.20, 1.0));

		Color c = new Color((float)sineValue,
				(float)Math.min(sineValue*1.5-0.25, 1.0),
				(float)sineValue);

	        data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+0] = (byte) c.getRed();
	        data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+1] = (byte) c.getGreen();
	        data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+2] = (byte) c.getBlue();
	        data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+3] = (byte) 255;
	} } } }

	private int buildNoiseTexture()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();

		byte[] data = new byte[noiseHeight*noiseWidth*noiseDepth*4];
		
		fillDataArray(data);

		ByteBuffer bb = Buffers.newDirectByteBuffer(data);

		int[] textureIDs = new int[1];
		gl.glGenTextures(1, textureIDs, 0);
		int textureID = textureIDs[0];

		gl.glBindTexture(GL_TEXTURE_3D, textureID);

		gl.glTexStorage3D(GL_TEXTURE_3D, 1, GL_RGBA8, noiseWidth, noiseHeight, noiseDepth);
		gl.glTexSubImage3D(GL_TEXTURE_3D, 0, 0, 0, 0,
				noiseWidth, noiseHeight, noiseDepth, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8_REV, bb);
		
		gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

		return textureID;
	}

	void generateNoise()
	{	for (int x=0; x<noiseWidth; x++)
		{	for (int y=0; y<noiseHeight; y++)
			{	for (int z=0; z<noiseDepth; z++)
				{	noise[x][y][z] = random.nextDouble();
	}	}	}	}
	
	double smoothNoise(double x1, double y1, double z1)
	{	//get fractional part of x, y, and z
		double fractX = x1 - (int) x1;
		double fractY = y1 - (int) y1;
		double fractZ = z1 - (int) z1;

		//neighbor values
		int x2 = ((int)x1 + noiseWidth + 1) % noiseWidth;
		int y2 = ((int)y1 + noiseHeight+ 1) % noiseHeight;
		int z2 = ((int)z1 + noiseDepth + 1) % noiseDepth;

		//smooth the noise by interpolating
		double value = 0.0;
		value += (1-fractX) * (1-fractY) * (1-fractZ) * noise[(int)x1][(int)y1][(int)z1];
		value += (1-fractX) * fractY     * (1-fractZ) * noise[(int)x1][(int)y2][(int)z1];
		value += fractX     * (1-fractY) * (1-fractZ) * noise[(int)x2][(int)y1][(int)z1];
		value += fractX     * fractY     * (1-fractZ) * noise[(int)x2][(int)y2][(int)z1];

		value += (1-fractX) * (1-fractY) * fractZ     * noise[(int)x1][(int)y1][(int)z2];
		value += (1-fractX) * fractY     * fractZ     * noise[(int)x1][(int)y2][(int)z2];
		value += fractX     * (1-fractY) * fractZ     * noise[(int)x2][(int)y1][(int)z2];
		value += fractX     * fractY     * fractZ     * noise[(int)x2][(int)y2][(int)z2];
		
		return value;
	}

	private double turbulence(double x, double y, double z, double size)
	{	double value = 0.0, initialSize = size;
		while(size >= 0.9)
		{	value = value + smoothNoise(x/size, y/size, z/size) * size;
			size = size / 2.0;
		}
		value = 128.0 * value / initialSize;
		return value;
	}

	private double logistic(double x)
	{	double k = 3.0;
		return (1.0/(1.0+Math.pow(2.718,-k*x)));
	}
	
	public static void main(String[] args) { new Starter(); }
	public void dispose(GLAutoDrawable drawable) {}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);
		setupShadowBuffers();
	}
}