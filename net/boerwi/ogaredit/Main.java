package net.boerwi.ogaredit;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.charset.Charset;
import java.io.BufferedWriter;
import javax.swing.JOptionPane;


/** Static entry class for OGAREdit*/
public class Main {
	/** Entry point for OGAREdit
	* @param args unused
	*/
	public static void main(String[] args) {
		Breakpad bp = new Breakpad("OGAREDIT_CRASHLOG_%d.txt");
		Thread.setDefaultUncaughtExceptionHandler(bp);
		System.out.println("OGAREdit");
		new OE_ViewControl_Swing((args.length > 0) ? args[0] : null);
	}
}
class Breakpad implements Thread.UncaughtExceptionHandler{
	private final String pathPattern;
	Breakpad(String pathPattern){
		this.pathPattern = pathPattern;
	}
	@Override
	public void uncaughtException(Thread T, Throwable e){
		String message;
		if(e instanceof AssertionError){
			message = String.format("OGAREdit has crashed because it entered an unsupported state protected by an assertion.\nThe assertion message was: %s.\n", e.getMessage());
		}else{
			message = String.format("OGAREdit has crashed.\n%s\n", e.toString());
		}
		Path logPath = Path.of(System.getProperty("user.home"), String.format(pathPattern, System.currentTimeMillis()/(long)1000));
		try{
			BufferedWriter log = Files.newBufferedWriter(logPath, Charset.forName("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			log.write(message);
			log.write("BEGIN STACKTRACE\n");
			for(StackTraceElement elem : e.getStackTrace()){
				log.write(elem.toString()+"\n");
			}
			log.write("END STACKTRACE\n");
			log.close();
			message = message + "A crashlog has been written to "+logPath+".\n";
		}catch(Exception exc){
			message = message + "Crashlog failed to write: "+ exc.toString();
		}
		System.out.println(message);
		JOptionPane.showMessageDialog(null, message);
	}
}
