package a4;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.lang.Math;
import org.joml.*;

public class ToggleFogCommand extends AbstractAction{
	
	private Starter target;
	
	public ToggleFogCommand(Starter s){
		super("ToggleFog");
		target = s;
	}
	// changes axesBool in starter to opposite value
	public void actionPerformed(ActionEvent ae){
		boolean fogBool = target.getFogBool();
		
		if(fogBool)
		{
			target.setFogBool(false);
		}
		else
		{
			target.setFogBool(true);
		}
	}
}