package net.boerwi.ogaredit;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;

import java.util.Arrays;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioInputStream;

class AudioEditor extends ResEditor{
	final OE_Audio target;
	final OE_BlobMgr blobMgr;
	final JLabel lbSource = new JLabel("\"\"", JLabel.LEFT);
	final JLabel lbDuration = new JLabel("", JLabel.CENTER);
	final JButton btChangeSource = new JButton("Change Source");
	final JButton btPlayPause = new JButton("⏵ Play");//⏸⏹
	final JTextArea descArea;
	final JPanel playbackPane;
	AsyncAudioPlayer[] audioplayer = new AsyncAudioPlayer[]{null};
	Thread audioPlayerThread = null;
	public AudioEditor(OE_Audio s, OE_BlobMgr blobMgr){
		target = s;
		this.blobMgr = blobMgr;
		panel = new JPanel(new GridLayout(2, 1));
		JPanel topPane = new JPanel(new GridLayout(2, 1, 0, 10));
		panel.add(topPane);
		JPanel srcPane = new JPanel(new GridLayout(1, 3, 0, 5));
		topPane.add(srcPane);
		srcPane.add(new JLabel("Source:", JLabel.RIGHT));
		srcPane.add(lbSource);
		srcPane.add(btChangeSource);
		btChangeSource.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				JFileChooser fc = new JFileChooser();
				fc.setFileFilter(ResEditor.audioFileFilter);
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
					stopPlaying();
					updateView();
				}
			}
		});
		JPanel descriptionPane = new JPanel(new BorderLayout());
		descriptionPane.add(new JLabel("Description:", JLabel.RIGHT), BorderLayout.WEST);
		descArea = new JTextArea(target.getDescription());
		descArea.addCaretListener(new CaretListener(){
			public void caretUpdate(CaretEvent e){
				s.setDescription(descArea.getText());
			}
		});
		descriptionPane.add(new JScrollPane(descArea), BorderLayout.CENTER);
		topPane.add(descriptionPane);
		playbackPane = new JPanel(new GridLayout(3, 1));
		playbackPane.setBorder(BorderFactory.createRaisedBevelBorder());
		panel.add(playbackPane, BorderLayout.CENTER);
		playbackPane.add(lbDuration);
		JPanel playerPane = new JPanel(new GridLayout(1, 3));
		playbackPane.add(playerPane);
		playerPane.add(btPlayPause);
		btPlayPause.addActionListener(new PlayButtonListener(this));
		updateView();
	}
	void stopPlaying(){
		if(audioplayer[0] == null) return;
		audioplayer[0].stopRunning();
		try{
			audioPlayerThread.join();
		}catch(InterruptedException e){
			// We an safely pretend like this didn't happen.
		}
		audioPlayerThread = null;
		audioplayer[0] = null;
		btPlayPause.setText("⏵ Play");
	}
	//This function is called from outside of the swing loop, so we need to handle it carefully.
	void audioDonePlaying(){
		SwingUtilities.invokeLater(new Runnable() { public void run() {
			stopPlaying();
		}});
	}
	void updateView(){
		if(target.blob == null){
			lbSource.setText("\"\"");
			descArea.setEnabled(false);
			playbackPane.setEnabled(false);
			lbDuration.setText("Duration: -m --s");
			btPlayPause.setEnabled(false);
			btPlayPause.setText("⏵ Play");
		}else{
			lbSource.setText("\""+target.getBlob().getName()+"\"");
			descArea.setEnabled(true);
			playbackPane.setEnabled(true);
			long ms = target.getDuration();
			btPlayPause.setEnabled(true);
			lbDuration.setText(String.format("Duration: %dm %02ds", ms/60000l, (ms/1000l)%60l));
		}
	}
	public void cleanup(){
		stopPlaying();
	}
	public OE_Audio getTarget(){
		return target;
	}
	class PlayButtonListener implements ActionListener{
		AudioEditor owner;
		public void actionPerformed(ActionEvent evt){
			if(owner.audioplayer[0] == null){ //We aren't playing; start
				owner.audioplayer[0] = new AsyncAudioPlayer(new BufferedInputStream(target.blob.getData()), owner);
				owner.audioPlayerThread = new Thread(audioplayer[0]);
				owner.audioPlayerThread.start();
				owner.btPlayPause.setText("⏹ Stop");
			}else{ //We are playing; stop
				owner.stopPlaying();
			}
		}
		public PlayButtonListener(AudioEditor owner){
			this.owner = owner;
		}
	}
}
class AsyncAudioPlayer implements Runnable{
	AudioInputStream in;
	SourceDataLine line;
	AudioFormat outFormat;
	AudioEditor callback;//FIXME make a listener architecture instead
	boolean running = true;
	public void run(){
		try{
			line.start();
			AudioInputStream pcmStream = AudioSystem.getAudioInputStream(outFormat, in);
			final byte[] buffer = new byte[65536];
			for(int n = 0; n != -1; n = pcmStream.read(buffer, 0, buffer.length)) {
				if(!running) break;
				line.write(buffer, 0, n);
			}
			line.drain();
			line.stop();
		}catch(Exception e){
			System.out.println(e);
		}
		callback.audioDonePlaying();
	}
	public void stopRunning(){
		running = false;
	}
	AsyncAudioPlayer(BufferedInputStream stream, AudioEditor callback){
		this.callback = callback;
		try{
			in = AudioSystem.getAudioInputStream(stream);
			outFormat = OE_Audio.rateMatchedLineFormat(in.getFormat());
			SourceDataLine.Info info = new SourceDataLine.Info(SourceDataLine.class, outFormat);
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(outFormat);
			//System.out.println(Arrays.toString(AudioSystem.getAudioFileTypes()));
			System.out.println(in+" "+in.getFormat());
		}catch(Exception e){
			System.out.println(e);
		}
	}
}
