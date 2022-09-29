package net.boerwi.ogaredit;

import java.util.Base64;
import java.util.Arrays;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.json.JSONString;
import org.json.JSONObject;
import org.json.JSONArray;

import javax.sound.sampled.*;

public class OE_Audio extends OE_Resource {
	String description = "[Audio Description]";
	long msDuration = -1;
	byte[] amplitudeGraph = new byte[50];
	OE_Blob blob = null;
	public OE_Audio(){
	}
	public OE_Audio(OE_Blob blob){
		setBlob(blob);
	}
	public OE_Audio(String name, long uid, JSONObject dat, OE_BlobMgr blobMgr){
		super(name, uid);
		// We don't use setBlob here because that would retrigger expensive file processing operations that we don't need since they are saved
		blob = blobMgr.getBlob(dat.getLong("blobId"));
		description = dat.getString("desc");
		msDuration = dat.getLong("msDur");
		amplitudeGraph = Base64.getDecoder().decode(dat.getString("amplitude"));
	}
	public OE_Resource duplicate(){
		OE_Audio ret = new OE_Audio(blob);
		ret.description = description;
		commonDuplicateTasks(ret);
		return ret;
	}
	public OE_ResType getType(){ return OE_ResType.AUDIO; }
	public void setDescription(String text){
		if(description.equals(text)) return;
		description = text;
		signalUnsavedChanges();
	}
	public String getDescription(){
		return description;
	}
	public long getDuration(){
		return msDuration;
	}
	public void setBlob(OE_Blob blob){
		if(this.blob != null){
			this.blob.remDep(this);
		}
		this.blob = blob;
		msDuration = -1;
		if(this.blob != null){
			blob.addDep(this);
			try{
				// Audio stream (encoded)
				AudioInputStream as = AudioSystem.getAudioInputStream(new BufferedInputStream(blob.getData()));
				AudioFormat af = as.getFormat();
				System.out.println("Source audio: "+as+"\n\tFormat:"+af+"\n\tSampleRate:"+af.getSampleRate()+" SampleBits:"+af.getSampleSizeInBits());
				// Line audio stream
				AudioInputStream las = AudioSystem.getAudioInputStream(rateMatchedLineFormat(af), as);
				long streamByteCount = 0;
				while(true){
					final byte[] buffer = las.readNBytes(65536);
					if(buffer.length == 0) break;
					streamByteCount += buffer.length;
				}
				// Line audio format
				AudioFormat laf = las.getFormat();
				double streamSec = (streamByteCount/laf.getFrameSize()) / (double) laf.getFrameRate();
				System.out.println("Seconds: "+streamSec+" Rate: "+laf.getFrameRate()+" Byte Count: "+streamByteCount);
				msDuration = Math.round(streamSec*1000.0);
			}catch(UnsupportedAudioFileException ex){
				assert false : "unsupported audio file: "+ex;
			}catch(IOException ex){
				assert false : "IOException"+ex;
			}
		}
		//TODO calculate amplitude graph
		signalUnsavedChanges();
	}
	public String getExportFileName(){
		if(blob == null){
			return "__default.mp3";
		}else{
			return getId()+"_"+toString()+".mp3";
		}
	}
	public byte[] getExportEncodedAudio(){
		byte[] ret = new byte[0];
		if(blob != null){
			try{
				AudioInputStream as = AudioSystem.getAudioInputStream(new BufferedInputStream(blob.getData()));
				AudioInputStream las = AudioSystem.getAudioInputStream(rateMatchedLineFormat(as.getFormat()), as);
				ret = encodeToMp3(las, 56, false);
			}catch(UnsupportedAudioFileException ex){
				assert false : "Unsupported audio file for test export";
			}catch(IOException ex){
				assert false : "IOException"+ex;
			}
		}
		return ret;
	}
	// From https://odoepner.wordpress.com/2013/07/19/play-mp3-or-ogg-using-javax-sound-sampled-mp3spi-vorbisspi/
	static AudioFormat rateMatchedLineFormat(AudioFormat inFormat) {
		final int ch = inFormat.getChannels();
		final float rate = inFormat.getSampleRate();
		return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, rate, 16, ch, ch * 2, rate, false);
	}
	// Referenced https://github.com/nwaldispuehl/java-lame/blob/master/src/test/java/net/sourceforge/lame/lowlevel/LameEncoderTest.java
	static byte[] encodeToMp3(AudioInputStream audioInputStream, int kbps, boolean variableBitRate){
		de.sciss.jump3r.lowlevel.LameEncoder encoder = new de.sciss.jump3r.lowlevel.LameEncoder(audioInputStream.getFormat(), kbps, 3/*Mono, see https://github.com/Sciss/jump3r/blob/master/src/main/java/de/sciss/jump3r/lowlevel/LameEncoder.java*/, 1, variableBitRate);
		ByteArrayOutputStream mp3 = new ByteArrayOutputStream();
		byte[] inputBuffer = new byte[encoder.getPCMBufferSize()];
		byte[] outputBuffer = new byte[encoder.getPCMBufferSize()];
		int bytesRead;
		int bytesWritten;
		try{
			while(0 < (bytesRead = audioInputStream.read(inputBuffer))) {
				bytesWritten = encoder.encodeBuffer(inputBuffer, 0, bytesRead, outputBuffer);
				mp3.write(outputBuffer, 0, bytesWritten);
			}
		}catch(IOException ex){
			assert false : "Failed to encode MP3: "+ex;
			encoder.close();
			return new byte[0];
		}
		encoder.close();
		return mp3.toByteArray();
	}
	public OE_Blob getBlob(){
		return blob;
	}
	public void cleanup(){
		setBlob(null);
		super.cleanup();
	}
	public String toJSONString(){
		JSONObject ret = getCoreJson();
		JSONObject dat = (JSONObject)(ret.get("dat"));
		dat.put("msDur", msDuration);
		long blobId = -1;
		if(blob != null){
			blobId = blob.getId();
		}
		dat.put("blobId", blobId);
		dat.put("desc", description);
		dat.put("amplitude", Base64.getEncoder().encodeToString(amplitudeGraph));
		return ret.toString();
	}
}
