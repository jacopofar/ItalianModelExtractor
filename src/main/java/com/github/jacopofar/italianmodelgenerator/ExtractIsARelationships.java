package com.github.jacopofar.italianmodelgenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

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

public class ExtractIsARelationships {
	private static HashMap<String,HashSet<String>> hypernymCache=new HashMap<String,HashSet<String>>(200);
	private static long MAX_CACHED_WORDS=3000;
	/**
	 * NOTE: this method was used to extract IsA relationships from ConceptNet,
	 * currently a different approach is adopted and this code is kept as a reference
	 * */
	public static void extractRelationShips(String jsonsFolder,
			HashMap<String, String> dict) throws IOException, JWNLException {
		FileWriter fw =new FileWriter(new File("hyponyms_1.tsv"));
		FileWriter fh =new FileWriter(new File("hyponyms_2.tsv"));

		int isA=0,matches=0;
		for(File f:new File(jsonsFolder).listFiles()){

			System.out.println("reading input file "+f.getName()+"...");
			String line=null;
			try {
				FileReader fr=new FileReader(f);
				try(BufferedReader br=new BufferedReader(fr)){
					int numRead=0;
					while((line=br.readLine())!=null){
						numRead++;
						if(numRead%10000==0) System.out.println("read "+numRead+" lines of this specific file, "+isA+" total is-a relationships found so far, "+matches+" match with the desired language terms");
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
						String endi=dict.get(end);
						String starti=dict.get(start);

						//now start and end contain the list of terms in the desired language delimited by pipes
						//if one of them has no correspondences, skip it
						if(starti==null || endi==null)
							continue;
						fh.write(start+"\t"+end+"\n");
						matches++;
						for(String s:starti.split("\\|"))
							for(String e:endi.split("\\|"))
								fw.write(s+"\t"+e+"\n");
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
		fw.close();
		fh.close();
	}

	/**
	 * Extracts all the IsA relationships with matches in the given dictionary
	 * @param delimiter 
	 * @param mw 
	 * @return the number of relationships found
	 * @throws IOException 
	 * */
	public static int extractIsARelationShips(HashMap<String, String> dict,PoSTagChecker posChecker,String hyponymPath, String delimiter) throws JWNLException, IOException {
		FileWriter fw =new FileWriter(new File(hyponymPath));
		int foundRels=0,dictReads=0,ignoredForPOS=0;
		Dictionary wordNetDictionary = Dictionary.getDefaultResourceInstance();
		for(Entry<String, String> e:dict.entrySet()){
			dictReads++;
			if(!isInWordNet(e.getKey(),wordNetDictionary))
				continue;
			//a word is present, let's take the hypernym list
			for(String hyp:getHypernyms(e.getKey(),wordNetDictionary)){
				if(dict.containsKey(hyp)){
					foundRels++;
					//we found an IsA relationship, let's save it
					for(String start:e.getValue().split("\\|"))
						for(String end:dict.get(hyp).split("\\|")){
							if(!posChecker.haveCommonPOS(end, start))
								ignoredForPOS++;
							else
								if(!start.equals(end)){
									fw.write(start+delimiter+end+delimiter+e.getKey()+delimiter+hyp+"\n");
								}
						}
					if(foundRels%1000==0)
						System.out.println("Found "+foundRels+" IsA relationships, "+dictReads+" words examined of "+dict.size()+". "+ignoredForPOS+" relationships ignored for non matching POS tags [cached words:"+hypernymCache.size()+"]");
				}
			}
		}
		fw.close();
		return foundRels;
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

	private static HashSet<String> getHypernyms(String word,Dictionary dictionary) throws JWNLException{
		if(hypernymCache.containsKey(word))
			return hypernymCache.get(word);
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
		return hypernyms;
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

}
