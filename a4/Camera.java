package a4;

import java.lang.Math;
import org.joml.*;

public class Camera{

	private Vector4f uVec; // left right x position
	private Vector4f vVec; // up down y position
	private Vector4f nVec; // forward / backward z position
	private Vector4f cVec; // camera position
	private Vector3f rotation;
	
	public Camera(){
		
		uVec = new Vector4f(); 
		uVec.set(1,0,0,0);
		
		vVec = new Vector4f(); 
		vVec.set(0,1,0,0);
		
		nVec = new Vector4f(); 
		nVec.set(0,0,1,0);
		
		cVec = new Vector4f(); 
		cVec.set(0,0,12,1);
		
		rotation = new Vector3f();
		rotation.set(0,0, 0);
	}
	
	public Vector4f getUVec(){
		return uVec;
	}
	
	public Vector4f getVVec(){
		return vVec;
	}
	
	public Vector4f getNVec(){
		return nVec;
	}
	
	public Vector4f getCVec(){
		return cVec;
	}
	
	//since we're just adding/subtracting unit vectors  to/from the C vector we can move 3 directions with one method
	// move camera down/forward/left
	public void moveCameraDFL(Vector4f scaledVector){
		cVec.sub(scaledVector);
	}
	
	// move camera back / right / up
	public void moveCameraBRU(Vector4f scaledVector){
		cVec.add(scaledVector);
	}
	
	// increment the specified axis
	public void incrementRotation(Vector3f angles){
		rotation.add(angles);
	}
	// decrement specified axis
	public void decrementRotation(Vector3f angles){
		rotation.sub(angles);
	}
	
	public Vector3f getRotationVec(){
		return rotation;
	}
}