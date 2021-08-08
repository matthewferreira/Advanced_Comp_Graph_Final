package a4;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import org.joml.*;
//this command makes the camera move backward
public class BackwardCommand extends AbstractAction{
	
	private Camera target;
	
	public BackwardCommand(Camera c){
		super("Backward");
		target = c;
	}
	
	public void actionPerformed(ActionEvent ae){
		//move camera backward a little bit
		target.moveCameraBRU(target.getNVec().normalize(0.2f));
		// set N vector back to 1.0 length
		target.getNVec().normalize(1.0f);
	}
}