package com.github.jacopofar.italianmodelgenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import com.github.jacopofar.sqlliteoutput.SQLLiteModelWriter;

import net.sf.extjwnl.JWNLException;

public class StartConversion {

	public static void main(String[] args) throws IOException, JWNLException {
		if(args.length!=3){
			System.err.println("wrong arguments. 3 arguments required:");
			System.err.println("1 - the path of the conceptnet jsons folder");
			System.err.println("2 - the path of the (compressed or not) en.wiktionary XML dump");
			System.err.println("3 - the path of the output folder");
			System.exit(1);
		}
		String jsonsFolder=args[0];
		String language="it";
		String workdir=args[2];
		String rulesFile=workdir+"cleanrules.txt";
		String wiktionaryPath=args[1];
		//conceptnet raw json is a folder of .jsons files
		//each row of those files represent a concept
		//we read the /r/TranslationOf concepts to create an English to Italian dictionary
		//then, in a second step, WordNet is used to get IsA relationships between English terms, mapped to relationships between Italian ones
		//this method is of course VERY imprecise, but the resulting wordnet is still better than nothing
		System.out.println("Extracting a stupid en->"+language+" dictionary...");
		HashMap<String, String> dict = ExtractStupidDictionary.extractDictionary(jsonsFolder,language);
		if(new File(rulesFile).exists()){
			System.out.println("Found cleanrules.txt, cleaning dictionary...");
			ExtractStupidDictionary.applyCleanRules(dict,rulesFile);
		}
		FileWriter fw =new FileWriter(new File(workdir+"dictionary.tsv"));
		for(Entry<String, String> e:dict.entrySet()){
			fw.write(e.getKey()+"\t"+e.getValue()+"\n");
		}
		fw.close();
		System.out.println("dictionary extracted, number of english words: "+dict.size());
		System.out.println("Extracting italian POS information from en.wiktionary...");

		//these two steps could be merged in a single one by reading the dictionary dump once
		int k=POSListGenerator.extractPOS(workdir+"POS_list.txt", wiktionaryPath,"\t");
		System.out.println("POS list extracted, number of words: "+k);
		System.out.println("Extracting italian verb conjugation information from en.wiktionary...");
		k=VerbExtractor.extractItalianVerbForms(workdir+"verb_conjugations.txt", wiktionaryPath,"\t");
		System.out.println("verb conjugations extracted, number of conjugations: "+k);

		System.out.println("reading the POS list to ignore IsA relationships between different parts of the speech...");
		PoSTagChecker posChecker = PoSTagChecker.getInstance(workdir+"POS_list.txt", new String[]{"verb","noun","adjective","adverb"});
		System.out.println("Extracting IsA relationships from wordnet using the POS data to filter them...");
		k=ExtractIsARelationships.extractIsARelationShips(dict,posChecker,workdir+"hyponyms.tsv","\t");
		System.out.println("Italian IsA relationships extracted, number of relationshisp found: "+k);
		System.out.println("TSV files generated, creating the corresponfing SQLLite database");
		ModelWriter mw = null;
		try {
			mw=new SQLLiteModelWriter(workdir);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			System.err.println("Error while instantiating SQLLite database, check your classpath and Maven build");
			System.exit(4);
		}
		System.out.println("exporting verb conjugations in a SQLLite DB...");
		mw.importVerbConjugations(workdir+"verb_conjugations.txt","\t");
		System.out.println("exporting PoS data in a SQLLite DB...");
		mw.importPOSs(workdir+"POS_list.txt","\t");
		System.out.println("exporting IsAs data in a SQLLite DB...");
		mw.importIsAs(workdir+"hyponyms.tsv","\t");
		mw.close();

	}

}
