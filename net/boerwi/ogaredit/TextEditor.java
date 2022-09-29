package net.boerwi.ogaredit;

import javax.swing.*;
import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;
import java.awt.*;

class TextEditor extends ResEditor{
	OE_Text target;
	public TextEditor(OE_Text s){
		target = s;
		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		JTextArea tbox = new JTextArea(target.getData());
		tbox.addCaretListener(new CaretListener(){
			public void caretUpdate(CaretEvent e){
				s.setData(tbox.getText());
			}
		});
		JScrollPane tpane = new JScrollPane(tbox);
		panel.add(tpane, BorderLayout.CENTER);
	}
	public OE_Text getTarget(){
		return target;
	}
}
