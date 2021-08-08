package a4;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import org.joml.*;

public class LeftCommand extends AbstractAction{
	
	private Camera target;
	
	public LeftCommand(Camera c){
		super("Left");
		target = c;
	}
	
	public void actionPerformed(ActionEvent ae){
		//decrement camera position by 20% of the UVec		
		target.moveCameraDFL(target.getUVec().normalize(0.2f));
		// set UVec back to normal 1.0 length
		target.getUVec().normalize(1.0f);
	}
}