package a4;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.lang.Math;
import org.joml.*;

public class PanDownCommand extends AbstractAction{
	
	private Camera target;

	
	public PanDownCommand(Camera c){
		super("PanRight");
		target = c;
	}
	
	public void actionPerformed(ActionEvent ae){
		//we rotate around the X axis to simulate panning to the up / down
		Vector3f rotateAboutX = new Vector3f((float)Math.toRadians(10.0), 0.0f, 0.0f);
		target.incrementRotation(rotateAboutX);
	}
}