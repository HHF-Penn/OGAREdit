package net.boerwi.ogaredit;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.regex.*;
import net.boerwi.extrawidgets.HelpIcon;

class FloorplanEditor extends ResEditor implements ChangeListener{
	final OE_Floorplan target;
	final ArrayList<GV_Line> thickWalls = new ArrayList<>();
	final GridView gv = new GridView();
	final JPanel prop_pane = new JPanel(new GridLayout(2,3)),
		wall_pane = new JPanel(new BorderLayout()),
		region_pane = new JPanel(new BorderLayout());
	final JComboBox<GV_Hull> regionDrop = new JComboBox<>();
	final JTextField regionPointEntryField = new JTextField();
	public final static int T_NONE = 0, T_AVATAR = 1, T_NEWWALL = 2, T_MOVEWCORNER = 3, T_DELETEWALL = 4, T_NEWREGIONPOINT = 5, T_MOVEREGIONPOINT = 6, T_DELETEREGIONPOINT = 7;
	final RadioJButtonMgr funcMgr = new RadioJButtonMgr(T_NONE);
	// We need these because we need to be able to enable and disable them later on
	private JButton regionAddPts = new JButton(),
		regionMovePts = new JButton(),
		regionDelPts = new JButton(),
		deleteRegion = new JButton("Delete Region");
	private JCheckBox cbThickWalls = new JCheckBox();
	public FloorplanEditor(OE_Floorplan s){
		target = s;
		panel = new JPanel(new BorderLayout());
		JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
		tabs.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e){
				// Deactivate all tools when the tab changes
				funcMgr.activate(null);
				gv.repaint();
			}
		});
		regionDrop.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				// When editing which region is active, disable any active tool
				funcMgr.activate(null);
				gv.repaint();
			}
		});
		tabs.addTab("Properties", null, prop_pane);
		prop_pane.add(funcMgr.addButton(new JButton(), "Set Avatar Location", "Done", T_AVATAR));
		JSpinner eyeHeight = new JSpinner(new SpinnerNumberModel(target.getEyeHeight(), 50, 300, 1));
		eyeHeight.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e){
				target.setEyeHeight((int)eyeHeight.getValue());
			}
		});
		JLabel EHLabel = new JLabel("Eye Height (cm): ");
		EHLabel.setLabelFor(eyeHeight);
		EHLabel.setHorizontalAlignment(JLabel.RIGHT);
		prop_pane.add(EHLabel);
		prop_pane.add(eyeHeight);
		JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 3600, target.getAvatarDeciAngle());
		slider.setBorder(BorderFactory.createTitledBorder("Start Angle"));
		slider.setMajorTickSpacing(900);
		slider.setMinorTickSpacing(225);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.setSnapToTicks(true);
		Hashtable<Integer, JComponent> sliderLabels = slider.createStandardLabels(slider.getMajorTickSpacing());
		for(int lval = slider.getMinimum(); lval <= slider.getMaximum(); lval += slider.getMajorTickSpacing()){
			JLabel lbl = (JLabel)sliderLabels.get(lval);
			lbl.setText((lval/10)+" ");
		}
		slider.setLabelTable(sliderLabels);
		slider.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e){
				target.setAvatarDeciAngle(slider.getValue());
				gv.repaint();
			}
		});
		prop_pane.add(slider);
		JSpinner wallHeight = new JSpinner(new SpinnerNumberModel(target.getWallHeight(), 50, 1200, 5));
		wallHeight.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e){
				target.setWallHeight((int)wallHeight.getValue());
			}
		});
		JLabel WHLabel = new JLabel("Wall Height (cm): ");
		WHLabel.setLabelFor(wallHeight);
		WHLabel.setHorizontalAlignment(JLabel.RIGHT);
		prop_pane.add(WHLabel);
		prop_pane.add(wallHeight);
		tabs.addTab("Walls", null, wall_pane);
		JPanel wall_thick_pane = new JPanel(new GridLayout(1,2));
		wall_thick_pane.add(new JLabel("Thick Walls: ", JLabel.RIGHT));
		wall_thick_pane.add(cbThickWalls);
		cbThickWalls.setSelected(target.isThickWalls());
		cbThickWalls.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				target.setThickWalls(cbThickWalls.isSelected());
				reconstructThickWalls();
				gv.repaint();
			}
		});
		reconstructThickWalls();
		wall_pane.add(wall_thick_pane, BorderLayout.NORTH);
		Action submitManualAction = new AbstractAction(){
			private static final Pattern doublePattern = Pattern.compile("^-?[0-9]*([.][0-9]+)?$");
			private final Color defaultColor = (new JTextField()).getBackground();
			private static final Color badColor = new Color(250, 180, 180);
			public void actionPerformed(ActionEvent e){
				JTextField entryField = (JTextField) e.getSource();
				String actionCommand = e.getActionCommand();
				String[] coordStrs = entryField.getText().strip().split("[\\s,;]+");
				boolean good = true;
				int[] coords = new int[coordStrs.length];
				for(int idx = 0; idx < coordStrs.length; idx++){
					if( !doublePattern.matcher(coordStrs[idx]).matches() ){
						good = false;
						break;
					}
					coords[idx] = (int)(1000*Double.parseDouble(coordStrs[idx]));
				}
				if(actionCommand.equals("SUBMIT_MANUAL_WALL")){
					if(coords.length != 4){
						good = false;
					}
					if(good){
						GV_Line newWall = new GV_Line(coords[0], coords[1], coords[2], coords[3]);
						target.addWall(newWall);
						entryField.setText("");
						gv.addComponent(newWall);
						reconstructThickWalls();
					}
				}else if(actionCommand.equals("SUBMIT_MANUAL_REGION_POINT")){
					if(coords.length != 2){
						good = false;
					}
					if(good){
						target.addRegionPoint((GV_Hull)regionDrop.getSelectedItem(), coords[0], coords[1]);
						entryField.setText("");
						gv.repaint();
					}
				}else{
					assert false : "Incorrect action command: "+actionCommand;
				}
				if(good){
					entryField.setBackground(defaultColor);
				}else{
					entryField.setBackground(badColor);
				}
			}
		};
		final JTextField wallEntryField = new JTextField();
		wallEntryField.setActionCommand("SUBMIT_MANUAL_WALL");
		wallEntryField.addActionListener(submitManualAction);
		JPanel wall_button_pane = new JPanel(new GridLayout(2,2,5,5));
		wall_button_pane.add(funcMgr.addButton(new JButton(), "Draw New Walls", "Finish Draw", T_NEWWALL));
		wall_button_pane.add(funcMgr.addButton(new JButton(), "Move Corners", "Finish", T_MOVEWCORNER));
		wall_button_pane.add(HelpIcon.wrapComponent(wallEntryField,
			"""
			<html>\
			<p>This textbox can rapidly create walls if you have exact coordinates in meters.</p>\
			<p>Enter four numbers in this pattern:</p>\
			<p>`X1 Y1 X2 Y2`</p>\
			<p>Then press 'Enter' to place the wall and clear the textbox.</p>\
			</html>\
			"""
			,
			BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Manual Wall Create")
		));
		wall_button_pane.add(funcMgr.addButton(new JButton(), "Delete Walls", "Finish", T_DELETEWALL));
		wall_pane.add(wall_button_pane, BorderLayout.CENTER);
		tabs.addTab("Regions", null, region_pane);
		JPanel regionPaneTop = new JPanel(new BorderLayout());
		JPanel regionPaneTopRight = new JPanel(new GridLayout(1,2));
		JPanel regionPaneBottom = new JPanel(new GridLayout(2,2,5,5));
		region_pane.add(regionPaneTop, BorderLayout.NORTH);
		region_pane.add(regionPaneBottom, BorderLayout.CENTER);
		regionPaneTop.add(regionDrop, BorderLayout.CENTER);
		regionPaneTop.add(regionPaneTopRight, BorderLayout.EAST);
		JButton newRegion = new JButton("New Region");
		newRegion.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				String regionName = JOptionPane.showInputDialog("Please enter a name for the new region", "Unnamed Region");
				if(regionName == null) return;
				GV_Hull newRegion = new GV_Hull();
				target.addRegion(regionName, newRegion);
				// Add and select the newly-created region in the menu
				regionDrop.addItem(newRegion);
				regionDrop.setSelectedItem(newRegion);
				gv.addComponent(newRegion);
				updateRegionButtonEnable();
			}
		});
		deleteRegion.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				GV_Hull current = (GV_Hull)regionDrop.getSelectedItem();
				String confirmMessage = String.format("Remove region \"%s\"?", current.getName());
				int confirm = JOptionPane.showConfirmDialog(null, confirmMessage, confirmMessage, JOptionPane.YES_NO_OPTION);
				if(confirm == JOptionPane.YES_OPTION){
					target.removeRegion(current);
					regionDrop.removeItem(current);
					gv.removeComponent(current);
					updateRegionButtonEnable();
				}
			}
		});
		regionPaneTopRight.add(newRegion);
		regionPaneTopRight.add(deleteRegion);
		regionPointEntryField.setActionCommand("SUBMIT_MANUAL_REGION_POINT");
		regionPointEntryField.addActionListener(submitManualAction);
		regionPaneBottom.add(funcMgr.addButton(regionAddPts, "Add Points", "Finish", T_NEWREGIONPOINT));
		regionPaneBottom.add(HelpIcon.wrapComponent(regionPointEntryField,
			"""
			<html>\
			<p>This textbox can rapidly create region points if you have exact coordinates in meters.</p>\
			<p>Enter two numbers in this pattern:</p>\
			<p>`X Y`</p>\
			<p>Then press 'Enter' to place the point and clear the textbox.</p>\
			</html>\
			"""
			,
			BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Manual Point Create")
		));
		regionPaneBottom.add(funcMgr.addButton(regionMovePts, "Move Points", "Finish", T_MOVEREGIONPOINT));
		regionPaneBottom.add(funcMgr.addButton(regionDelPts, "Delete Points", "Finish", T_DELETEREGIONPOINT));
		panel.add(tabs, BorderLayout.NORTH);
		gv.addComponent(s.getAvatar());
		Iterator<GV_Hull> hullIter = s.regions.iterator();
		while(hullIter.hasNext()){
			GV_Hull h = hullIter.next();
			gv.addComponent(h);
			regionDrop.addItem(h);
		}
		Iterator<GV_Line> wallIter = s.walls.iterator();
		while(wallIter.hasNext()){
			GV_Line w = wallIter.next();
			gv.addComponent(w);
		}
		// This class is used to hook gridview's drawing to draw the points of the currently selected region
		class RegionPointDrawHook extends GridViewComponent{
			public RegionPointDrawHook(){
				super(0,0);
			}
			GV_Hull toDraw(){
				if(tabs.getSelectedComponent() == region_pane){
					return (GV_Hull)regionDrop.getSelectedItem();
				}
				return null;
			}
			public void draw(Graphics2D g, AffineTransform pxT, AffineTransform realT){
				GV_Hull d = toDraw();
				if(d != null){
					d.drawPoints(g, pxT, realT);
				}
			}
			public void getBounds(long[] ret){
				GV_Hull d = toDraw();
				if(d != null){
					d.getBounds(ret);
				}
			}
		}
		gv.addComponent(new RegionPointDrawHook());
		panel.add(gv, BorderLayout.CENTER);
		updateRegionButtonEnable();
		funcMgr.addChangeListener(this);
	}
	void reconstructThickWalls(){
		for(GV_Line w : thickWalls){
			gv.removeComponent(w);
		}
		thickWalls.clear();
		if(target.isThickWalls()){
			thickWalls.addAll(Arrays.asList(target.getThickWalls()));
			for(GV_Line w : thickWalls){
				w.setColor(Color.MAGENTA);
				gv.addComponent(w);
			}
		}
	}
	void updateRegionButtonEnable(){
		boolean hasRegions = (target.getRegionCount() > 0);
		if(hasRegions){
			deleteRegion.setEnabled(true);
			regionAddPts.setEnabled(true);
			regionMovePts.setEnabled(true);
			regionDelPts.setEnabled(true);
			regionPointEntryField.setEnabled(true);
		}else{
			deleteRegion.setEnabled(false);
			regionAddPts.setEnabled(false);
			regionMovePts.setEnabled(false);
			regionDelPts.setEnabled(false);
			regionPointEntryField.setEnabled(false);
		}
		// When editing which regions exist, disable any active tool
		funcMgr.activate(null);
	}
	// This handles events from the RadioJButtonMgr 'funcMgr' for tool state changes.
	public void stateChanged(ChangeEvent e){
		switch(funcMgr.getState()){
			case T_NONE:
				gv.enableMouseClick(true);
				gv.setGridTool(null);
				break;
			case T_AVATAR:
				gv.enableMouseClick(false);
				gv.setGridTool(new AvatarTool(gv, target));
				break;
			case T_NEWWALL:
				gv.enableMouseClick(false);
				gv.setGridTool(new NewWallTool(gv, target, this));
				break;
			case T_MOVEWCORNER:
				gv.enableMouseClick(true);
				gv.setGridTool(new MoveWallCornerTool(gv, target, 500, this));
				break;
			case T_DELETEWALL:
				gv.enableMouseClick(true);
				gv.setGridTool(new DeleteWallTool(gv, target, this));
				break;
			case T_NEWREGIONPOINT:
				gv.enableMouseClick(false);
				gv.setGridTool(new NewRegionPointTool(gv, (GV_Hull)regionDrop.getSelectedItem(), target));
				break;
			case T_MOVEREGIONPOINT:
				gv.enableMouseClick(true);
				gv.setGridTool(new MoveRegionPointTool(gv, (GV_Hull)regionDrop.getSelectedItem(), target, 500));
				break;
			case T_DELETEREGIONPOINT:
				gv.enableMouseClick(true);
				gv.setGridTool(new DeleteRegionPointTool(gv, (GV_Hull)regionDrop.getSelectedItem(), target, 500));
				break;
			default:
				assert false : "Invalid function";
				break;
		}
		gv.repaint();
	}
	public OE_Floorplan getTarget(){
		return target;
	}
}

