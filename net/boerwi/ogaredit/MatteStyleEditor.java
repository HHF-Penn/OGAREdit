package net.boerwi.ogaredit;

import javax.swing.*;
import java.awt.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.HashMap;

import net.boerwi.extrawidgets.HelpIcon;

class MatteStyleEditor extends ResEditor{
	OE_MatteStyle target;
	private JButton btEdgeColor = new JButton("Set Color");
	class StyleEditor implements ChangeListener, ActionListener{
		HashMap<Object, String> mapping = new HashMap<>();
		public void enroll(Object o, String ident){
			mapping.put(o, ident);
			if(o instanceof JButton){
				((JButton)o).addActionListener(this);
			}else if(o instanceof JSpinner){
				((JSpinner)o).addChangeListener(this);
			}else if(o instanceof JComboBox){
				((JComboBox)o).addActionListener(this);
			}else{
				assert false : "Unimplemented widget listener";
			}
		}
		public void update(Object s){
			OE_MatteStyle target = MatteStyleEditor.this.target;
			String[] c = mapping.get(s).split("\\^");
			int lidx = Integer.parseInt(c[0]);
			if(c[1].equals("width")){
				target.setWidth(lidx, (Integer)((JSpinner)s).getValue());
			}else if(c[1].equals("height")){
				target.setHeight(lidx, (Integer)((JSpinner)s).getValue());
			}else if(c[1].equals("standoff")){
				target.setStandoff(lidx, (Integer)((JSpinner)s).getValue());
			}else if(c[1].equals("color")){
				ColorPicker cp = new ColorPicker(target.layers[lidx].color);
				cp.show();
				target.setColor(lidx, cp.getColor());
				((JButton)s).setBackground(cp.getColor().toAwtColor());
			}else if(c[1].equals("bevel")){
				target.setBevel(lidx, 10*(Integer)((JComboBox)s).getSelectedItem());
			}else{
				assert false : "Unknown widget identifier";
			}
		}
		public void stateChanged(ChangeEvent evt){
			update(evt.getSource());
		}
		public void actionPerformed(ActionEvent evt){
			update(evt.getSource());
		}
	}
	StyleEditor se = new StyleEditor();
	public MatteStyleEditor(OE_MatteStyle s){
		target = s;
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		JPanel edgeColorPanel = new JPanel();
		edgeColorPanel.add(new JLabel("Edge Color:"));
		btEdgeColor.setBackground(target.edgeColor.toAwtColor());
		btEdgeColor.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				ColorPicker cp = new ColorPicker(target.edgeColor);
				cp.show();
				target.setEdgeColor(cp.getColor());
				btEdgeColor.setBackground(cp.getColor().toAwtColor());
			}
		});
		edgeColorPanel.add(HelpIcon.wrapComponent(btEdgeColor, "Color of the outermost matte edge which connects to the wall", null));
		JPanel layerPanel = new JPanel();
		layerPanel.setLayout(new GridLayout(target.LAYERCOUNT+1,5,5,5));
		layerPanel.add(new JLabel("Matte Color", JLabel.CENTER));
		layerPanel.add(new JLabel("<html>Left and Right<br>Border Widths (mm)</html>", JLabel.CENTER));
		layerPanel.add(new JLabel("<html>Top and Bottom<br>Border Widths (mm)</html>", JLabel.CENTER));
		layerPanel.add(new JLabel("<html>Bevel Angle (deg)</html>", JLabel.CENTER));
		layerPanel.add(new JLabel("<html>Standoff (mm)</html>", JLabel.CENTER));
		for(int lidx = 0; lidx < target.LAYERCOUNT; lidx++){
			JButton cbt = new JButton("Set Color");
			cbt.setBackground(target.layers[lidx].color.toAwtColor());
			JSpinner spWidth = new JSpinner();
			JSpinner spHeight = new JSpinner();
			JComboBox<Integer> cbBevelAngle = new JComboBox<>();
			cbBevelAngle.addItem(Integer.valueOf(45));
			cbBevelAngle.addItem(Integer.valueOf(90));
			cbBevelAngle.setSelectedItem(Integer.valueOf(target.layers[lidx].cdegBevelAngle/10));
			JSpinner spStandoff = new JSpinner();
			se.enroll(cbt, ""+lidx+"^color");
			se.enroll(spWidth, ""+lidx+"^width");
			se.enroll(spHeight, ""+lidx+"^height");
			se.enroll(cbBevelAngle, ""+lidx+"^bevel");
			se.enroll(spStandoff, ""+lidx+"^standoff");
			spWidth.setModel(new SpinnerNumberModel(target.layers[lidx].mmWidth, 0, 1000, 1));
			spHeight.setModel(new SpinnerNumberModel(target.layers[lidx].mmHeight, 0, 1000, 1));
			spStandoff.setModel(new SpinnerNumberModel(target.layers[lidx].mmStandoff, 0, 100, 1));
			layerPanel.add(cbt);
			layerPanel.add(spWidth);
			layerPanel.add(spHeight);
			layerPanel.add(cbBevelAngle);
			layerPanel.add(spStandoff);
		}
		panel.add(edgeColorPanel);
		panel.add(layerPanel);
	}
	public OE_MatteStyle getTarget(){
		return target;
	}
}
