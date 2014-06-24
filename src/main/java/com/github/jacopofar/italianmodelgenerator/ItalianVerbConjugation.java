package com.github.jacopofar.italianmodelgenerator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an Italian verb conjugation and provides helper methods to write/read it using strings with delimiters and parse en.wiktionary descriptions
 * */
public class ItalianVerbConjugation {
	public enum Mode {
		INFINITIVE,
		GERUND,
		PRESENT_PARTICIPLE,
		PAST_PARTICIPLE,
		INDICATIVE_PRESENT,
		INDICATIVE_IMPERFECT,
		INDICATIVE_PAST_HISTORIC,
		INDICATIVE_FUTURE,
		CONDITIONAL_PRESENT,
		SUBJUNCTIVE_PRESENT,
		SUBJUNCTIVE_IMPERFECT,
		IMPERATIVE
	}
	private String conjugated;
	private String infinitive;
	private char number='-';
	private int person=0;
	private Mode mode;
	private static HashMap<Mode,String> modeRepresentations;
	static{
		modeRepresentations=new HashMap<Mode,String>(11);
		modeRepresentations.put(Mode.INFINITIVE,"infinitive");
		modeRepresentations.put(Mode.IMPERATIVE,"imperative");
		modeRepresentations.put(Mode.GERUND,"gerund");
		modeRepresentations.put(Mode.PRESENT_PARTICIPLE,"present participle");
		modeRepresentations.put(Mode.PAST_PARTICIPLE,"past participle");
		modeRepresentations.put(Mode.INDICATIVE_PRESENT,"indicative present");
		modeRepresentations.put(Mode.INDICATIVE_IMPERFECT,"indicative imperfect");
		modeRepresentations.put(Mode.INDICATIVE_PAST_HISTORIC,"indicative past historic");
		modeRepresentations.put(Mode.INDICATIVE_FUTURE,"indicative future");
		modeRepresentations.put(Mode.CONDITIONAL_PRESENT,"conditional");
		modeRepresentations.put(Mode.SUBJUNCTIVE_PRESENT,"subjunctive present");
		modeRepresentations.put(Mode.SUBJUNCTIVE_IMPERFECT,"subjunctive imperfect");
	}
	/**
	 * Instantiates a verb conjugation object starting from the string representation from the toStringRepresentation method
	 * */
	public ItalianVerbConjugation(String line, String delimiter) {
		try{
			String[] data = line.split(delimiter);
			this.conjugated=data[0];
			this.infinitive=data[1];
			this.setMode(data[2]);
			this.number=data[3].charAt(0);
			this.person=Integer.parseInt(data[4]);
		}catch(Exception e){
			System.err.println("Offending verb string: "+line);
			e.printStackTrace();
			throw new RuntimeException("Error while instantiating the verb object");
		}
	}

	public ItalianVerbConjugation() {}

