package net.boerwi.ogaredit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import javax.swing.event.*;
import java.util.*;

import java.io.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

class ImageEditor extends ResEditor implements AsyncBlobWaiter{
	static final ArrayList<String> quickKeys = new ArrayList<>(Arrays.asList("Title","Date","Artist","Medium"));
	
	final OE_Image target;
	final OE_BlobMgr blobMgr;
	final AsyncBlobServer blobServer;
	BufferedImage img = null;
	final GridView gv = new GridView();
	final TreeMap<String, String> tags;
	final JLabel lbSource = new JLabel("\"\"", JLabel.LEFT);
	final JDialog tagEditor = new JDialog(null, "Extended Tags", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
	//Inputs
	ImageInputListener inputListener = new ImageInputListener();
	final JButton
		btChangeSource = new JButton("Change Source"),
		btExtendedTags = new JButton("Edit Extended Tags"),
		btRotLeft = new JButton("↶"),
		btRotRight = new JButton("↷"),
		btResetCrop = new JButton("Reset Crop"),
		btPreviewCrop = new JButton("Preview Crop");
	final JCheckBox cbLockAspect = new JCheckBox("Lock Aspect");
	final JSpinner spWidth = new JSpinner(), spHeight = new JSpinner();
	final ArrayList<JComponent> disableable = new ArrayList<>(Arrays.asList(btExtendedTags, btRotLeft, btRotRight, btResetCrop, btPreviewCrop, cbLockAspect, spWidth, spHeight));
	public ImageEditor(OE_Image s, OE_BlobMgr blobMgr, AsyncBlobServer blobServer){
		target = s;
		this.tags = target.tags;
		this.blobMgr = blobMgr;
		this.blobServer = blobServer;
		panel = new JPanel(new BorderLayout(0, 10));
		setInputEnabled(false);
		JPanel topPane = new JPanel(new GridLayout(2, 1, 0, 5));
		topPane.setBorder(BorderFactory.createRaisedBevelBorder());
		JPanel tagPane = new JPanel(new BorderLayout());
		JPanel quickTagPane = new JPanel(new GridLayout(2, 4));
		quickTagPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Quick Tags"));
		for(String tagKey : quickKeys){
			quickTagPane.add(new JLabel(tagKey, JLabel.RIGHT));
			JTextField field = new JTextField(tags.get(tagKey));
			field.addCaretListener(new CaretListener(){
				public void caretUpdate(CaretEvent e){
					target.setTag(tagKey, field.getText());
				}
			});
			quickTagPane.add(field);
			disableable.add(field);
		}
		tagPane.add(quickTagPane, BorderLayout.NORTH);
		tagPane.add(btExtendedTags, BorderLayout.CENTER);
		JPanel propPane = new JPanel(new GridLayout(2, 1, 0, 5));
		JPanel sizePane = new JPanel(new GridLayout(1, 6));
		cbLockAspect.setHorizontalAlignment(SwingConstants.RIGHT);
		cbLockAspect.setSelected(target.getAspectLocked());
		sizePane.add(cbLockAspect);
		spWidth.setModel(new SpinnerNumberModel(0.1*(double)target.getMMWidth(), 0.1, 100000.0, 0.1));
		spHeight.setModel(new SpinnerNumberModel(0.1*(double)target.getMMHeight(), 0.1, 100000.0, 0.1));
		sizePane.add(new JLabel("Width (cm):", JLabel.RIGHT));
		sizePane.add(spWidth);
		sizePane.add(new JLabel("Height (cm):", JLabel.RIGHT));
		sizePane.add(spHeight);
		sizePane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Dimensions"));
		JPanel srcPane = new JPanel(new GridLayout(1, 3));
		srcPane.add(new JLabel("Source:", JLabel.RIGHT));
		srcPane.add(lbSource);
		srcPane.add(btChangeSource);
		topPane.add(propPane);
		topPane.add(tagPane);
		propPane.add(srcPane);
		propPane.add(sizePane);
		
		JPanel editPane = new JPanel(new BorderLayout());
		editPane.add(gv, BorderLayout.CENTER);
		gv.setRotation(target.getRotation());
		JPanel editToolPane = new JPanel(new GridLayout(1,3,10,0));
		JPanel editToolPaneContainer = new JPanel(new FlowLayout());
		editToolPaneContainer.setBorder(BorderFactory.createEtchedBorder());
		editToolPaneContainer.add(editToolPane);
		editPane.add(editToolPaneContainer, BorderLayout.NORTH);
		JPanel rotatePane = new JPanel(new GridLayout(1,2));
		rotatePane.add(btRotLeft);
		rotatePane.add(btRotRight);
		editToolPane.add(rotatePane);
		editToolPane.add(btResetCrop);
		editToolPane.add(btPreviewCrop);

		panel.add(topPane, BorderLayout.NORTH);

		panel.add(editPane, BorderLayout.CENTER);
		setupTagEditor();
		setupInputActions();
		
		if(target.getBlob() != null){
			loadImage(target.getThumbnail());
			blobServer.getBlobDataAsync(target.getBlob().getId(), this);
		}else{
			loadImage(null);
		}
	}
	void setInputEnabled(boolean enable){
		for(JComponent c : disableable){
			c.setEnabled(enable);
		}
	}
	void loadImage(BufferedImage img){
		lbSource.setText("\"\"");
		gv.clearComponents();
		gv.setGridTool(null);
		if(img != null){
			assert target.getBlob() != null : "We shouldn't be loading any image if the blob is null";
			lbSource.setText("\""+target.getBlob().getName()+"\"");
			int width = img.getWidth();
			int height = img.getHeight();
			// Longest dimension is 50000 mm
			double mmMultiplier = 50000.0 / Math.max(width, height);
			width *= mmMultiplier;
			height *= mmMultiplier;
			GV_Image dispImage = new GV_Image(0, height, width, -height, img);
			gv.addComponent(dispImage);
			gv.setGridTool(new MoveCropPointTool(gv, dispImage, target, this));
		}
		setInputEnabled(img != null);
	}
	public void receiveBlobData(byte[] data){
		BufferedImage img = OE_Image.imageFromBytes(data);
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				loadImage(img);
			}
		});
	}
	void setupTagEditor(){
		tagEditor.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		//tagEditor.setModal(true);
		JPanel content = new JPanel(new BorderLayout());
		JTabbedPane tpTabs = new JTabbedPane(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);
		JButton btExit = new JButton("Done");
		btExit.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				tagEditor.setVisible(false);
			}
		});
		content.add(tpTabs, BorderLayout.CENTER);
		content.add(btExit, BorderLayout.SOUTH);
		for(String tag : tags.keySet()){
			if(!quickKeys.contains(tag)){
				JTextArea txt = new JTextArea(tags.get(tag), 10, 80);
				txt.addCaretListener(new CaretListener(){
					public void caretUpdate(CaretEvent e){
						target.setTag(tag, txt.getText());
					}
				});
				tpTabs.addTab(tag, new JScrollPane(txt));
			}
		}
		tagEditor.add(content);
		tagEditor.pack();
	}
	void setupInputActions(){
		btChangeSource.addActionListener(inputListener);
		btExtendedTags.addActionListener(inputListener);
		btRotLeft.addActionListener(inputListener);
		btRotRight.addActionListener(inputListener);
		btResetCrop.addActionListener(inputListener);
		btPreviewCrop.addActionListener(inputListener);
		cbLockAspect.addActionListener(inputListener);
		spWidth.addChangeListener(inputListener);
		spHeight.addChangeListener(inputListener);
	}
	void updateSpinners(){
		inputListener.spinnersEnabled = false;
		spWidth.setValue(0.1*(double)target.getMMWidth());
		spHeight.setValue(0.1*(double)target.getMMHeight());
		inputListener.spinnersEnabled = true;
	}
	class ImageInputListener implements ChangeListener, ActionListener{
		boolean spinnersEnabled = true;
		public void stateChanged(ChangeEvent evt){
			if(!spinnersEnabled) return;
			Object src = evt.getSource();
			if(src == spWidth || src == spHeight){
				if(src == spWidth){
					target.setWidth((int)Math.round((double)spWidth.getValue()*10.0));
				}else{
					target.setHeight((int)Math.round((double)spHeight.getValue()*10.0));
				}
				// update both value displays, since they can both change if aspect is locked
				updateSpinners();
			}else{
				assert false : "Unknown state event";
			}
		}
		public void actionPerformed(ActionEvent evt){
			Object src = evt.getSource();
			if(src == btChangeSource){
				JFileChooser fc = new JFileChooser();
				fc.setFileFilter(ResEditor.imageFileFilter);
				fc.setMultiSelectionEnabled(false);
				fc.setAcceptAllFileFilterUsed(true);
				fc.setFileHidingEnabled(true);
				int ret = fc.showOpenDialog(null);
				if(ret == JFileChooser.APPROVE_OPTION){
					try{
						File sel = fc.getSelectedFile();
						long bId = blobMgr.addBlob(new FileInputStream(sel), sel.getName());
						target.setBlob(blobMgr.getBlob(bId));
					}catch(IOException e){
						
						target.setBlob(null);
						assert false;//FIXME
					}
					if(target.getBlob() != null){
						loadImage(target.getThumbnail());
						blobServer.getBlobDataAsync(target.getBlob().getId(), ImageEditor.this);
					}else{
						loadImage(null);
					}
				}
				updateSpinners();
			}else if(src == btExtendedTags){
				tagEditor.setVisible(true);
			}else if(src == btRotLeft){
				target.rotate(1);
				gv.setRotation(target.getRotation());
				// If aspect is locked, a rotation can trigger dimension changes
				updateSpinners();
				gv.repaint();
			}else if(src == btRotRight){
				target.rotate(-1);
				gv.setRotation(target.getRotation());
				// If aspect is locked, a rotation can trigger dimension changes
				updateSpinners();
				gv.repaint();
			}else if(src == btResetCrop){
				target.resetCropCorners();
				gv.repaint();
			}else if(src == btPreviewCrop){
				BufferedImage cImg = target.getCroppedImage();//getSourceImage();
				JDialog previewDialog = new JDialog((Frame)null, "Crop Preview");//, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
				//previewDialog.setResizable(false);
				previewDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				Container content = previewDialog.getContentPane();//new JPanel(new GridLayout(2,1));
				content.setLayout(new BorderLayout());
				JButton btExit = new JButton("Close");
				btExit.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent evt){
						previewDialog.dispose();
					}
				});
				ImgViewer i = new ImgViewer(300, cImg);
				content.add(i, BorderLayout.CENTER);
				content.add(btExit, BorderLayout.SOUTH);
				previewDialog.pack();
				previewDialog.setVisible(true);
			}else if(src == cbLockAspect){
				target.setAspectLocked(cbLockAspect.isSelected());
				updateSpinners();
			}else{
				assert false : "Unknown action";
			}
		}
	}
	public OE_Image getTarget(){
		return target;
	}
}
@SuppressWarnings("serial") // We don't support serialization
class ImgViewer extends JComponent{
	int size;
	BufferedImage cImg;
	ImgViewer(int size, BufferedImage cImg){
		this.cImg = cImg;
		this.size = size;
		//this.setMinimumSize(new Dimension(300,300));
		this.setPreferredSize(new Dimension(300,300));
		//this.setSize(new Dimension(300,300));
	}
	public void paintComponent(Graphics g){
		Graphics2D g2 = (Graphics2D)g;
		int width = cImg.getWidth();
		int height = cImg.getHeight();
		int cHeight = getHeight();
		int cWidth = getWidth();
		double aspect = (double)width/(double)height;
		double canvasAspect = cWidth/(double)cHeight;
		double scale = Math.min(cWidth/(double)width,cHeight/(double)height);
		//System.out.println("scale: "+scale+" aspect: "+aspect);
		AffineTransform t = new AffineTransform();
		t.scale(scale, scale);
		if(aspect < canvasAspect){//move horizontal
			t.translate(0.5*(cWidth-scale*width)/scale, 0.0);
		}else{//move vertical
			t.translate(0.0, 0.5*(cHeight-scale*height)/scale);
		}
		//System.out.println(t);
		g2.drawRenderedImage(cImg, t);
	}
}
class MoveCropPointTool extends GridTool{
	int[] mouse = null;
	//target is the drawn entity, model is the resource/backend entity. This tool bridges the gap for manipulating crop points
	GV_Image target;
	OE_Image model;
	ImageEditor editor;
	int selected = -1;
	int closestPoint = -1;
	String[] cornerName = new String[]{"Upper Left", "Lower Left", "Lower Right", "Upper Right"};
	MoveCropPointTool(GridView owner, GV_Image target, OE_Image model, ImageEditor editor){
		super(owner);
		this.target = target;
		this.model = model;
		this.editor = editor;
	}
	int getClosestPoint(){
		if(mouse == null) return -1;
		int ret = -1;
		double dist = 1000.0;
		for(int idx = 0; idx < 4; idx++){
			double[] cropCorner = model.getCropCorner(idx);
			int[] loc = target.getRealCoords(cropCorner[0], cropCorner[1]);
			loc[0] -= mouse[0];
			loc[1] -= mouse[1];
			double testDist = Math.sqrt(loc[0]*loc[0] + loc[1]*loc[1]);
			if(testDist < dist){
				dist = testDist;
				ret = idx;
			}
		}
		return ret;
	}
	public void mousePressed(MouseEvent evt){
		mouse = owner.getMouseLoc(evt, false);
		selected = getClosestPoint();
		mouseDragged(evt);
	}
	public void mouseReleased(MouseEvent evt){
		selected = -1;
		owner.repaint();
	}
	public void mouseMoved(MouseEvent evt){
		mouse = owner.getMouseLoc(evt, false);
		int newClosest = getClosestPoint();
		if(newClosest != closestPoint){
			closestPoint = newClosest;
			owner.repaint();
		}
	}
	public void mouseDragged(MouseEvent evt){
		mouse = owner.getMouseLoc(evt, false);
		if(selected != -1){
			double[] imgcoords = target.getImageCoords(mouse[0], mouse[1]);
			model.setCropCorner(selected, imgcoords[0], imgcoords[1]);
			editor.updateSpinners();
		}
		owner.repaint();
	}
	public void mouseExited(MouseEvent evt){
		mouse = null;
		owner.repaint();
	}
	public void draw(Graphics2D g){
		int[] realX = new int[4];
		int[] realY = new int[4];
		int closestPoint = getClosestPoint();
		for(int idx = 0; idx < 4; idx++){
			double[] cropCorner = model.getCropCorner(idx);
			int[] loc = target.getRealCoords(cropCorner[0], cropCorner[1]);
			realX[idx] = loc[0];
			realY[idx] = loc[1];
		}
		g.setColor(Color.RED);
		for(int idx = 0; idx < 4; idx++){
			g.drawLine(realX[idx], realY[idx], realX[(idx+1)%4], realY[(idx+1)%4]);
		}
		g.setColor(Color.GRAY);
		for(int idx = 0; idx < 4; idx++){
			if(closestPoint == idx) continue;
			g.fillOval(realX[idx]-200, realY[idx]-200, 400, 400);
		}
		for(int idx = 0; idx < 4; idx++){
			drawTextAt(g, cornerName[idx], realX[idx], realY[idx]);
		}
		g.setColor(Color.RED);
		if(closestPoint != -1){
			g.drawRect(realX[closestPoint]-300, realY[closestPoint]-300, 600, 600);
			owner.enableMouseClick(false);
		}else{
			owner.enableMouseClick(true);
		}
	}
}
