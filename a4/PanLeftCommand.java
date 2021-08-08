package a4;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.lang.Math;
import org.joml.*;

public class PanLeftCommand extends AbstractAction{
	
	private Camera target;

	
	public PanLeftCommand(Camera c){
		super("PanLeft");
		target = c;
	}
	
	public void actionPerformed(ActionEvent ae){
		//we rotate around the Y axis to simulate panning to the right / left
		Vector3f rotateAboutY = new Vector3f(0.0f, (float)Math.toRadians(10), 0.0f);
		target.decrementRotation(rotateAboutY);
	}
}