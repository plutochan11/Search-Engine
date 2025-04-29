package hk.ust.csit5930.utils;
import java.io.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;

import hk.ust.csit5930.IRUtilities.Porter;

public class StopStem
{
	private Porter porter;
	private HashSet<String> stopWords;
	public boolean isStopWord(String str)
	{
		return stopWords.contains(str);	
	}
	public StopStem(String str)
	{
		super();
		porter = new Porter();
		stopWords = new HashSet<String>();

		// use BufferedReader to extract the stopwords in stopwords.txt (path passed as parameter str)
		// add them to HashSet<String> stopWords
		// MODIFY THE BELOW CODE AND ADD YOUR CODES HERE
		try {
			BufferedReader reader = new BufferedReader(new FileReader(str));
			String curr;
			while ((curr = reader.readLine()) != null) {
				stopWords.add(curr.trim());
			}
			reader.close();
		} catch (FileNotFoundException e) {
			System.err.println("File not found: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("Error reading file: " + e.getMessage());
		}


	}
	public String stem(String str)
	{
		return porter.stripAffixes(str);
	}
	public static void main(String[] arg)
	{
		StopStem stopStem = new StopStem("stopwords.txt");
		String input="";
		try{
			do
			{
				System.out.print("Please enter a single English word: ");
				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				input = in.readLine();
				if(input.length()>0)
				{	
					if (stopStem.isStopWord(input))
						System.out.println("It should be stopped");
					else
			   			System.out.println("The stem of it is \"" + stopStem.stem(input)+"\"");
				}
			}
			while(input.length()>0);
		}
		catch(IOException ioe)
		{
			System.err.println(ioe.toString());
		}
	}
}
