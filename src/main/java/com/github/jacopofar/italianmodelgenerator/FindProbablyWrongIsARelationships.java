package com.github.jacopofar.italianmodelgenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.PointerUtils;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.data.Word;
import net.sf.extjwnl.data.list.PointerTargetNode;
import net.sf.extjwnl.data.list.PointerTargetNodeList;
import net.sf.extjwnl.dictionary.Dictionary;

import org.json.JSONException;
import org.json.JSONObject;
/**
 * This program lists the mismatches between ConceptNet and WordNet IsA relationships.
 * It looks for this relationship between English words in a JSON ConceptNet dump and then looks for those words in Wordnet
 * When two words are considered hypernym/hyponym in ConceptNet and present as lemmas in WordNet it save the corresponding JSON in a positive or negative file
 * depending on the presence of the same IsA relationship also in WordNet
 * These two files, containing also the natural language description of the concept, can be later used to train models to identify errors
 * */
public class FindProbablyWrongIsARelationships {
	private static HashMap<String,HashSet<String>> hypernymCache=new HashMap<String,HashSet<String>>();
	private static long MAX_CACHED_WORDS=3000000;
	public static void extractRelationShips(String jsonsFolder, String outputFolder) throws JWNLException {
		FileWriter fNotFound = null;
		FileWriter fFound = null;
		try {
			fNotFound = new FileWriter(new File(outputFolder+"/not_found_in_wordnet.tsv"));
			fFound =new FileWriter(new File(outputFolder+"/found_in_wordnet.tsv"));
		} catch (IOException e1) {
			e1.printStackTrace();
			System.err.println("probably the paths are wrong or in a position this program doesn't have rights to write to");
			System.exit(1);
		}


		Dictionary wordNetDictionary = Dictionary.getDefaultResourceInstance();		
		int isA=0,wnMatch=0,wnNotMatch=0;
		for(File f:new File(jsonsFolder).listFiles()){

			System.out.println("reading input file "+f.getName()+"...");
			String line=null;
			try {
				FileReader fr=new FileReader(f);
				try(BufferedReader br=new BufferedReader(fr)){
					int numRead=0;
					while((line=br.readLine())!=null){
						numRead++;
						if(numRead%10000==0) System.out.println("read "+numRead+" lines of this specific file, "+isA+" total is-a relationships found so far, "+wnMatch+" match with wordnet, "+wnNotMatch+" present as word but not in a IsA relationhsip");
						//let's check that it goes from english to desired language and is a translation-of relationship, before creating an expensive JSON object
						if(!line.contains("\"rel\": \"/r/IsA\"") || !line.contains("\"start\": \"/c/en/")
								|| !line.contains("\"end\": \"/c/en/")) continue;
						isA++;
						JSONObject j=new JSONObject(line);
						String end=j.getString("start");
						String start=j.getString("end");
						if(end==null || start==null){
							System.err.println("Error, 'start' and 'end' keys not found or null as root keys in '"+line+"' skipped...");
							continue;
						}
						//ignore numbers
						if(end.matches("/c/en/[0-9]+") || start.matches("/c/en/[0-9]+"))
							continue;
						end=end.replace("/c/en/","");
						start=start.replace("/c/en/","");
						if(isInWordNet(end,wordNetDictionary) && isInWordNet(start,wordNetDictionary)){
							if(!isHypernym(end,start,wordNetDictionary)){
								fNotFound.write(end+"\t"+start+"\t"+j.getString("surfaceText")+"\n");
								wnNotMatch++;
							}
							else{
								fFound.write(end+"\t"+start+"\t"+j.getString("surfaceText")+"\n");
								wnMatch++;
							}
						}
					}
				}catch (IOException e) {
					System.err.println("Error while reading "+f.getName()+", check permissions. The content has been skipped");
				} catch (JSONException e) {
					System.err.println("Error while parsing the JSON string '"+line+"', ignored...");
				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.err.println("Error while reading "+f.getName()+", has it been deleted? Skipped...");
			} 
		}
		try {
			fNotFound.close();
			fFound.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("something bad happened while closing the output files...");
			System.exit(2);
		}
		

	}

	private static boolean isHypernym(String word,String possibleHypernym,Dictionary dictionary) throws JWNLException{
		if(hypernymCache.containsKey(word))
			return hypernymCache.get(word).contains(possibleHypernym);
		//if there are too many words in the cache, just empty it
		if(hypernymCache.size()>MAX_CACHED_WORDS)
			hypernymCache.clear();
		//no matches in the cache, let's look in wordnet

		HashSet<Synset> senses=getSenses(word,dictionary);
		HashSet<String> hypernyms=new HashSet<String>(5);
		for(Synset sense:senses){
			for(PointerTargetNodeList m:PointerUtils.getHypernymTree(sense).toList()){
				for(PointerTargetNode w:m){
					for(Word hypernym:w.getSynset().getWords())
						hypernyms.add(hypernym.getLemma());
				}
			}
		}
		//let's add the results to the cache
		hypernymCache.put(word,hypernyms);
		//return the result
		return hypernyms.contains(possibleHypernym);
	}

	private static HashSet<Synset> getSenses(String word,Dictionary dictionary) throws JWNLException{
		//we don't have an immediate way to get the part-of-speech tag of a word from Conceptnet, let's just look for each kind of PoS
		IndexWord nounCandidate = dictionary.getIndexWord(POS.NOUN, word);
		IndexWord adjectiveCandidate = dictionary.getIndexWord(POS.ADJECTIVE, word);
		IndexWord verbCandidate = dictionary.getIndexWord(POS.VERB, word);
		IndexWord adverbCandidate = dictionary.getIndexWord(POS.ADVERB, word);
		HashSet<Synset> senses=new HashSet<Synset>();
		if(nounCandidate!=null){
			for(Synset sense:nounCandidate.getSenses())
				senses.add(sense);	
		}
		if(adjectiveCandidate!=null){
			for(Synset sense:adjectiveCandidate.getSenses())
				senses.add(sense);	
		}
		if(verbCandidate!=null){
			for(Synset sense:verbCandidate.getSenses())
				senses.add(sense);	
		}
		if(adverbCandidate!=null){
			for(Synset sense:adverbCandidate.getSenses())
				senses.add(sense);	
		}
		return senses;
	}

	private static boolean isInWordNet(String word,Dictionary dictionary) throws JWNLException{
		IndexWord nounCandidate = dictionary.getIndexWord(POS.NOUN, word);
		IndexWord adjectiveCandidate = dictionary.getIndexWord(POS.ADJECTIVE, word);
		IndexWord verbCandidate = dictionary.getIndexWord(POS.VERB, word);
		IndexWord adverbCandidate = dictionary.getIndexWord(POS.ADVERB, word);
		if(nounCandidate!=null)return true;
		if(adjectiveCandidate!=null)return true;
		if(verbCandidate!=null)return true;
		if(adverbCandidate!=null)return true;
		return false;
	}

	public static void main(String argc[]){
		if(argc.length!=2 && argc.length!=3){
			System.out.println("wrong command line usage, I expected the path of the ConceptNet *.jsons folder, the path where to write the two output files and possibly a max cache size value");
			try{
			if(argc.length==3)
				MAX_CACHED_WORDS=Long.parseLong(argc[2]);
			}
			catch(NumberFormatException e){
				System.err.println("Error, "+argc[2]+" is not a number");
				System.exit(3);
			}
				
		}
		try {
			FindProbablyWrongIsARelationships.extractRelationShips(argc[0],argc[1]);
		} catch (JWNLException e) {
			e.printStackTrace();
			System.err.println("something went wrong while retrieving WordNet data");
			return;
		}
		System.out.println("The program terminated correctly");
	}


}
