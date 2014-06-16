package com.github.jacopofar.italianmodelgenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONException;
import org.json.JSONObject;

public class ExtractStupidDictionary {
	/**
	 * Takes a conceptnet raw jsons folder and a language (for example 'it') and returns the English-to-language mapping
	 * An Wnglish word will be mapped to 1 or more words in the desired language, delimited with a pipe
	 * Be careful, returned dictionary will be very ambiguous
	 * */
	public static HashMap<String,String> extractDictionary(String folderPath,String language){
		HashMap<String,String> dictionary=new HashMap<String,String>(10000);
		for(File f:new File(folderPath).listFiles()){
			System.out.println("reading input file "+f.getName()+"...");
			String line=null;
			try {
				FileReader fr=new FileReader(f);
				try(BufferedReader br=new BufferedReader(fr)){
					int numRead=0;
					while((line=br.readLine())!=null){
						numRead++;
						if(numRead%10000==0) System.out.println("read "+numRead+" lines of this specific file, "+dictionary.size()+" total dictionary entries so far");
						//let's check that it goes from english to desired language and is a translation-of relationship, before creating an expensive JSON object
						if(!line.contains("/r/TranslationOf") || !line.contains("\"start\": \"/c/"+language+"/")
								|| !line.contains("\"end\": \"/c/en/")) continue;
						JSONObject j=new JSONObject(line);
						String end=j.getString("start");
						String start=j.getString("end");
						if(end==null || start==null){
							System.err.println("Error, 'start' and 'end' keys not found or null as root keys in '"+line+"' skipped...");
							continue;
						}
						end=end.replace("/c/"+language+"/","");
						start=start.replace("/c/en/","");
						//information about plural forms not necessary
						//for(String part:start.split("/")){
						String part=start.split("/")[0];
						if (part.length()==1) continue;
						if(!part.startsWith("plural_form_of_") && !part.startsWith("plural_of_")){
							if(dictionary.containsKey(part) && !dictionary.get(part).contains(end))
								dictionary.put(part, dictionary.get(part)+"|"+end);
							else
								dictionary.put(part, end);
						}
					}
				}catch (IOException e) {
					System.err.println("Error while reading "+f.getName()+", check permissions. The content has been skipped");
				} catch (JSONException e) {
					System.err.println("Error while parsing the JSON string '"+line+"' (is this a ConceptNet raw .jsons dump?), ignored...");
				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.err.println("Error while reading "+f.getName()+", has it been deleted? Skipped...");
			} 
		}
		return dictionary;
	}

	public static void applyCleanRules(HashMap<String, String> dict,
			String rulesFile) throws IOException {
		int removedDictEntries=0;
		FileReader fr=new FileReader(rulesFile);
		BufferedReader br=new BufferedReader(fr);
		String line=null;
		while((line=br.readLine())!=null){
			if(line.startsWith("#") || line.length()==0) continue;
			if(line.startsWith("REMOVEDICT")){
				String[] p=line.split(" ");
				if(!dict.containsKey(p[1]))
					continue;
				HashSet<String> mapped = new HashSet<String>(Arrays.asList(dict.get(p[1]).split("\\|!")));
				mapped.remove(p[2]);
				removedDictEntries++;
				if(mapped.size()==0)
					dict.remove(p[1]);
				else{
					String newVal="";
					for(String m:mapped){
						newVal+="|"+m;
					}
					dict.put(p[1], newVal);
				}
			}
		}
		fr.close();
		System.out.println(removedDictEntries+" entries removed from the dictionary according to the rules");
	}
}
