package net.boerwi.ogaredit;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

abstract class ResEditor {
	JPanel panel;
	public static final FileFilter
		imageFileFilter = new FileNameExtensionFilter(".jpeg, .jpg, .png, .tiff, .tif", "jpeg", "jpg", "png", "tiff", "tif"),
		audioFileFilter = new FileNameExtensionFilter(".mp3, .wav", "mp3", "wav"),
		textFileFilter = new FileNameExtensionFilter(".txt", "txt"),
		oeFileFilter = new FileNameExtensionFilter(".ogr", "ogr"),
		jsonFileFilter = new FileNameExtensionFilter(".json", "json");
	static ResEditor createEditor(OE_Resource s, OE_DB db, AsyncBlobServer blobServer){
		if(s instanceof OE_Image){
			return new ImageEditor((OE_Image)s, db.getBlobMgr(), blobServer);
		}else if(s instanceof OE_Text){
			return new TextEditor((OE_Text)s);
		}else if(s instanceof OE_LabelStyle){
			return new LabelStyleEditor((OE_LabelStyle)s);
		}else if(s instanceof OE_MatteStyle){
			return new MatteStyleEditor((OE_MatteStyle)s);
		}else if(s instanceof OE_Audio){
			return new AudioEditor((OE_Audio)s, db.getBlobMgr());
		}else if(s instanceof OE_Floorplan){
			return new FloorplanEditor((OE_Floorplan)s);
		}else if(s instanceof OE_Gallery){
			return new GalleryEditor((OE_Gallery)s, new OE_ResourceSelector(db));
		}else{
			assert false : "Unknown type";
		}
		return null;
	}
	JPanel getPanel(){
		return panel;
	}
	public void cleanup(){}
	abstract OE_Resource getTarget();
}
