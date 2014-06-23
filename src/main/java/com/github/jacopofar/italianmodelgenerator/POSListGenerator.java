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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.xml.sax.SAXException;

public class POSListGenerator {

	private static BufferedWriter wr;
	private static String delim="\t";


	/**
	 * Small tool to extract a list of word roles from an XML dump of en.wiktionary
	 * It's made for Italian but should work for other languages with mimimal changes.
	 * 
	 * Example output:
	 * 
	 * It reads the XML file line per line and parse with regular expressions, without using SAX
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if(args.length!=2){
			System.err.println("Wrong usage, need 2 arguments:");
			System.err.println("The path of the XML dump (compressed or not) of en.wiktionary");
			System.err.println("The path of the POS dictionary to be generated");
			System.exit(1);
		}

	}

	/**
	 * Extracts Italian POS tags from the given page and saves them, if any, in the writer
	 * */
	private static int managePage(String content, String title) throws IOException {
		if(!content.contains("==Italian==")) return 0;
		String italianSegment=content.split("==Italian==")[1];
		italianSegment=italianSegment.split("[^=]==[^=]")[0];
		Matcher m = Pattern.compile("===([a-zA-Z ]+)===").matcher(italianSegment);
		String add="";
		while (m.find()) {
			String maybe=m.group().toLowerCase().replace("=", "");
			if(maybe.equals("adverb") && !add.contains(maybe))add+=","+maybe;
			if(maybe.equals("adjective") && !add.contains(maybe))add+=","+maybe;
			if(maybe.equals("noun") && !add.contains(maybe))add+=","+maybe;
			if(maybe.equals("verb") && !add.contains(maybe))add+=","+maybe;
			if(maybe.equals("proper noun") && !add.contains(maybe))add+=","+maybe;
			if(maybe.equals("conjunction") && !add.contains(maybe))add+=","+maybe;
			if(maybe.equals("verb form") && !add.contains(maybe))add+=","+"verb"; //don't care if it's a form. Moreover, is not always used
			if(maybe.equals("pronoun") && !add.contains(maybe))add+=","+maybe;
			if(maybe.equals("article") && !add.contains(maybe))add+=","+maybe;
			if(maybe.equals("preposition") && !add.contains(maybe))add+=","+maybe;
			if(maybe.equals("interjection") && !add.contains(maybe))add+=","+maybe;
		}
		if(add.length()>0){
			wr.write(title+delim);
			add=add.substring(1);
			wr.write(add);
			wr.write("\n");
			return 1;
		}
		else{
			System.out.println("--didn't understand the word type (POS) of: "+title);
			return 0;
		}
	}

	public static int extractPOS(String outputFilePath,String enWiktionaryXMLDumpPAth, String delimiter) throws IOException{
		delim=delimiter;
		String strLine;
		String title="";
		String content="";
		boolean insideArticle=false;
		File file = new File(outputFilePath);
		DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
		wr=new BufferedWriter(new OutputStreamWriter(out));
		//parameters to inform the user about the work status
		long start=System.currentTimeMillis();
		long lastMessage=start;
		int saw=0;
		int written=0;

		//open the en.wiktionary dump and read it line per line
		FileInputStream fstream = new FileInputStream(enWiktionaryXMLDumpPAth);
		BufferedInputStream in = new BufferedInputStream(fstream);
		Reader input;
		try {
			input = new InputStreamReader(new CompressorStreamFactory().createCompressorInputStream(in),"UTF-8");
		} catch (CompressorException e1) {
			System.err.println("Compression error, falling back to plain-text reading");
			e1.printStackTrace();
			input=new InputStreamReader(in);
		}
		BufferedReader br = new BufferedReader(input);

		while ((strLine = br.readLine()) != null){

			if(lastMessage+5000<System.currentTimeMillis()){
				System.out.println(saw+" pages encountered ("+Math.round(((double)saw/(double)(System.currentTimeMillis()-start))*1000)+" pages per second)");
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
				written+=managePage(content,title);
				saw++;
				continue;
			}
			if(insideArticle)content+=strLine;
		}
		wr.close();
		out.close();
		System.out.println("Finished! It took "+(System.currentTimeMillis()-start)/1000 +" seconds");
		return written;
	}

}
