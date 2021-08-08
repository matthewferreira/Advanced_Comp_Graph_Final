package a4;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import org.joml.*;

public class RightCommand extends AbstractAction{
	
	private Camera target;

	
	public RightCommand(Camera c){
		super("Right");
		target = c;
	}
	
	public void actionPerformed(ActionEvent ae){
		//increment camera position by 20% of the UVec
		target.moveCameraBRU(target.getUVec().normalize(0.2f));
		// set UVec back to normal 1.0 length
		target.getUVec().normalize(1.0f);
	}
}