	/**
	 * Parse a portion of wikicode and return the corresponding italian verb conjugation, if any
	 * @param title the title of the en.wiktionary page the snippet was extracted from
	 * @param wikiCode the wikicode snippet containing the verb conjugation(s) to extract
	 * */
	public static Set<ItalianVerbConjugation> parseWikiCode(String title,String wikiCode){
		HashSet<ItalianVerbConjugation> ics = new HashSet<ItalianVerbConjugation>();
		for(String candidateVerb:wikiCode.split("#")){
			Set<ItalianVerbConjugation> cvs=extractVerbs(candidateVerb);
			for(ItalianVerbConjugation cv:cvs){
				if(cv==null)
					continue;
				//a candidate verb was extracted, is it valid? If so, add it to the list
				//a verb with no mode is an error
				if(cv.mode==null||cv.infinitive==null)
					continue;
				//dirty text? ignore
				if((cv.conjugated+cv.infinitive).indexOf('?')!=-1)
					continue;
				//a verb mode without person data is OK
				if(cv.mode==Mode.GERUND || cv.mode==Mode.INFINITIVE|| cv.mode==Mode.PAST_PARTICIPLE|| cv.mode==Mode.PRESENT_PARTICIPLE){
					ics.add(cv);
					continue;
				}

				//the verb has a person, is checked for it
				if(cv.number=='-' || cv.person==0 || cv.mode==null)
					continue;
				cv.setConjugated(title);
				ics.add(cv);

			}
		}


		//
		return ics;
	}
	private static Set<ItalianVerbConjugation> extractVerbs(String candidateVerb) {
		HashSet<ItalianVerbConjugation> results=new HashSet<ItalianVerbConjugation>();
		//a language is defined in this snippet, but is not Italian, skip it now
		if(candidateVerb.contains("lang=") && !candidateVerb.contains("lang=it"))
			return results;
		//parse the {{conjugation of}} template
		//examples of template usage
		//{{conjugation of|1 vedere    |2|3 1|4 s|5 cond|6 lang=it}}
		//{{conjugation of|1 accrescere|2|3 3|4 s|5 pres|6 ind|lang=it}}
		//{{conjugation of|valere  ||3|s|past|historic|ind|lang=it}}
		//{{conjugation of|motivare||1|s|pres|act|ind|lang=it}}
		Matcher m = Pattern.compile(".*\\{\\{(conjugation of\\|[^\\}]+)\\}\\}").matcher(candidateVerb.replace("lang=it|", ""));
		while (m.find()) {
			String t=m.group();
			String[] data=t.toLowerCase().split("\\|");
			ItalianVerbConjugation ic = new ItalianVerbConjugation();
			ic.infinitive=data[1];
			try{
				ic.setNumber(data[4]);
				ic.setPerson(data[3]);
			}catch(NumberFormatException e){
				e.printStackTrace();
				System.err.println("Offending verb:"+candidateVerb);
			}
			catch(RuntimeException e){
				e.printStackTrace();
				System.err.println("Offending verb:"+candidateVerb);
			}

			if(t.contains("|imperf|ind|"))ic.setMode(Mode.INDICATIVE_IMPERFECT);
			if(t.contains("|[[past historic]]|"))ic.setMode(Mode.INDICATIVE_PAST_HISTORIC);
			if(t.contains("|fut|"))ic.setMode(Mode.INDICATIVE_FUTURE);
			if(t.contains("|cond|"))ic.setMode(Mode.CONDITIONAL_PRESENT);
			if(t.contains("|pres|sub|"))ic.setMode(Mode.SUBJUNCTIVE_PRESENT);
			if(t.contains("|imperf|sub|"))ic.setMode(Mode.SUBJUNCTIVE_IMPERFECT);
			if(t.contains("|pres|ind|"))ic.setMode(Mode.INDICATIVE_PRESENT);
			if(t.contains("|imp|"))ic.setMode(Mode.IMPERATIVE);
			results.add(ic);
		}

		//pattern example: {{gerund of|psicologizzare|...}}
		m = Pattern.compile(".*\\{\\{(gerund of\\|[^\\}]+)\\}\\}").matcher(candidateVerb);
		while (m.find()) {
			ItalianVerbConjugation ic = new ItalianVerbConjugation();
			ic.setInfinitive(m.group().split("\\|")[1].toLowerCase());
			ic.setMode(Mode.GERUND);
			results.add(ic);
		}

		//pattern: {{present participle of|psicologizzare|...}}
		m = Pattern.compile("\\{\\{(present participle of\\|[^\\}]+)\\}\\}").matcher(candidateVerb);
		while (m.find()) {
			ItalianVerbConjugation ic = new ItalianVerbConjugation();
			ic.setInfinitive(m.group().split("\\|")[1].toLowerCase());
			ic.setMode(Mode.PRESENT_PARTICIPLE);
			results.add(ic);
		}


		//pattern: {{past participle of|psicologizzare|...}}
		m = Pattern.compile("\\{\\{(past participle of\\|[^\\}]+)\\}\\}").matcher(candidateVerb);
		while (m.find()) {
			ItalianVerbConjugation ic = new ItalianVerbConjugation();
			ic.setInfinitive(m.group().split("\\|")[1].toLowerCase());
			ic.setMode(Mode.PAST_PARTICIPLE);
			results.add(ic);
		}

		//this wasn't a template, let's try to match the textual description

		String desc=candidateVerb.toLowerCase();
		//often a verb has multiple person (for example the verb "vada" is currently described as "first-person singular, second-person singular and third-person singular present subjunctive of andare"
		String multiplePersons="";
		if(desc.contains("first-person"))multiplePersons+="1";
		if(desc.contains("second-person"))multiplePersons+="2";
		if(desc.contains("third-person"))multiplePersons+="3";
		//one or more person? iterate over them
		if(multiplePersons.length()>0){
			for(char p:multiplePersons.toCharArray()){
				ItalianVerbConjugation ic = new ItalianVerbConjugation();
				
				ic.person=Integer.parseInt(p+"");
				
				if(desc.contains("singular"))ic.number='s';
				if(desc.contains("plural"))ic.number='p';

				if(desc.contains("indicative present"))ic.mode=Mode.INDICATIVE_PRESENT;
				if(desc.contains("present tense"))ic.mode=Mode.INDICATIVE_PRESENT;
				if(desc.contains("imperfect tense"))ic.mode=Mode.INDICATIVE_IMPERFECT;
				if(desc.contains("past participle"))ic.mode=Mode.PAST_PARTICIPLE;
				if(desc.contains("present subjunctive"))ic.mode=Mode.SUBJUNCTIVE_PRESENT;


				//let's find the infinitive, it ends with "are", "ere" or "ire"
				m=Pattern.compile("([a-z]+[aei]re)").matcher(desc);
				while (m.find()) {
					ic.infinitive=m.group();
				}
				results.add(ic);
			}
		}
		else{
			//no persons found, it may be an impersonal form like infinitive, or just a string with no verbs in it
			ItalianVerbConjugation ic = new ItalianVerbConjugation();
			
			if(desc.contains("singular"))ic.number='s';
			if(desc.contains("plural"))ic.number='p';

			if(desc.contains("indicative present"))ic.mode=Mode.INDICATIVE_PRESENT;
			if(desc.contains("present tense"))ic.mode=Mode.INDICATIVE_PRESENT;
			if(desc.contains("imperfect tense"))ic.mode=Mode.INDICATIVE_IMPERFECT;
			if(desc.contains("past participle"))ic.mode=Mode.PAST_PARTICIPLE;
			if(desc.contains("present subjunctive"))ic.mode=Mode.SUBJUNCTIVE_PRESENT;


			//let's find the infinitive, it ends with "are", "ere" or "ire"
			m=Pattern.compile("([a-z]+[aei]re)").matcher(desc);
			while (m.find()) {
				ic.infinitive=m.group();
			}
			results.add(ic);
		}
		return results;

	}