class AvatarTool extends GridTool{
	OE_Floorplan target;
	int[] mouse = null;
	AvatarTool(GridView owner, OE_Floorplan target){
		super(owner);
		this.target = target;
	}
	public void mousePressed(MouseEvent evt){
		mouse = owner.getMouseLoc(evt, true);
		target.setAvatarPosition(mouse[0], mouse[1]);
		owner.repaint();
	}
	public void mouseMoved(MouseEvent evt){
		mouse = owner.getMouseLoc(evt, true);
		owner.repaint();
	}
	public void mouseExited(MouseEvent evt){
		mouse = null;
		owner.repaint();
	}
	public void draw(Graphics2D g){
		if(mouse == null) return;
		GV_Vec ghostAvatar = new GV_Vec(mouse[0], mouse[1], target.getAvatarDeciAngle());
		ghostAvatar.setColor(Color.BLACK);
		ghostAvatar.draw(g, owner.pxTransform, owner.realTransform);
		drawTextAt(g, String.format("%.1f %.1f m", mouse[0]*0.001, mouse[1]*0.001), mouse[0], mouse[1]);
	};
}
class NewWallTool extends GridTool{
	OE_Floorplan model;
	FloorplanEditor editor;
	int[] mouse = null;
	int[] lastLoc = null;
	boolean angleConstrained = false;
	NewWallTool(GridView owner, OE_Floorplan model, FloorplanEditor editor){
		super(owner);
		this.editor = editor;
		this.model = model;
	}
	int[] angleConstrain(int[] test, int[] constraint, int grid){
		long dX = test[0]-constraint[0];
		long dY = test[1]-constraint[1];
		double distance = Math.sqrt(dX*dX + dY*dY);
		double unitX = dX/distance;
		double unitY = dY/distance;
		double angle = Math.atan2(dY, dX);
		// What angle are we pegged to? Constrain to 0-7 inc.
		int pegIdx = ((int)Math.round(angle * 4.0/Math.PI)+8) % 8;
		double pegAngle = Math.PI*pegIdx*0.25;
		double pegUnitX = Math.cos(pegAngle);
		double pegUnitY = Math.sin(pegAngle);
		// Dot product for projection
		double resLength = unitX*pegUnitX + unitY*pegUnitY;
		//System.out.println(resLength);
		resLength *= distance;
		// Odd peg means we are at a 45deg angle. We need to do special stuff to avoid accidentally snapping to an incorrect peg where x and y mags are not equal
		if(pegIdx % 2 == 1){
			double gridsq = Math.sqrt(2.0*grid*grid);
			resLength = Math.round(resLength/gridsq) * gridsq;
		}else{
			resLength = Math.round(resLength/grid) * grid;
		}
		return new int[]{constraint[0]+(int)(resLength*pegUnitX), constraint[1]+(int)(resLength*pegUnitY)};
	}
	public void mousePressed(MouseEvent evt){
		mouseMoved(evt);
		// We can draw a wall if we have a lastLoc
		if(evt.getButton() == MouseEvent.BUTTON1){
			if(lastLoc != null){
				GV_Line newWall = new GV_Line(lastLoc[0], lastLoc[1], mouse[0], mouse[1]);
				model.addWall(newWall);
				owner.addComponent(newWall);
				editor.reconstructThickWalls();
			}
			lastLoc = mouse;
		}else{
			lastLoc = null;
		}
		owner.repaint();
	}
	public void mouseDragged(MouseEvent evt){
		mouseMoved(evt);
	}
	public void mouseMoved(MouseEvent evt){
		if(angleConstrained && lastLoc != null){
			mouse = owner.getMouseLoc(evt, false);
			mouse = angleConstrain(mouse, lastLoc, owner.getMMGrid());
			//mouse = owner.snapToGrid(mouse);
		}else{
			mouse = owner.getMouseLoc(evt, true);
		}
		owner.repaint();
	}
	public void mouseExited(MouseEvent evt){
		mouse = null;
		owner.repaint();
	}
	public void keyPressed(KeyEvent e){
		if(e.getKeyCode() == KeyEvent.VK_SHIFT){
			angleConstrained = true;
		}
	}
	public void keyReleased(KeyEvent e){
		if(e.getKeyCode() == KeyEvent.VK_SHIFT){
			angleConstrained = false;
		}
	}
	public void draw(Graphics2D g){
		if(mouse == null) return;
		if(lastLoc != null){
			GV_Line ghostWall = new GV_Line(lastLoc[0], lastLoc[1], mouse[0], mouse[1]);
			ghostWall.setColor(Color.RED);
			ghostWall.draw(g, owner.pxTransform, owner.realTransform);
			double distance = Math.sqrt(Math.pow(lastLoc[0]-mouse[0], 2) + Math.pow(lastLoc[1]-mouse[1], 2));
			drawTextAt(g, String.format("%.1f %.1f m\n DIST: %.2f m", mouse[0]*0.001, mouse[1]*0.001, distance*0.001), mouse[0], mouse[1]);
		}else{
			drawTextAt(g, String.format("%.1f %.1f m", mouse[0]*0.001, mouse[1]*0.001), mouse[0], mouse[1]);
		}
	};
}
class MoveWallCornerTool extends GridTool{
	OE_Floorplan model;
	FloorplanEditor editor;
	int mmSelRadius;
	int[] mouse = null;
	//int[] lastLoc = null;
	ArrayList<int[]> targets = new ArrayList<>();
	MoveWallCornerTool(GridView owner, OE_Floorplan model, int mmSelRadius, FloorplanEditor editor){
		super(owner);
		this.model = model;
		this.editor = editor;
		this.mmSelRadius = mmSelRadius;
	}
	int[] getClosestCorner(){
		if(mouse == null) return null;
		int[] ret = null;
		double bestDistance = mmSelRadius;
		for(GV_Line wall : model.walls){
			int[][] pts = new int[][]{wall.p1, wall.p2};
			for(int idx = 0; idx < 2; idx++){
				long dX = pts[idx][0] - mouse[0];
				long dY = pts[idx][1] - mouse[1];
				double dist = Math.sqrt(dX*dX+dY*dY);
				if(dist <= bestDistance){
					bestDistance = dist;
					ret = Arrays.copyOf(pts[idx], 2);
				}
			}
		}
		return ret;
	}
	void getTargetsAt(int[] loc){
		targets.clear();
		if(loc == null) return;
		for(GV_Line wall : model.walls){
			int[][] pts = new int[][]{wall.p1, wall.p2};
			for(int idx = 0; idx < 2; idx++){
				if(pts[idx][0] == loc[0] && pts[idx][1] == loc[1]){
					targets.add(pts[idx]);
				}
			}
		}
	}
	public void mousePressed(MouseEvent evt){
		mouse = owner.getMouseLoc(evt, true);
		int[] closestCorner = getClosestCorner();
		getTargetsAt(closestCorner);
		mouseDragged(evt);
	}
	public void mouseReleased(MouseEvent evt){
		targets.clear();
		owner.repaint();
	}
	public void mouseMoved(MouseEvent evt){
		mouse = owner.getMouseLoc(evt, false);
		owner.repaint();
	}
	public void mouseDragged(MouseEvent evt){
		mouse = owner.getMouseLoc(evt, true);
		for(int[] targ : targets){
			targ[0] = mouse[0];
			targ[1] = mouse[1];
		}
		if(targets.size() > 0){
			model.signalUnsavedChanges();
		}
		editor.reconstructThickWalls();
		owner.repaint();
	}
	public void mouseExited(MouseEvent evt){
		mouse = null;
		owner.repaint();
	}
	public void draw(Graphics2D g){
		Iterator<GV_Line> wallIter = model.walls.iterator();
		GV_Point wallVert = new GV_Point(0,0);
		wallVert.setColor(Color.BLUE);
		while(wallIter.hasNext()){
			GV_Line wall = wallIter.next();
			int[][] pts = new int[][]{wall.p1, wall.p2};
			for(int idx = 0; idx < 2; idx++){
				wallVert.setPosition(pts[idx][0], pts[idx][1]);
				wallVert.draw(g, owner.pxTransform, owner.realTransform);
			}
		}
		int[] closestCorner = getClosestCorner();
		if(closestCorner != null){
			owner.enableMouseClick(false);
			wallVert.setPosition(closestCorner[0], closestCorner[1]);
			wallVert.setColor(Color.RED);
			wallVert.draw(g, owner.pxTransform, owner.realTransform);
			drawTextAt(g, String.format("%.1f %.1f m", closestCorner[0]*0.001, closestCorner[1]*0.001), closestCorner[0], closestCorner[1]);
		}else{
			owner.enableMouseClick(true);
		}
	}
}
class DeleteWallTool extends GridTool{
	OE_Floorplan model;
	FloorplanEditor editor;
	int[] mouse = null;
	DeleteWallTool(GridView owner, OE_Floorplan model, FloorplanEditor editor){
		super(owner);
		this.model = model;
		this.editor = editor;
	}
	GV_Line getClosestWall(){
		if(mouse == null) return null;
		GV_Line ret = null;
		double bestScore = Double.POSITIVE_INFINITY;
		for(GV_Line wall : model.walls){
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
			model.deleteWall(closestWall);
			owner.removeComponent(closestWall);
			editor.reconstructThickWalls();
			owner.repaint();
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
			Color oldColor = closestWall.getColor();
			closestWall.setColor(Color.RED);
			closestWall.draw(g, owner.pxTransform, owner.realTransform);
			closestWall.setColor(oldColor);
			if(mouse != null) drawTextAt(g, "DELETE", mouse[0], mouse[1]);
		}
	}
}

