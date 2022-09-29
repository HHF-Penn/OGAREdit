package net.boerwi.ogaredit;

import java.util.regex.Pattern;
import java.util.function.Predicate;
import java.util.ArrayList;
import java.util.Random;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;


import org.apache.commons.imaging.Imaging;


public class OE_Color{
	static final Random rnd = new Random();
	static final String colorPattern = "#?[0-9a-fA-F]{6}";// #?([0-9a-fA-F]{6})|([0-9a-fA-F]{8})"
	static final Predicate<String> colorPredicate = Pattern.compile(colorPattern).asMatchPredicate();
	public OE_Color(){
		this("#555555");
	}
	// RGBA, domain 0-255 inclusive
	final int[] components = new int[]{255,255,255,255};
	public OE_Color(String hex){
		if(!colorPredicate.test(hex)){
			hex = "555555";
		}
		//Cut off the first character (#) on odd-length strings
		if(hex.length()%2 == 1){
			hex = hex.substring(1);
		}
		for(int idx = 0; idx < hex.length()/2; idx++){
			components[idx] = Integer.parseInt(hex.substring(idx*2, idx*2+2), 16);
		}
	}
	public OE_Color(int R, int G, int B, int A){
		components[0] = R;
		components[1] = G;
		components[2] = B;
		components[3] = A;
	}
	/**
	 * degHue - 0.0-1.0;
	 * saturation 0.0-1.0
	 * value 0.0-1.0
	 * from https://en.wikipedia.org/wiki/HSL_and_HSV#HSV_to_RGB
	 */
	public static OE_Color fromHSV(float hue, float saturation, float value){
		//assert hue >= 0 && hue <= 1 && saturation >= 0 && saturation <= 1 && value >= 0 && value >= 1 : "fromHSV got out of bounds values";
		if(hue < 0.0f) hue = 0.0f;
		if(hue > 1.0f) hue = 1.0f;
		if(saturation < 0.0f) saturation = 0.0f;
		if(saturation > 1.0f) saturation = 1.0f;
		if(value < 0.0f) value = 0.0f;
		if(value > 1.0f) value = 1.0f;
		double chroma = saturation * value;
		//H', which is from 0-6.
		double hueP = hue*6.0;
		// Second-largest component
		double X = chroma * (1.0-Math.abs(hueP % 2.0 - 1.0));
		// This is wack, but results in the correct component indices
		int XIdx = 2+((-1 - ((int)hueP)) % 3);
		int chromaIdx = ((((int)hueP)+1)/2)%3;
		int base = (int)(255*(value - chroma));
		int[] rgb = new int[]{base,base,base};
		rgb[XIdx] += 255*X;
		rgb[chromaIdx] += 255*chroma;
		return new OE_Color(rgb[0], rgb[1], rgb[2], 255);
	}
	public static OE_Color random(){
		return new OE_Color(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256), 255);
	}
	public float[] getHSV(){
		int[] rgb = new int[]{components[0], components[1], components[2]};
		int min = 255;
		int max = 0;
		for(int idx = 0; idx < 3; idx++){
			if(rgb[idx] < min) min = rgb[idx];
			if(rgb[idx] > max) max = rgb[idx];
		}
		float chroma = (max-min)/255.0f;
		float value = max/255.0f;
		float saturation = 0.0f;
		if(value != 0.0f) saturation = chroma/value;
		float hue = 0.0f;
		if(chroma != 0){
			if(max == rgb[0]){ // V = R
				hue = (rgb[1]-rgb[2])/255.0f/chroma;
			}else if(max == rgb[1]){ // V = G
				hue = 2.0f+(rgb[2]-rgb[0])/255.0f/chroma;
			}else if(max == rgb[2]){ // V = B
				hue = 4.0f+(rgb[0]-rgb[1])/255.0f/chroma;
			}
			hue = ((hue/6.0f) + 2.0f ) % 1.0f;
		}
		return new float[]{hue, saturation, value};
	}
	public int asInt(){
		return components[3]<<24 | components[0] << 16 | components[1] << 8 | components[2];
	}
	public String getHex(boolean includeAlpha){
		if(includeAlpha){
			return String.format("#%02X%02X%02X%02X", components[0], components[1], components[2], components[3]);
		}else{
			return String.format("#%02X%02X%02X", components[0], components[1], components[2]);
		}
	}
	public Color toAwtColor(){
		return new Color(components[0], components[1], components[2], components[3]);
	}
	@Override
	public boolean equals(Object other){
		if(other instanceof OE_Color){
			OE_Color o = (OE_Color) other;
			return components[0] == o.components[0] && components[1] == o.components[1] && components[2] == o.components[2] && components[3] == o.components[3];
		}
		return false;
	}
	public int hashCode(){
		return asInt();
	}
}
class ColorPicker implements CaretListener, ChangeListener{
	private OE_Color c;
	private JSlider slValue = new JSlider(JSlider.VERTICAL, 0, 255, 0);
	private JTextField tfHexColor = new JTextField();
	private HueSaturationChooser hsChooser;
	private JPanel plMain = new JPanel(new BorderLayout());
	public ColorPicker(OE_Color start){
		c = start;
		if(c == null) c = new OE_Color();
		
		// Hex color text box
		JPanel topHexPane = new JPanel(new GridLayout(1, 2));
		topHexPane.add(new JLabel("Hex:", JLabel.RIGHT));
		topHexPane.add(tfHexColor);
		plMain.add(topHexPane, BorderLayout.NORTH);
		
		// Graphical color chooser
		JPanel bottomColorPane = new JPanel(new BorderLayout());
		plMain.add(bottomColorPane, BorderLayout.CENTER);
		// Value slider
		bottomColorPane.add(slValue, BorderLayout.WEST);
		 // Hue-saturation field
		hsChooser = new HueSaturationChooser();
		bottomColorPane.add(hsChooser, BorderLayout.CENTER);
		
		// Listeners
		slValue.addChangeListener(this);
		tfHexColor.addCaretListener(this);
		hsChooser.addChangeListener(this);
		updating = true;
		updateHexChooser();
		updateGraphicalChooser();
		updating = false;
	}
	public void show(){
		JDialog cpDialog = new JDialog(null, "Color Picker", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
		cpDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		Container content = cpDialog.getContentPane();
		content.setLayout(new BorderLayout());
		content.add(this.getPickerComponent(), BorderLayout.CENTER);
		JButton btExit = new JButton("Close");
		btExit.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent evt){
				cpDialog.dispose();
			}
		});
		content.add(btExit, BorderLayout.SOUTH);
		cpDialog.pack();
		cpDialog.setVisible(true);
	}
	private boolean updating = false;// prevents changes from looping modifications
	public void stateChanged(ChangeEvent evt){
		if(updating) return;
		updating = true;
		float value = slValue.getValue()/255.0f;
		float hue = hsChooser.getHue();
		float saturation = hsChooser.getSaturation();
		if(evt.getSource() == slValue){
			hsChooser.setColorValue(value);
		}else if(evt.getSource() == hsChooser){
		}
		c = OE_Color.fromHSV(hue, saturation, value);
		updateHexChooser();
		updating = false;
	}
	public void caretUpdate(CaretEvent evt){
		if(updating) return;
		updating = true;
		c = new OE_Color(tfHexColor.getText());
		updateGraphicalChooser();
		updating = false;
	}
	private void updateHexChooser(){
		tfHexColor.setText(c.getHex(false));
	}
	private void updateGraphicalChooser(){
		float[] hsv = c.getHSV();
		hsChooser.setHSV(hsv[0], hsv[1], hsv[2]);
		slValue.setValue((int)Math.round(hsv[2]*255.0));
		hsChooser.repaint();
	}
	public OE_Color getColor(){
		return c;
	}
	public JComponent getPickerComponent(){
		return plMain;
	}
	@SuppressWarnings("serial") // We don't support serialization
	class HueSaturationChooser extends JComponent implements MouseInputListener{
		private static BufferedImage colorGradientImage = null;
		private float value = 1.0f, saturation = 1.0f, hue = 1.0f;
		private ArrayList<ChangeListener> changeListeners = new ArrayList<>();
		public HueSaturationChooser(){
			if(colorGradientImage == null){
				try{
					colorGradientImage = Imaging.getBufferedImage(this.getClass().getClassLoader().getResourceAsStream("resource/colorGradient.png"));
				}catch(Exception e){
					assert false : "Failed to load color gradient image from jar";
				}
			}
			setPreferredSize(new Dimension(300, 300));
			addMouseListener(this);
			addMouseMotionListener(this);
		}
		public void setHSV(float hue, float saturation, float value){
			this.hue = hue;
			this.saturation = saturation;
			this.value = value;
			repaint();
		}
		public void setColorValue(float value){
			this.value = value;
			repaint();
		}
		public float getSaturation(){
			return saturation;
		}
		public float getHue(){
			return hue;
		}
		public void paintComponent(Graphics g){
			Graphics2D g2 = (Graphics2D)g;
			int cHeight = getHeight();
			int cWidth = getWidth();
			AffineTransform pxT = g2.getTransform();
			AffineTransform cSpaceT = new AffineTransform();
			cSpaceT.scale(cWidth/255.0, cHeight/255.0);
			g2.transform(cSpaceT);
			/*for(int x = 0; x < 255; x++){
				for(int y = 0; y < 255; y++){
					g.setColor(OE_Color.fromHSV(x/255.0f, y/255.0f, value).toAwtColor());
					g.fillRect(x, y, 1, 1);
				}
			}*/
			// Drawing buffered image data is much faster than looping drawing rectangles
			g2.drawRenderedImage(colorGradientImage, null);
			// Fill in semi-transparent black rectangle to simulate value. FIXME I don't think this is accurate at values which are not 0 or 1.
			g.setColor(new Color(0.0f, 0.0f, 0.0f, 1.0f-value));
			g.fillRect(0, 0, 255, 255);
			g.setColor(Color.RED);
			int x = (int)(hue * 255);
			int y = (int)(saturation * 255);
			g.drawRect(x-2, y-2, 4, 4);
			g.setColor(Color.BLACK);
			g.drawLine(x-4, y, x+4, y);
			g.drawLine(x, y-4, x, y+4);
		}
		private void reportChange(){
			ChangeEvent e = new ChangeEvent(this);
			for(ChangeListener l : changeListeners){
				l.stateChanged(e);
			}
		}
		public void addChangeListener(ChangeListener l){
			changeListeners.add(l);
		}
		public void removeChangeListener(ChangeListener l){
			changeListeners.remove(l);
		}
		public void mouseClicked(MouseEvent e){}
		public void mouseEntered(MouseEvent e){}
		public void mouseExited(MouseEvent e){}
		public void mousePressed(MouseEvent e){
			hue = e.getX()/(float)getWidth();
			saturation = e.getY()/(float)getHeight();
			repaint();
			reportChange();
		}
		public void mouseReleased(MouseEvent e){}
		public void mouseDragged(MouseEvent e){
			mousePressed(e);
		}
		public void mouseMoved(MouseEvent e){}
	}
}
