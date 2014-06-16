package com.github.jacopofar.italianmodelgenerator;

import java.io.IOException;


public interface ModelWriter {
//	/**
//	 * Stores an IsA relationship (hyponym-hypernym)
//	 * */
//	public void storeIsA(String hyponym,String hypernym);
	
//	/**
//	 * Stores the POS tags (as a list of comma-separated values)
//	 * */
//	public void storePOSTags(String word,String POStags);
//	/**
//	 * Stores the verb conjugation
//	 * */
//	public void storeVerbConjugation(String conjugated, String  infinitive,int person,char number,String form,String time);
//
	/**
	 * Imports the specified POS file in the model
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 * */
	public void importPOSs(String string,String delimiter) throws IOException;

	/**
	 * Imports the specified Verb conjugations file in the model
	 * @throws IOException 
	 * */
	public void importVerbConjugations(String string,String delimiter) throws IOException;

	/**
	 * Imports the specified Hyponym/hypernym file in the model
	 * @throws IOException 
	 * */
	public void importIsAs(String string,String delimiter) throws IOException;

	/**
	 * Close the model, possibly saving pending commits
	 * */
	public void close();
}
