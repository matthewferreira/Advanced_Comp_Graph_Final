package a4;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.lang.Math;
import org.joml.*;

public class PanRightCommand extends AbstractAction{
	
	private Camera target;

	
	public PanRightCommand(Camera c){
		super("PanRight");
		target = c;
	}
	
	public void actionPerformed(ActionEvent ae){
		//we rotate around the Y axis to simulate panning to the right / left
		Vector3f rotateAboutY = new Vector3f(0.0f, (float)Math.toRadians(10.0), 0.0f);
		target.incrementRotation(rotateAboutY);
		
	}
}