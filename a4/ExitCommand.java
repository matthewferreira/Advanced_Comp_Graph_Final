package a4;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;


public class ExitCommand extends AbstractAction{
	
	Starter target;
	
	public ExitCommand(Starter s){
		super("Exit");
		target = s;
	}
	
	public void actionPerformed(ActionEvent ae){
		//close program
		System.exit(0);
	}
}