package net.boerwi.ogaredit;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;

/**Links multiple swing jbuttons in a radio-select type configuration with toggled text
 * 
 */
public class RadioJButtonMgr{
	int defaultState;
	int currentState;
	RadioJButtonMgrInst current = null;
	ArrayList<RadioJButtonMgrInst> buttons = new ArrayList<>();
	ArrayList<ChangeListener> listeners = new ArrayList<>();
	public RadioJButtonMgr(int defaultState){
		this.defaultState = defaultState;
		this.currentState = defaultState;
	}
	public JButton addButton(JButton b, String inactiveText, String activeText, int state){
		buttons.add(new RadioJButtonMgrInst(b, inactiveText, activeText, state, this));
		return b;
	}
	public int getState(){
		return currentState;
	}
	public void activate(RadioJButtonMgrInst target){
		if(current == target) return;
		current = target;
		Iterator<RadioJButtonMgrInst> bIter = buttons.iterator();
		while(bIter.hasNext()){
			RadioJButtonMgrInst curr = bIter.next();
			if(curr == target){
				curr.activate();
			}else{
				curr.deactivate();
			}
		}
		if(target != null){
			currentState = target.state;
		}else{
			currentState = defaultState;
		}
		fireChange();
	}
	public void addChangeListener(ChangeListener l){
		listeners.add(l);
	}
	void fireChange(){
		ChangeEvent c = new ChangeEvent(this);
		Iterator<ChangeListener> listenIter = listeners.iterator();
		while(listenIter.hasNext()){
			listenIter.next().stateChanged(c);
		}
	}
}
class RadioJButtonMgrInst implements ActionListener{
	JButton b;
	String inactiveText;
	String activeText;
	int state;
	boolean active = false;
	RadioJButtonMgr owner;
	RadioJButtonMgrInst(JButton b, String inactiveText, String activeText, int state, RadioJButtonMgr owner){
		this.b = b;
		this.inactiveText = inactiveText;
		this.activeText = activeText;
		this.state = state;
		this.owner = owner;
		b.setText(inactiveText);
		b.addActionListener(this);
	}
	void deactivate(){
		b.setText(inactiveText);
		active = false;
	}
	void activate(){
		b.setText(activeText);
		active = true;
	}
	public void actionPerformed(ActionEvent e){
		if(active){
			owner.activate(null);
		}else{
			owner.activate(this);
		}
	}
}