	private void setMode(Mode m) {
		this.mode=m;
	}

	private void setNumber(String numberString) {
		setNumber(numberString.charAt(0));

	}
	private void setPerson(String personString) {
		if(personString.matches("1|2|3"))
			setPerson(Integer.parseInt(personString));
		else
			throw new RuntimeException("Error, the person "+personString+" is not a valid one");

	}
	public String getConjugated() {
		return conjugated;
	}
	public void setConjugated(String conjugated) {
		this.conjugated = conjugated;
	}
	public String getInfinitive() {
		return infinitive;
	}
	public void setInfinitive(String infinitive) {
		this.infinitive = infinitive;
	}
	public char getNumber() {
		return number;
	}
	public void setNumber(char number) {
		this.number = number;
	}
	public int getPerson() {
		return person;
	}
	public void setPerson(int person) {
		this.person = person;
	}

	public String toStringRepresentation(String delim) {
		return this.conjugated+delim+
				this.infinitive+delim+
				this.getMode()+delim+
				this.number+delim+
				this.person;
	}
	/**
	 * Returns the mode and the time of the conjugation, for example "indicative past historic" 
	 * */
	public String getMode() {
		return modeRepresentations.get(mode);
	}
	void setMode(String repr) {
		for(Entry<Mode, String> mr:modeRepresentations.entrySet()){
			if(mr.getValue().equals(repr)){
				this.mode= mr.getKey();
				return;
			}
		}
		throw new RuntimeException("ERROR, the verbal mode '"+repr+"' is unknown");
	}

	public String toString(){
		return this.toStringRepresentation(",");
	}
}
