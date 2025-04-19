/* --
COMP336 Lab1 Exercise
Student Name:
Student ID:
Section:
Email:
*/

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

class Posting implements Serializable
{
	public String doc;
	public int freq;
	Posting(String doc, int freq)
	{
		this.doc = doc;
		this.freq = freq;
	}
}

public class InvertedIndex
{
	private RecordManager recman;
	private HTree hashtable;

	InvertedIndex(String recordmanager, String objectname) throws IOException
	{
		recman = RecordManagerFactory.createRecordManager(recordmanager);
		long recid = recman.getNamedObject(objectname);

		if (recid != 0)
			hashtable = HTree.load(recman, recid);
		else
		{
			hashtable = HTree.createInstance(recman);
			recman.setNamedObject( objectname, hashtable.getRecid() );
		}
	}


	public void finalize() throws IOException
	{
		recman.commit();
		recman.close();				
	} 

	public void addEntry(String word, int x, int y) throws IOException
	{
		// Add a "docX Y" entry for the key "word" into hashtable
		Object obj = hashtable.get(word);
		List<Posting> postings;

		// Ensure the retrieved object is a List<Posting>
		if (obj instanceof List) {
			postings = (List<Posting>) obj;
		} else {
			postings = new ArrayList<>();
		}
		Posting newPosting = new Posting("doc" + x, y);
		// Check if the exact entry already exists before adding
		if (!postings.contains(newPosting)) {
			postings.add(newPosting);
			hashtable.put(word, postings);
			recman.commit(); // Ensure persistence
		}

	}
	public void delEntry(String word) throws IOException
	{
		// Delete the word and its list from the hashtable
		hashtable.remove(word);
		recman.commit();
	} 
	public void printAll() throws IOException
	{
		// Print all the data in the hashtable
		// ADD YOUR CODES HERE
		// iterate through all keys
		FastIterator iter = hashtable.keys();
		String key;
		if (iter == null) {
			System.out.println("Hashtable is empty.");
			return;
		}
		while ((key = (String) iter.next()) != null) {
			List<Posting> postings = (List<Posting>) hashtable.get(key);

			if (postings != null && !postings.isEmpty()) {
				StringBuilder result = new StringBuilder(key + " =");

				for (Posting posting : postings) {
					result.append(" ").append(posting.doc).append(" ").append(posting.freq);
				}

				System.out.println(result.toString());
			}
		}
	}

	public static void main(String[] args)
	{
		try
		{
			InvertedIndex index = new InvertedIndex("lab1","ht2");
	
			index.addEntry("cat", 2, 6);
			index.addEntry("dog", 1, 33);
			System.out.println("First print");
			index.printAll();
			
			index.addEntry("cat", 8, 3);
			index.addEntry("dog", 6, 73);
			index.addEntry("dog", 8, 83);
			index.addEntry("dog", 10, 5);
			index.addEntry("cat", 11, 106);
			System.out.println("Second print");
			index.printAll();
			
			index.delEntry("dog");
			System.out.println("Third print");
			index.printAll();
			index.finalize();
		}
		catch(IOException ex)
		{
			System.err.println(ex.toString());
		}

	}
}
