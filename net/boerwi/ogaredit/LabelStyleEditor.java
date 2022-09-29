package net.boerwi.ogaredit;

import java.util.Map;

import javax.swing.*;
import java.awt.*;
import javax.swing.event.*;
import java.awt.event.*;

class LabelStyleEditor extends ResEditor{
	OE_LabelStyle target;
	private JComboBox<OE_LabelStyle.Cardinal> cbLabelPosition = new JComboBox<>();
	private JComboBox<OE_LabelStyle.VAlignment> cbVerticalAlignment = new JComboBox<>();
	private JButton btBGColor = new JButton("Set Color");
	public LabelStyleEditor(OE_LabelStyle s){
		target = s;
		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		JPanel propPaneContainer = new JPanel();
		JPanel propPane = new JPanel(new GridLayout(3,2,5,5));
		propPaneContainer.add(propPane);
		panel.add(propPaneContainer, BorderLayout.NORTH);
		for(OE_LabelStyle.Cardinal v : OE_LabelStyle.Cardinal.values()){
			cbLabelPosition.addItem(v);
		}
		for(OE_LabelStyle.VAlignment v : OE_LabelStyle.VAlignment.values()){
			cbVerticalAlignment.addItem(v);
		}
		ActionListener aListener = new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				Object src = evt.getSource();
				if(src == btBGColor){
					String cName = evt.getActionCommand();
					String currentColorHex = target.getBGColor().getHex(false);
					ColorPicker cp = new ColorPicker(new OE_Color(currentColorHex));
					cp.show();
					target.setBGColor(cp.getColor());
					btBGColor.setBackground(cp.getColor().toAwtColor());
				}else if(src == cbLabelPosition){
					target.setTextSide((OE_LabelStyle.Cardinal) cbLabelPosition.getSelectedItem());
				}else if(src == cbVerticalAlignment){
					target.setVAlign((OE_LabelStyle.VAlignment) cbVerticalAlignment.getSelectedItem());
				}
			}
		};
		propPane.add(new JLabel("Label position:", JLabel.RIGHT));
		propPane.add(cbLabelPosition);
		cbLabelPosition.setSelectedItem(target.getTextSide());
		cbLabelPosition.addActionListener(aListener);
		propPane.add(new JLabel("Label vertical alignment:", JLabel.RIGHT));
		propPane.add(cbVerticalAlignment);
		cbVerticalAlignment.setSelectedItem(target.getVAlign());
		cbVerticalAlignment.addActionListener(aListener);
		propPane.add(new JLabel("Background color:", JLabel.RIGHT));
		propPane.add(btBGColor);
		btBGColor.setBackground(target.getBGColor().toAwtColor());
		btBGColor.addActionListener(aListener);

		final JPanel textTypePane = new JPanel();
		panel.add(textTypePane, BorderLayout.CENTER);
		final GridBagLayout gridbag = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();
		textTypePane.setLayout(gridbag);
		class InsertMgr{
			GridBagLayout gridbag;
			GridBagConstraints c;
			JPanel p;
			public InsertMgr(GridBagLayout gridbag, GridBagConstraints c, JPanel p){
				this.gridbag = gridbag;
				this.c = c;
				this.p = p;
			}
			public void addElement(Component target){
				gridbag.setConstraints(target, c);
				p.add(target);
			}
		}
		InsertMgr i = new InsertMgr(gridbag, c, textTypePane);
		c.weightx = 1.0;
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 1; // Skip first cell
		i.addElement(new JLabel("Color", JLabel.CENTER));
		c.gridx = GridBagConstraints.RELATIVE; // Resume normal placement after skipping the first cell
		i.addElement(new JLabel("↔ Align", JLabel.CENTER));
		c.gridwidth = 2;
		c.weightx = 1.5;
		i.addElement(new JLabel("Size", JLabel.CENTER));
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		i.addElement(new JLabel("Style Override", JLabel.LEFT));
		class StyleEditor implements ChangeListener, ActionListener{
			private JComboBox<OE_LabelStyle.HAlignment> cbHorizontalAlignment = new JComboBox<>();
			private JSpinner spFontSize = new JSpinner();
			private JButton btTextColor = new JButton("Text Color");
			private JCheckBox
				cbUL = new JCheckBox("Underline"),
				cbBD = new JCheckBox("Bold"),
				cbIT = new JCheckBox("Italic");
			private String sName;
			private OE_LabelStyle.TextStyle current;
			public StyleEditor(String sName, OE_LabelStyle.TextStyle value){
				this.sName = sName;
				c.gridwidth = GridBagConstraints.REMAINDER;
				i.addElement(new JSeparator(SwingConstants.HORIZONTAL));
				c.gridwidth = 1;
				i.addElement(new JLabel(sName, JLabel.RIGHT));
				i.addElement(btTextColor);
				i.addElement(cbHorizontalAlignment);
				for(OE_LabelStyle.HAlignment v : OE_LabelStyle.HAlignment.values()){
					cbHorizontalAlignment.addItem(v);
				}
				i.addElement(spFontSize);
				spFontSize.setModel(new SpinnerNumberModel(12.0, 5.0, 100.0, 0.5));
				c.weightx = 0.5;
				i.addElement(new JLabel("pt"));
				c.weightx = 1.0;
				JPanel styleOverridePane = new JPanel(new GridLayout(3, 1));
				c.gridwidth = GridBagConstraints.REMAINDER;
				i.addElement(styleOverridePane);
				styleOverridePane.add(cbUL);
				styleOverridePane.add(cbBD);
				styleOverridePane.add(cbIT);
				setState();
				cbUL.addActionListener(this);
				cbBD.addActionListener(this);
				cbIT.addActionListener(this);
				cbHorizontalAlignment.addActionListener(this);
				spFontSize.addChangeListener(this);
				btTextColor.addActionListener(this);
			}
			private boolean updatingState = false;
			public void stateChanged(ChangeEvent evt){
				if(updatingState) return;
				if(evt.getSource() == spFontSize){
					target.setStyle(sName, target.new TextStyle(current.getColor(), current.getHAlign(), (double)spFontSize.getValue(), current.getStyle().isUnderline(), current.getStyle().isItalic(), current.getStyle().isBold()));
				}
				setState();
			}
			public void actionPerformed(ActionEvent evt){
				if(updatingState) return;
				final Object src = evt.getSource();
				if(src == cbUL || src == cbBD || src == cbIT){
					target.setStyle(sName, target.new TextStyle(current.getColor(), current.getHAlign(), current.getPtSize(), cbUL.isSelected(), cbIT.isSelected(), cbBD.isSelected()));
				}else if(src == cbHorizontalAlignment){
					target.setStyle(sName, target.new TextStyle(current.getColor(), (OE_LabelStyle.HAlignment)cbHorizontalAlignment.getSelectedItem(), current.getPtSize(), current.getStyle().isUnderline(), current.getStyle().isItalic(), current.getStyle().isBold()));
				}else if(src == btTextColor){
					String currentColorHex = current.getColor().getHex(false);
					ColorPicker cp = new ColorPicker(new OE_Color(currentColorHex));
					cp.show();
					target.setStyle(sName, target.new TextStyle(cp.getColor(), current.getHAlign(), current.getPtSize(), current.getStyle().isUnderline(), current.getStyle().isItalic(), current.getStyle().isBold()));
				}
				setState();
			}
			private void setState(){
				updatingState = true;
				OE_LabelStyle.TextStyle style = target.getStyle(sName);
				current = style;
				spFontSize.setValue(Double.valueOf(style.getPtSize()));
				cbHorizontalAlignment.setSelectedItem(style.getHAlign());
				btTextColor.setBackground(style.getColor().toAwtColor());
				OE_LabelStyle.TextStyle.RichStyle rs = style.getStyle();
				cbUL.setSelected(rs.isUnderline());
				cbBD.setSelected(rs.isBold());
				cbIT.setSelected(rs.isItalic());
				updatingState = false;
			}
		}
		for(Map.Entry<String, OE_LabelStyle.TextStyle> entry : target.getStyles()){
			new StyleEditor(entry.getKey(), entry.getValue());
		}
	}
	public OE_LabelStyle getTarget(){
		return target;
	}
}
