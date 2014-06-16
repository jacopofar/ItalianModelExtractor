package com.github.jacopofar.italianmodelgenerator;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.xml.sax.SAXException;

public class VerbExtractor {

	private static BufferedWriter wr;
	private static String delim="\t";
	private static BufferedWriter wrerr;
	/**
	 * Tool to extract italian verb forms from a dump of en.wiktionary
	 * Example output:
	 * 
	 * It reads the XML file line per line and parse with regular expressions, without using SAX
	 * 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if(args.length!=2){
			System.err.println("Wrong usage, need 2 arguments:");
			System.err.println("The path of the XML dump (compressed or not) of en.wiktionary");
			System.err.println("The path of the verb conjugation TSV to be generated");
			System.exit(1);
		}
		VerbExtractor.extractItalianVerbForms(args[1], args[0],"\t");

	}

	private static int managePage(String content, String title) throws IOException {
		if(content.contains("present participle of|andare"))
			System.out.println("!!!");
		if(!content.contains("==Italian==")) return 0;
		String verbIT=null;
		try{
			verbIT=content.split("==Italian==")[1].split("===[Vv]erb===")[1];
			String[] verbParts = verbIT.split("===[^=]+===[^=]");
			verbIT="==verb=="+verbParts[0];
		}
		catch(ArrayIndexOutOfBoundsException e){
			return 0;
		}

		Set<ItalianVerbConjugation> verbs = ItalianVerbConjugation.parseWikiCode(title, verbIT);
		if(title==null)
			return 0;
		for(ItalianVerbConjugation v:verbs){
			wr.write(v.toStringRepresentation(delim)+delim+verbIT.replace(delim, "").replace("\n", "")+"\n");
		}
		//the verb was not found, let's write it into a file to allow further improvement of the extractor
		if(verbs.size()==0){
			wrerr.write(title+delim+verbIT.replace(delim, "")+"\n");
		}

		return verbs.size();
	}

	/**
	 * Extract the Italian verb forms from a compressed en.wiktionary dump and saves them in a TSV file with these columns:
	 * COLUMNS MEANING:
	 * 1 - conjugated form
	 * 2 - infinitive form
	 * 3 - person number (1,2,3)
	 * 4 - plural or singular (p,s)
	 * 5 - verbal form (indicative, subjunctive, etc.)
	 * 6 - time, when possible with the verbal form,or an empty string
	 * @param delimiter 
	 * @param mw 
	 * */
	public static int extractItalianVerbForms(String outputFilePath,String enWiktionaryXMLDumpPAth, String delimiter) throws IOException{
		delim=delimiter;
		String strLine;
		String title="";
		String content="";
		boolean insideArticle=false;
		File file = new File(outputFilePath);
		DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
		wr=new BufferedWriter(new OutputStreamWriter(out));

		File fileerr = new File(outputFilePath+".unrecognized");
		DataOutputStream outerr = new DataOutputStream(new FileOutputStream(fileerr));
		wrerr=new BufferedWriter(new OutputStreamWriter(outerr));


		//parameters to inform the user about the work status
		long start=System.currentTimeMillis();
		long lastMessage=start;
		int saw=0;

		//open the en.wiktionary dump and read it line per line
		FileInputStream fstream = new FileInputStream(enWiktionaryXMLDumpPAth);
		BufferedInputStream in = new BufferedInputStream(fstream);
		Reader input;
		try {
			input = new InputStreamReader(new CompressorStreamFactory().createCompressorInputStream(in),"UTF-8");
		} catch (CompressorException e1) {
			System.err.println("Compression error, falling back to plain-text file reading");
			e1.printStackTrace();
			input=new InputStreamReader(in);
		}
		BufferedReader br = new BufferedReader(input);
		int found=0;
		while ((strLine = br.readLine()) != null){

			if(lastMessage+5000<System.currentTimeMillis()){
				System.out.println(saw+" pages encountered, "+found+" verbal forms so far ("+Math.round(((double)saw/(double)(System.currentTimeMillis()-start))*1000)+" pages per second)");
				lastMessage=System.currentTimeMillis();
			}
			if(strLine.contains("<title>")){
				title=strLine.split("<title>")[1].split("</title>")[0];
			}
			if(strLine.contains("<text ")){
				insideArticle=true;
				content="";
				try{content=strLine.split(">")[1];}catch(ArrayIndexOutOfBoundsException e){}
				continue;
			}
			if(strLine.contains("</text>")){
				insideArticle=false;
				content+=strLine.split("<")[0];
				found+=managePage(content,title);
				saw++;
				continue;
			}
			if(insideArticle)content+=strLine;
		}
		wr.close();
		wrerr.close();
		out.close();
		outerr.close();
		System.out.println("Finished! It took "+(System.currentTimeMillis()-start)/1000 +" seconds");
		return found;
	}

}
