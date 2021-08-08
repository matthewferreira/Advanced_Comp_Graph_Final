package a4;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import org.joml.*;

public class DownCommand extends AbstractAction{
	
	private Camera target;
	
	public DownCommand(Camera c){
		super("Down");
		target = c;
	}
	
	public void actionPerformed(ActionEvent ae){
		//move camera down a little bit
		target.moveCameraDFL(target.getVVec().normalize(0.2f));
		// set V vector back to 1.o length
		target.getVVec().normalize(1.0f);
	}
}