class NewRegionPointTool extends GridTool{
	GV_Hull target;
	int[] mouse = null;
	OE_Floorplan model;
	NewRegionPointTool(GridView owner, GV_Hull target, OE_Floorplan model){
		super(owner);
		this.model = model;
		this.target = target;
	}
	public void mousePressed(MouseEvent evt){
		mouse = owner.getMouseLoc(evt, true);
		model.addRegionPoint(target, mouse[0], mouse[1]);
		owner.repaint();
	}
	public void mouseMoved(MouseEvent evt){
		mouse = owner.getMouseLoc(evt, true);
		owner.repaint();
	}
	public void mouseExited(MouseEvent evt){
		mouse = null;
		owner.repaint();
	}
	public void draw(Graphics2D g){
		if(mouse == null) return;
		GV_Point ghostPoint = new GV_Point(mouse[0], mouse[1]);
		ghostPoint.setColor(Color.BLACK);
		ghostPoint.draw(g, owner.pxTransform, owner.realTransform);
		drawTextAt(g, String.format("%.1f %.1f m", mouse[0]*0.001, mouse[1]*0.001), mouse[0], mouse[1]);
	};
}

class MoveRegionPointTool extends GridTool{
	GV_Hull target;
	int[] mouse = null;
	int mmSelRadius;
	GV_Point selected = null;
	OE_Floorplan model;
	MoveRegionPointTool(GridView owner, GV_Hull target, OE_Floorplan model, int mmSelRadius){
		super(owner);
		this.target = target;
		this.model = model;
		this.mmSelRadius = mmSelRadius;
	}
	GV_Point getClosestPoint(){
		if(mouse == null) return null;
		GV_Point ret = null;
		double bestDistance = mmSelRadius;
		for(GV_Point pt : target.pts){
			int[] ptLoc = new int[]{pt.getX(), pt.getY()};
			long dX = ptLoc[0] - mouse[0];
			long dY = ptLoc[1] - mouse[1];
			double dist = Math.sqrt(dX*dX+dY*dY);
			if(dist <= bestDistance){
				bestDistance = dist;
				ret = pt;
			}
		}
		return ret;
	}
	public void mousePressed(MouseEvent evt){
		mouse = owner.getMouseLoc(evt, true);
		selected = getClosestPoint();
		mouseDragged(evt);
	}
	public void mouseReleased(MouseEvent evt){
		selected = null;
		owner.repaint();
	}
	public void mouseMoved(MouseEvent evt){
		mouse = owner.getMouseLoc(evt, false);
		owner.repaint();
	}
	public void mouseDragged(MouseEvent evt){
		mouse = owner.getMouseLoc(evt, true);
		if(selected != null){
			selected.setPosition(mouse[0], mouse[1]);
			target.rebuildHull();
			model.signalUnsavedChanges();//FIXME
		}
		owner.repaint();
	}
	public void mouseExited(MouseEvent evt){
		mouse = null;
		owner.repaint();
	}
	public void draw(Graphics2D g){
		GV_Point closestPoint = getClosestPoint();
		if(closestPoint != null){
			Color oldColor = closestPoint.getColor();
			closestPoint.setColor(Color.GRAY);
			closestPoint.draw(g, owner.pxTransform, owner.realTransform);
			closestPoint.setColor(oldColor);
			int x = closestPoint.getX();
			int y = closestPoint.getY();
			drawTextAt(g, String.format("%.1f %.1f m", x*0.001, y*0.001), x, y);
			owner.enableMouseClick(false);
		}else{
			owner.enableMouseClick(true);
		}
	}
}
class DeleteRegionPointTool extends GridTool{
	GV_Hull target;
	int[] mouse = null;
	int mmSelRadius;
	OE_Floorplan model;
	DeleteRegionPointTool(GridView owner, GV_Hull target, OE_Floorplan model, int mmSelRadius){
		super(owner);
		this.target = target;
		this.model = model;
		this.mmSelRadius = mmSelRadius;
	}
	GV_Point getClosestPoint(){
		if(mouse == null) return null;
		GV_Point ret = null;
		double bestDistance = mmSelRadius;
		Iterator<GV_Point> ptIter = target.pts.iterator();
		while(ptIter.hasNext()){
			GV_Point pt = ptIter.next();
			int[] ptLoc = new int[]{pt.getX(), pt.getY()};
			long dX = ptLoc[0] - mouse[0];
			long dY = ptLoc[1] - mouse[1];
			double dist = Math.sqrt(dX*dX+dY*dY);
			if(dist <= bestDistance){
				bestDistance = dist;
				ret = pt;
			}
		}
		return ret;
	}
	public void mousePressed(MouseEvent evt){
		mouse = owner.getMouseLoc(evt, true);
		GV_Point selected = getClosestPoint();
		if(selected != null){
			target.removePoint(selected);
			model.signalUnsavedChanges();//FIXME
			owner.repaint();
		}
	}
	public void mouseMoved(MouseEvent evt){
		mouse = owner.getMouseLoc(evt, false);
		owner.repaint();
	}
	public void mouseDragged(MouseEvent evt){
		mouseMoved(evt);
	}
	public void mouseExited(MouseEvent evt){
		mouse = null;
		owner.repaint();
	}
	public void draw(Graphics2D g){
		GV_Point closestPoint = getClosestPoint();
		if(closestPoint != null){
			Color oldColor = closestPoint.getColor();
			closestPoint.setColor(Color.GRAY);
			closestPoint.draw(g, owner.pxTransform, owner.realTransform);
			closestPoint.setColor(oldColor);
			if(mouse != null) drawTextAt(g, "DELETE", mouse[0], mouse[1]);
		}
	}
}
