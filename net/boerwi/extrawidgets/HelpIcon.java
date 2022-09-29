package net.boerwi.extrawidgets;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import org.apache.commons.imaging.Imaging;

/**
 * A help widget. It appears as a question-mark icon and instantly displays an HTML tooltip on mouseover.
 */
@SuppressWarnings("serial") // We don't support serialization
public class HelpIcon extends JComponent{
	private JToolTip tt = new JToolTip();
	private Popup displayed = null;
	/** The size of the widget. */
	private static final int dim = 20;
	/** Preferred margin around the widget. */
	private static final int dimMargin = 5;
	private static final Dimension d_dim = new Dimension(dim, dim);
	private static final Dimension p_dim = new Dimension(dim+dimMargin, dim+dimMargin);
	private static BufferedImage helpIcon = null;
	private static final AffineTransform iconScaleT = new AffineTransform();
	/**
	 * Bundles a Component with a HelpIcon.
	 * @param c The Component to bundle.
	 * @param helpText The tooltip HTML to associate with the new HelpIcon.
	 * @param border A border to apply to the returned JPanel, or null.
	 * @return A JPanel containing `c` and a HelpIcon.
	 */
	public static JPanel wrapComponent(Component c, String helpText, Border border){
		JPanel ret = new JPanel(new BorderLayout());
		if(border != null){
			ret.setBorder(border);
		}
		ret.add(c, BorderLayout.CENTER);
		ret.add(new HelpIcon(helpText), BorderLayout.EAST);
		return ret;
	}
	/**
	 * Creates a label with a HelpIcon next to it.
	 * @see wrapComponent
	 * @param text The label text.
	 * @param helpText The tooltip HTML to associate with the HelpIcon.
	 * @return A JPanel containing a JLabel and a HelpIcon.
	 */
	public static JPanel wrapString(String text, String helpText){
		return wrapComponent(new JLabel(text), helpText, null);
	}
	/**
	 * Creates a new HelpIcon.
	 * @param ttText The tooltip contents to display on hover. (in HTML)
	 */
	public HelpIcon(String ttText){
		// Static load icon on first instantiation
		if(helpIcon == null){
			try{
				helpIcon = Imaging.getBufferedImage(this.getClass().getClassLoader().getResourceAsStream("resource/ico_hoverHelp.png"));
				iconScaleT.translate(-dim/2.0, -dim/2.0);
				iconScaleT.scale(dim/(float)helpIcon.getWidth(), dim/(float)helpIcon.getHeight());
			}catch(Exception e){
				assert false : "Failed to load help icon from jar";
			}
		}
		// Component tasks
		setPreferredSize(p_dim);
		setMinimumSize(d_dim);
		setMaximumSize(p_dim);
		HelpIconMouseHandler mh = new HelpIconMouseHandler();
		addMouseListener(mh);
		addMouseMotionListener(mh);
		
		// Tooltip tasks
		tt.setTipText(ttText);
	}
	private void displayTT(){
		if(displayed != null) return;
		Point pt = this.getLocationOnScreen();
		displayed = PopupFactory.getSharedInstance().getPopup(this, tt, pt.x+(getWidth()+dim)/2, pt.y+(getHeight()+dim)/2);
		displayed.show();
	}
	private void hideTT(){
		if(displayed == null) return;
		displayed.hide();
		displayed = null;
	}
	/** Paints a help icon.
	 * @param g The graphics context to paint in.
	 */
	public void paintComponent(Graphics g){
		Graphics2D g2 = (Graphics2D)g;
		g2.transform(new AffineTransform(1,0,0,1, getWidth()/2.0, getHeight()/2.0));
		g2.transform(iconScaleT);
		g2.drawRenderedImage(helpIcon, null);
	}
	private class HelpIconMouseHandler implements MouseInputListener{
		public void mouseClicked(MouseEvent e){
			// Some future 'open a help webpage in the browser' action?
		}
		public void mouseEntered(MouseEvent e){
			mouseMoved(e);
		}
		public void mouseExited(MouseEvent e){
			hideTT();
		}
		public void mousePressed(MouseEvent e){}
		public void mouseReleased(MouseEvent e){}
		public void mouseDragged(MouseEvent e){}
		public void mouseMoved(MouseEvent e){
			// If we are distended, only display the tooltip when above the square icon
			float x = ((float)e.getX())-getWidth()/2.0f;
			float y = ((float)e.getY())-getHeight()/2.0f;
			if(Math.abs(x) < dim/2.0f && Math.abs(y) < dim/2.0f){
				displayTT();
			}else{
				hideTT();
			}
		}
	}
}
