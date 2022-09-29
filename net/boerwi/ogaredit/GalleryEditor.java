package net.boerwi.ogaredit;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import java.awt.event.*;
import java.util.*;

import net.boerwi.extrawidgets.HelpIcon;

class GalleryEditor extends ResEditor{
	final OE_Gallery target;
	final GridView gv = new GridView();
	final Color backgroundButtonColor;
	final TreeMap<String, JButton> colorButtons = new TreeMap<>();
	final OE_ResourceSelector resSelector;
	final JButton btSetFloorplan;
	final JCheckBox cbClickExamine = new JCheckBox("Click-to-Examine");
	final JCheckBox cbAutoplayAudio = new JCheckBox("Autoplay Audio");
	final JSpinner spExportRes;
	public GalleryEditor(OE_Gallery s, OE_ResourceSelector resSelector){
		target = s;
		this.resSelector = resSelector;
		panel = new JPanel(new BorderLayout());
		JPanel topPane = new JPanel(new GridLayout(1, 3, 5, 5));
		panel.add(topPane, BorderLayout.NORTH);
		panel.add(gv, BorderLayout.CENTER);
		Set<String> colorNames = target.getColorNames();
		JPanel colorPane = new JPanel(new GridLayout(colorNames.size(), 1));
		colorPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Colors"));
		ActionListener colorButtonListener = new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				String cName = evt.getActionCommand();
				String currentColorHex = target.getColor(cName).getHex(false);
				ColorPicker cp = new ColorPicker(new OE_Color(currentColorHex));
				cp.show();
				target.setColor(cName, cp.getColor().getHex(false));
				updateButtons();
			}
		};
		for(String colorName : colorNames){
			JPanel colorRow = new JPanel(new GridLayout(1, 2));
			colorRow.add(new JLabel(colorName+" Color: ", JLabel.CENTER));
			JButton cButton = new JButton("set");
			cButton.addActionListener(colorButtonListener);
			cButton.setActionCommand(colorName);
			colorButtons.put(colorName, cButton);
			colorRow.add(cButton);
			colorPane.add(colorRow);
		}
		backgroundButtonColor = colorButtons.get(colorButtons.firstKey()).getBackground();
		topPane.add(colorPane);
		btSetFloorplan = new JButton("Set Floorplan");
		btSetFloorplan.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				if(target.getFloorplan() != null){
					int confirm = JOptionPane.showConfirmDialog(null, "Removing floorplan will remove all art placements in this gallery!\nConfirm remove?", "Confirm Change Floorplan", JOptionPane.YES_NO_OPTION);
					if(confirm == JOptionPane.NO_OPTION) return;
					target.setFloorplan(null);
					updateFloorplanView();
				}else{
					OE_Floorplan fp = (OE_Floorplan)resSelector.singleSelect(OE_ResType.FLOORPLAN);
					target.setFloorplan(fp);
					updateFloorplanView();
				}
				updateButtons();
			}
		});
		JPanel optionPane = new JPanel(new GridLayout(3, 1));
		topPane.add(optionPane);
		topPane.add(btSetFloorplan);
		optionPane.add(cbClickExamine);
		optionPane.add(cbAutoplayAudio);
		ActionListener cbListener = new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				if(evt.getSource() == cbClickExamine){
					target.setCanClickToExamine(cbClickExamine.isSelected());
				}else if(evt.getSource() == cbAutoplayAudio){
					target.setAutoplayAudio(cbAutoplayAudio.isSelected());
				}
				updateButtons();
			}
		};
		cbClickExamine.addActionListener(cbListener);
		cbAutoplayAudio.addActionListener(cbListener);
		spExportRes = new JSpinner(new SpinnerNumberModel(target.getPxPerMM(), 0.1, 12.0, 0.1));
		optionPane.add(HelpIcon.wrapComponent(spExportRes, """
		<html>\
		<p>This controls the resolution of exported images. Higher values result in larger final image size.</p>\
		<p>Images will not exceed their source resolution or 4096 in either dimension.</p>\
		<p>100 DPI is equal to 3.93 px/mm, but this is a higher than recommended resolution.</p>\
		</html>
		"""
		, BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "px/mm Image Resolution")));
		spExportRes.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e){
				target.setPxPerMM((double)(spExportRes.getValue()));
			}
		});
		updateButtons();
		updateFloorplanView();
	}
	public void openCurationPopup(GV_Line wall, boolean onLeft){
		new CurationPopup(wall, onLeft, this);
	}
	void updateFloorplanView(){
		OE_Floorplan fp = target.getFloorplan();
		gv.clearComponents();
		gv.setGridTool(null);
		if(fp != null){
			for(GV_Line w : fp.walls){
				gv.addComponent(w);
			}
			gv.addComponent(fp.getAvatar());
			gv.setGridTool(new WallSelectTool(gv, this));
			if(fp.isThickWalls()){
				for(GV_Line w : fp.getThickWalls()){
					w.setColor(Color.MAGENTA);
					gv.addComponent(w);
				}
			}
		}
	}
	void updateButtons(){
		for(String cname : colorButtons.keySet()){
			JButton b = colorButtons.get(cname);
			b.setEnabled(true);
			b.setBackground(target.getColor(cname).toAwtColor());
		}
		cbClickExamine.setSelected(target.getCanClickToExamine());
		if(target.getFloorplan() == null){
			btSetFloorplan.setText("Set Floorplan");
		}else{
			btSetFloorplan.setText("Remove Floorplan");
		}
		cbClickExamine.setSelected(target.getCanClickToExamine());
		cbAutoplayAudio.setSelected(target.getAutoplayAudio());
		// You can only set autoplay audio if click to examine is enabled.
		if(target.getCanClickToExamine()){
			cbAutoplayAudio.setEnabled(true);
		}else{
			cbAutoplayAudio.setEnabled(false);
		}
	}
	public OE_Gallery getTarget(){
		return target;
	}
}
class CurationPopup{
	final JButton
		btDeleteArt = new JButton("Delete Art"),
		btMatteAssociate = new JButton(),
		btTextAssociate = new JButton(),
		btAudioAssociate = new JButton();
	final JSpinner
		spCenterX = new JSpinner(),
		spCenterY = new JSpinner(),
		spScale = new JSpinner();
	final JComponent[] selectInterfaces = new JComponent[]{
		btDeleteArt, btMatteAssociate, btTextAssociate, btAudioAssociate,
		spCenterX, spCenterY, spScale};
	ArrayList<ArtPlacement> art = new ArrayList<>();
	ArtPlacement selected = null;
	GV_Line wall;
	boolean onLeft;
	GalleryEditor owner;
	final double cmDefaultX, cmDefaultY;
	final int mmWallX, mmWallY;
	final boolean[] spinnersEnabled = new boolean[]{true};
	CurationPopup(GV_Line wall, boolean onLeft, GalleryEditor owner){
		this.owner = owner;
		this.wall = wall;
		this.onLeft = onLeft;
		refreshArt();
		mmWallX = (int)wall.distance();
		mmWallY = owner.target.getFloorplan().getWallHeight()*10;
		WallCurator wallView = new WallCurator(owner.target.getColor("Walls"), mmWallX, mmWallY, this);
		JDialog curationDialog = new JDialog(null, "Wall Curator", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
		curationDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		Container content = curationDialog.getContentPane();
		content.setLayout(new BorderLayout());
		JPanel topPane = new JPanel(new GridLayout(5, 1, 0, 10));
		JPanel topPaneRow1 = new JPanel(new GridLayout(1, 2, 10, 0));
		Box topPaneRow2 = new Box(BoxLayout.X_AXIS);
		topPane.add(topPaneRow1);
		topPane.add(topPaneRow2);
		content.add(topPane, BorderLayout.NORTH);
		JButton btAddArt = new JButton("Add Artworks");
		btAddArt.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				OE_Resource[] res = owner.resSelector.multiSelect(OE_ResType.IMAGE);
				for(OE_Resource img : res){
					ArtPlacement p = owner.target.addArt(wall, onLeft, (OE_Image)img);
					// Put the art at eye height (it defaults to the center)
					owner.target.relocateArt(p, 0.5, owner.target.getFloorplan().getEyeHeight()*10.0/(double)mmWallY);
				}
				refreshArt();
				wallView.repaint();
			}
		});
		topPaneRow1.add(btAddArt);
		btDeleteArt.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				assert selected != null;
				owner.target.delArt(wall, selected);
				selected = null;
				refreshArt();
				updateButtons();
				wallView.repaint();
			}
		});
		topPaneRow1.add(btDeleteArt);
		cmDefaultX = mmWallX*0.05;
		cmDefaultY = mmWallY*0.05;
		
		ChangeListener spCenterListener = new ChangeListener(){
			public void stateChanged(ChangeEvent evt){
				if(spinnersEnabled[0]){
					relocateSelected((double)spCenterX.getValue()*10.0/mmWallX, (double)spCenterY.getValue()*10.0/mmWallY, (double)spScale.getValue());
					wallView.repaint();
				}
			}
		};
		spCenterX.setModel(new SpinnerNumberModel(cmDefaultX, 0, mmWallX*0.1, 0.1));
		spCenterX.addChangeListener(spCenterListener);
		spCenterY.setModel(new SpinnerNumberModel(cmDefaultY, 0, mmWallY*0.1, 0.1));
		spCenterY.addChangeListener(spCenterListener);
		spScale.setModel(new SpinnerNumberModel(1.0, 0.001, 1000.0, 0.001));
		spScale.addChangeListener(spCenterListener);
		topPaneRow2.add(new JLabel("Scale: ", JLabel.RIGHT));
		topPaneRow2.add(spScale);
		topPaneRow2.add(new JLabel(" Center(cm): X:", JLabel.RIGHT));
		topPaneRow2.add(spCenterX);
		topPaneRow2.add(new JLabel(" Y:", JLabel.RIGHT));
		topPaneRow2.add(spCenterY);
		topPane.add(btMatteAssociate);
		topPane.add(btTextAssociate);
		topPane.add(btAudioAssociate);
		btMatteAssociate.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				if(selected.matteStyle == null){
					OE_MatteStyle sel = (OE_MatteStyle)owner.resSelector.singleSelect(OE_ResType.MATTESTYLE);
					if(sel == null) return;
					owner.target.associateRes(selected, sel);
				}else{
					owner.target.associateRes(selected, (OE_MatteStyle)null);
				}
				updateButtons();
				wallView.repaint();
			}
		});
		btTextAssociate.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				if(selected.text == null){
					OE_Text sel = (OE_Text)owner.resSelector.singleSelect(OE_ResType.TEXT);
					if(sel == null) return;
					OE_LabelStyle styleSel = (OE_LabelStyle)owner.resSelector.singleSelect(OE_ResType.LABELSTYLE);
					if(styleSel == null) return;
					owner.target.associateRes(selected, sel, styleSel);
				}else{
					owner.target.associateRes(selected, (OE_Text)null, (OE_LabelStyle)null);
				}
				updateButtons();
				wallView.repaint();
			}
		});
		btAudioAssociate.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				if(selected.audio == null){
					OE_Audio sel = (OE_Audio)owner.resSelector.singleSelect(OE_ResType.AUDIO);
					if(sel == null) return;
					owner.target.associateRes(selected, sel);
				}else{
					owner.target.associateRes(selected, (OE_Audio)null);
				}
				updateButtons();
				wallView.repaint();
			}
		});
		wallView.setPreferredSize(new Dimension(400, 400));
		content.add(wallView, BorderLayout.CENTER);
		JButton btExit = new JButton("Close");
		btExit.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				curationDialog.dispose();
			}
		});
		content.add(btExit, BorderLayout.SOUTH);
		updateButtons();
		curationDialog.pack();
		curationDialog.setVisible(true);
	}
	void refreshArt(){
		art.clear();
		for(ArtPlacement a : owner.target.getArtOnWall(wall)){
			if(onLeft == a.onLeft){
				art.add(a);
			}
		}
	}
	void setSelected(ArtPlacement sel){
		selected = sel;
		System.out.println("setSelected: "+selected);
		updateButtons();
	}
	void relocateSelected(double cx, double cy, double scale){
		assert selected != null;
		owner.target.relocateArt(selected, cx, cy, scale);
		updateButtons();
	}
	void relocateSelected(double cx, double cy){
		assert selected != null;
		owner.target.relocateArt(selected, cx, cy);
		updateButtons();
	}
	void updateButtons(){
		// Disable spinners while we are triggering stuff
		spinnersEnabled[0] = false;
		if(selected == null){
			spCenterX.setValue(cmDefaultX);
			spCenterY.setValue(cmDefaultY);
			spScale.setValue(1.0);
			for(JComponent comp : selectInterfaces){
				comp.setEnabled(false);
			}
			btMatteAssociate.setText("Associate Matte");
			btTextAssociate.setText("Associate Text");
			btAudioAssociate.setText("Associate Audio");
		}else{
			spCenterX.setValue(selected.cx * mmWallX * 0.1);
			spCenterY.setValue(selected.cy * mmWallY * 0.1);
			spScale.setValue(selected.scale);
			if(selected.matteStyle == null){
				btMatteAssociate.setText("Associate Matte");
			}else{
				btMatteAssociate.setText("Remove Matte \""+selected.matteStyle.toString()+"\"");
			}
			if(selected.text == null){
				btTextAssociate.setText("Associate Text");
			}else{
				btTextAssociate.setText("Remove Text \""+selected.text.toString()+"\" (Style \""+selected.labelStyle+"\")");
			}
			if(selected.audio == null){
				btAudioAssociate.setText("Associate Audio");
			}else{
				btAudioAssociate.setText("Remove Audio \""+selected.audio.toString()+"\"");
			}
			for(JComponent comp : selectInterfaces){
				comp.setEnabled(true);
			}
		}
		spinnersEnabled[0] = true;
	}
}
@SuppressWarnings("serial") // We don't support serialization
class WallCurator extends JComponent implements MouseInputListener{
	AffineTransform pxT, realT, invRealT;
	int mmWallWidth, mmWallHeight;
	Color wallColor;
	CurationPopup owner;
	int[] mmMouse = null;
	int[] dragOffset = null;
	WallCurator(OE_Color wallColor, int mmWidth, int mmHeight, CurationPopup owner){
		this.owner = owner;
		this.wallColor = wallColor.toAwtColor();
		this.mmWallWidth = mmWidth;
		this.mmWallHeight = mmHeight;
		addMouseListener(this);
		addMouseMotionListener(this);
	}
	public void setMouseLoc(MouseEvent evt){
		int x = evt.getX();
		int y = evt.getY();
		Point2D.Double mloc = new Point2D.Double();
		invRealT.transform(new Point2D.Double(x, y), mloc);
		mmMouse = new int[]{(int)mloc.getX(), (int)mloc.getY()};
	}
	public void mouseClicked(MouseEvent e){}
	public void mouseEntered(MouseEvent e){}
	public void mouseExited(MouseEvent e){}
	public void mousePressed(MouseEvent e){
		setMouseLoc(e);
		ArtPlacement sel = null;
		for(ArtPlacement a : owner.art){
			int xD = (int)Math.abs(a.cx * mmWallWidth - mmMouse[0]);
			int yD = (int)Math.abs(a.cy * mmWallHeight - mmMouse[1]);
			if(xD < a.scale*a.img.getMMWidth()/2 && yD < a.scale*a.img.getMMHeight()/2){
				sel = a;
			}
		}
		if(sel != null){
			dragOffset = new int[]{(int)(sel.cx*mmWallWidth) - mmMouse[0], (int)(sel.cy*mmWallHeight) - mmMouse[1]};
		}
		System.out.println(sel);
		owner.setSelected(sel);
		repaint();
	}
	public void mouseReleased(MouseEvent e){
		dragOffset = null;
	}
	public void mouseMoved(MouseEvent e){}
	public void mouseDragged(MouseEvent e){
		if(owner.selected != null){
			setMouseLoc(e);
			double newCx = (mmMouse[0]+dragOffset[0])/(double)mmWallWidth;
			double newCy = (mmMouse[1]+dragOffset[1])/(double)mmWallHeight;
			owner.relocateSelected(newCx, newCy);
			repaint();
		}
	}
	public void paintComponent(Graphics g){
		Graphics2D g2 = (Graphics2D)g;
		pxT = g2.getTransform();
		// Pixel Draw Sizes
		int pxDWidth = getWidth();
		int pxDHeight = getHeight();
		// Draw Background
		g.setColor(Color.BLACK);
		g.fillRect(0,0,pxDWidth, pxDHeight);
		// Compute the transform for wall coordinates
		double scale = Math.min(pxDWidth/(double)mmWallWidth, pxDHeight/(double)mmWallHeight);
		double xOffset = 0.5*(pxDWidth - mmWallWidth*scale);
		double yOffset = 0.5*(pxDHeight + mmWallHeight*scale);
		realT = new AffineTransform();
		realT.translate(xOffset, yOffset);
		realT.scale(scale, -scale);
		try{
			invRealT = realT.createInverse();
		}catch(Exception e){
			invRealT = new AffineTransform();
		}
		g2.transform(realT);
		g.setColor(wallColor);
		g.fillRect(0, 0, mmWallWidth, mmWallHeight);
		for(ArtPlacement a : owner.art){
			double cx = a.cx;
			double cy = a.cy;
			double artScale = a.scale;
			double mmWidth = a.img.getMMWidth()*artScale;
			double mmHeight = a.img.getMMHeight()*artScale;
			if(a.matteStyle != null){
				OE_MatteStyle m = a.matteStyle;
				double mmMatteW = mmWidth;
				double mmMatteH = mmHeight;
				for(int layeridx = 0; layeridx < m.LAYERCOUNT; layeridx++){
					mmMatteW += 2*m.layers[layeridx].mmWidth;
					mmMatteH += 2*m.layers[layeridx].mmHeight;
				}
				for(int layeridx = m.LAYERCOUNT-1; layeridx >= 0; layeridx--){
					double mmAddW = 2*m.layers[layeridx].mmWidth;
					double mmAddH = 2*m.layers[layeridx].mmHeight;
					if(mmAddW != 0 || mmAddH != 0){
						g.setColor(m.layers[layeridx].color.toAwtColor());
						g.fillRect((int)(mmWallWidth*cx-mmMatteW/2), (int)(mmWallHeight*cy-mmMatteH/2), (int)mmMatteW, (int)mmMatteH);
						mmMatteW -= mmAddW;
						mmMatteH -= mmAddH;
					}
				}
			}
			BufferedImage thumb = a.img.getCroppedThumbnail();
			AffineTransform imgT = new AffineTransform();
			imgT.translate(mmWallWidth*cx-mmWidth/2, mmWallHeight*cy+mmHeight/2);
			imgT.scale(mmWidth/thumb.getWidth(), -mmHeight/thumb.getHeight());
			g2.drawRenderedImage(thumb, imgT);
		}
		if(owner.selected != null){
			int cx = (int)(owner.selected.cx * mmWallWidth);
			int cy = (int)(owner.selected.cy * mmWallHeight);
			g.setColor(Color.RED);
			g.fillOval(cx-75, cy-75, 150, 150);
		}
	}
}
class WallSelectTool extends GridTool{
	ArrayList<GV_Line> walls;
	GalleryEditor editor;
	int[] mouse = null;
	WallSelectTool(GridView owner, GalleryEditor editor){
		super(owner);
		this.editor = editor;
		walls = editor.target.getFloorplan().walls;
	}
	GV_Line getClosestWall(){
		if(mouse == null) return null;
		GV_Line ret = null;
		double bestScore = Double.POSITIVE_INFINITY;
		for(GV_Line wall : walls){
			double cScore = wall.clickScore(mouse[0], mouse[1]);
			if(cScore < bestScore){
				bestScore = cScore;
				ret = wall;
			}
		}
		// Check if the best candidate is still too far away
		if(ret != null){
			if(20.0 < ret.distanceToPoint(mouse[0], mouse[1])*owner.zScale()){
				ret = null;
			}
		}
		return ret;
	}
	public void mousePressed(MouseEvent evt){
		mouse = owner.getMouseLoc(evt, true);
		GV_Line closestWall = getClosestWall();
		if(closestWall != null){
			editor.openCurationPopup(closestWall, closestWall.onLeft(mouse[0], mouse[1]));
		}
	}
	public void mouseMoved(MouseEvent evt){
		mouse = owner.getMouseLoc(evt, false);
		owner.repaint();
	}
	public void mouseExited(MouseEvent evt){
		mouse = null;
		owner.repaint();
	}
	public void draw(Graphics2D g){
		GV_Line closestWall = getClosestWall();
		if(closestWall != null){
			boolean onLeft = closestWall.onLeft(mouse[0], mouse[1]);
			int midX = (closestWall.p1[0]+closestWall.p2[0])/2;
			int midY = (closestWall.p1[1]+closestWall.p2[1])/2;
			double[] vec = closestWall.getVec();
			double vecLen = 20.0/owner.zScale();
			if(!onLeft) vecLen *= -1.0;
			int endX = midX-(int)(vec[1]*vecLen);
			int endY = midY+(int)(vec[0]*vecLen);
			GV_Line dirArrow = new GV_Line(midX, midY, endX, endY);
			dirArrow.setColor(Color.RED);
			dirArrow.draw(g, owner.pxTransform, owner.realTransform);
			Color oldColor = closestWall.getColor();
			closestWall.setColor(Color.RED);
			closestWall.draw(g, owner.pxTransform, owner.realTransform);
			closestWall.setColor(oldColor);
		}
	}
}
