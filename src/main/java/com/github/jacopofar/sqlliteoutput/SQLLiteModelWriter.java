package com.github.jacopofar.sqlliteoutput;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

import com.github.jacopofar.italianmodelgenerator.ItalianVerbConjugation;
import com.github.jacopofar.italianmodelgenerator.ModelWriter;

public class SQLLiteModelWriter implements ModelWriter {

	private Connection connection;

	public SQLLiteModelWriter(String outputPath,String modelPart) throws ClassNotFoundException{
		Class.forName("org.sqlite.JDBC");
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:"+outputPath+"/it_"+modelPart+"_model.db");
			Statement statement = connection.createStatement();
			statement.executeUpdate("drop table if exists hyponyms");
			statement.executeUpdate("drop table if exists verb_conjugations");
			statement.executeUpdate("drop table if exists POS_tags");
			statement.executeUpdate("create table hyponyms (hyponym string, hyperonym string)");
			//statement.executeUpdate("create index i_hyperonym on hyponyms (hyperonym)");
			//statement.executeUpdate("create index i_hyponym on hyponyms (hyponym)");

			statement.executeUpdate("create table POS_tags (word string, types string)");
			//statement.executeUpdate("create index i_pos_word on POS_tags (word)");

			statement.executeUpdate("create table verb_conjugations (conjugated string, infinitive string,"
					+ "person integer,number char(1),form string)");
			connection.setAutoCommit(false);
			//statement.executeUpdate("create index i_conjugated on verb_conjugations (conjugated)");
			//statement.executeUpdate("create index i_infinitive on verb_conjugations (infinitive)");
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Error, the database '"+outputPath+"/it_model.db' could not be created");
		}
	}

	public void close(){
		System.out.println("creating indexes...");
		Statement statement;
		try {
			statement = connection.createStatement();
			statement.executeUpdate("create index i_hyperonym on hyponyms (hyperonym)");
			statement.executeUpdate("create index i_hyponym on hyponyms (hyponym)");
			statement.executeUpdate("create index i_pos_word on POS_tags (word)");
			statement.executeUpdate("create index i_conjugated on verb_conjugations (conjugated)");
			statement.executeUpdate("create index i_infinitive on verb_conjugations (infinitive)");
			connection.commit();
			System.out.println("Indexes created!");
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("SQL error while creating indexes, committing and closing the connection to SQLLite");
		}

	}

	@Override
	public void importPOSs(String path,String delimiter) throws IOException {
		FileReader fr=new FileReader(path);
		String line = null;
		try(BufferedReader br=new BufferedReader(fr)){

			int reads=0;
			PreparedStatement statement = connection.prepareStatement("insert into POS_tags values(?,?)");

			while((line=br.readLine())!=null){
				String[] parts = line.split(delimiter);
				statement.setString(1, parts[0]);
				statement.setString(2, parts[1]);
				statement.addBatch();
				reads++;
				if(reads%2000==0){
					statement.executeBatch();
					statement.clearBatch();
					connection.commit();
					System.out.println("["+LocalDateTime.now().toString()+"] - Imported "+reads+" POS tags forms");	
				}
			}
			statement.executeBatch();
			statement.clearBatch();
			connection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Data type error while reading "+line);
		}

	}

	@Override
	public void importVerbConjugations(String path,String delimiter) throws IOException {
		FileReader fr=new FileReader(path);
		String line = null;
		try(BufferedReader br=new BufferedReader(fr)){
			int reads=0;
			PreparedStatement statement = connection.prepareStatement("insert into verb_conjugations values(?,?,?,?,?)");
			while((line=br.readLine())!=null){
				//conjugated string, infinitive string,person integer,number char(1),form string,time string
				ItalianVerbConjugation ic = new ItalianVerbConjugation(line,delimiter);
				statement.setString(1, ic.getConjugated());
				statement.setString(2, ic.getInfinitive());
				statement.setInt(3, ic.getPerson());
				statement.setString(4, ic.getNumber()+"");
				statement.setString(5, ic.getMode());


				statement.addBatch();
				reads++;
				if(reads%2000==0){
					statement.executeBatch();
					statement.clearBatch();
					connection.commit();
					System.out.println("["+LocalDateTime.now().toString()+"] - Imported "+reads+" verbal forms");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Data type error while reading "+line);
		}


	}

	@Override
	public void importIsAs(String path,String delimiter) throws IOException {
		FileReader fr=new FileReader(path);
		String line = null;
		try(BufferedReader br=new BufferedReader(fr)){
			int reads=0;
			PreparedStatement statement = connection.prepareStatement("insert into hyponyms values(?,?)");
			while((line=br.readLine())!=null){
				String[] parts = line.split(delimiter);
				statement.setString(1, parts[0]);
				statement.setString(2, parts[1]);
				statement.addBatch();
				reads++;
				if(reads%2000==0){
					statement.executeBatch();
					statement.clearBatch();
					connection.commit();
					System.out.println("["+LocalDateTime.now().toString()+"] - Imported "+reads+" IsAs relationships");
				}

			}
			statement.executeBatch();
			statement.clearBatch();
			connection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Data type error while reading "+line);
		}

	}
}
