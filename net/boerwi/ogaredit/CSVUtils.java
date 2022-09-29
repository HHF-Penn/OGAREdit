package net.boerwi.ogaredit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.nio.charset.Charset;
import org.json.*;

/**
 * Utility class to export certain JSON patterns to CSV.
 * @see <a href=https://csv-spec.org/>https://csv-spec.org/</a>
 */
public abstract class CSVUtils{
	private CSVUtils(){}
	/** Line break string for CSV. */
	public static final String lineBreak = "\r\n"; //CRLF recommended for CSVs
	/** Performs best-effort conversion to a string through a variety of methods, then escapes the string for CSV usage.
	 * @param in Object to stringify (or a string).
	 * @return CSV-safe quoted string.
	 */
	public static String escapeString(Object in){
		if(in == null || in == JSONObject.NULL){
			return "NULL";
		}
		String str;
		if(in instanceof JSONString){
			str = ((JSONString) in).toJSONString();
		}else if(in.getClass().isArray()){
			str = (new JSONArray(in)).toString();
		}else{
			str = in.toString();
		}
		// I dislike this style of quote escaping, but it isn't my choice :)
		str = str.replaceAll("\"", "\"\"");
		// Apparently I shouldn't even escape newlines, if it is quoted
		// Universal quoting is not recommended, but I choose to anyway
		return forceCRLF("\""+str+"\"");
	}
	/** Converts standalone LFs to CRLFs.
	 * @param in Input string with LF or CRLF line endings.
	 * @return Output string with CRLF line endings.
	 */
	public static String forceCRLF(String in){
		//Remove all CR
		in = in.replaceAll("\r", "");
		//Convert all LF to CRLF
		return in.replaceAll("\n", lineBreak);
	}
	/** Converts a JSONObject to CSV. The keys for each top-level object get added as a new column. Every top-level value in the object should be a JSONObject too.
	 * @param o The JSON Object to convert.
	 * @param keyColumnName The name for the added column containing JSON keys.
	 * @param includeColumns Whitelist of column names to output, or NULL (to output all). 'keyColumnName' must be added manually, if desired.
	 * @return CSV-formatted UTF-8 encoded bytes.
	 */
	public static byte[] jsonToCSV(JSONObject o, String keyColumnName, String[] includeColumns){
		JSONArray transformed = new JSONArray();
		// Put each name as an entry of the object under 'keyColumnName' so that it will show up in the csv
		if(keyColumnName != null){
			for(String key : o.keySet()){
				JSONObject original = o.getJSONObject(key);
				JSONObject entry = new JSONObject(original, JSONObject.getNames(original));
				entry.put(keyColumnName, key);
				transformed.put(entry);
			}
		}
		return jsonToCSV(transformed, includeColumns);
	}
	/** Converts a JSONArray to CSV. The array should contain only JSONObjects. The keys of the included JSONObjects become columns in the resulting CSV.
	 * @param a The JSON Array to convert.
	 * @param includeColumns Whitelist of column names to output, or NULL (to output all).
	 * @return CSV-formatted UTF-8 encoded bytes.
	 */
	public static byte[] jsonToCSV(JSONArray a, String[] includeColumns){
		if(a.length() < 1){
			return new byte[0];
		}
		ArrayList<String> lines = new ArrayList<>();
		// Gather all the keys used in every object in 'a'
		LinkedHashSet<String> keys = new LinkedHashSet<>();
		for(Object obj : a){
			if( !(obj instanceof JSONObject) ){
				assert false : "invalid array member";
				return new byte[0];
			}
			JSONObject entry = (JSONObject) obj;
			keys.addAll( Arrays.asList(JSONObject.getNames(entry)) );
		}
		// Limit to only the specified columns if provided
		if(includeColumns != null){
			keys.retainAll(Arrays.asList(includeColumns));
		}
		
		// Construct a CSV header from the members of 'keys'
		String header = "";
		boolean firstEntry = true;
		for(String key : keys){
			if(!firstEntry){
				header = header + ",";
			}else{
				firstEntry = false;
			}
			header = header + escapeString(key);
		}
		lines.add(header);
		// Construct a new line for every entry in 'a'
		for(Object obj : a){
			firstEntry = true;
			String line = "";
			JSONObject entry = (JSONObject) obj;
			for(String key : keys){
				if(!firstEntry){
					line = line + ",";
				}else{
					firstEntry = false;
				}
				line = line + escapeString(entry.opt(key));
			}
			lines.add(line);
		}
		return String.join(lineBreak, lines).getBytes(Charset.forName("UTF-8"));
	}
}
