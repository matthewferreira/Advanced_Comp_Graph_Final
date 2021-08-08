package a4;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import org.joml.*;

public class ForwardCommand extends AbstractAction{
	
	private Camera target;
	
	public ForwardCommand(Camera c){
		super("Forward");
		target = c;
	}
	
	public void actionPerformed(ActionEvent ae){
		//move camera forward a little bit
		target.moveCameraDFL(target.getNVec().normalize(0.2f));
		//set N vector back to 1.0 length
		target.getNVec().normalize(1.0f);
	}
}