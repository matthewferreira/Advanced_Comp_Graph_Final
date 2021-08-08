package a4;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import org.joml.*;

public class UpCommand extends AbstractAction{
	
	private Camera target;
	
	public UpCommand(Camera c){
		super("Up");
		target = c;
	}
	
	public void actionPerformed(ActionEvent ae){
		//move camera forward a little bit
		target.moveCameraBRU(target.getVVec().normalize(0.2f));
		// set V vec to 1.0 length
		target.getVVec().normalize(1.0f);
	}
}