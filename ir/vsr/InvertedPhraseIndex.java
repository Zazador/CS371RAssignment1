/**
 * Name: Zach Zador
 * UTEID: zaz78
 * CSID: sakz
 */

package ir.vsr;

import java.io.*;
import java.util.*;
import java.lang.*;

import ir.utilities.*;
import ir.classifiers.*;

public class InvertedPhraseIndex extends InvertedIndex {
    public InvertedPhraseIndex(File dirFile, short docType, boolean stem, boolean feedback) {
        super(dirFile, docType, stem, feedback);
    }
  
    /**
     * The maximum number of phrases to be remembered
     */
    public static final int maxPhrases = 1000;
    
    /**
     * Index a directory of files and then interactively accept retrieval queries.
     * Command format: "InvertedPhraseIndex [OPTION]* [DIR]" where DIR is the name of
     * the directory whose files should be indexed, and OPTIONs can be
     * "-html" to specify HTML files whose HTML tags should be removed.
     * "-stem" to specify tokens should be stemmed with Porter stemmer.
     * "-feedback" to allow relevance feedback from the user.
     */
    public static void main(String[] args) {
        // Parse the arguments into a directory name and optional flag

        String dirName = args[args.length - 1];
        short docType = DocumentIterator.TYPE_TEXT;
        boolean stem = false, feedback = false;
        for (int i = 0; i < args.length - 1; i++) {
            String flag = args[i];
            if (flag.equals("-html"))
                // Create HTMLFileDocuments to filter HTML tags
                docType = DocumentIterator.TYPE_HTML;
            else if (flag.equals("-stem"))
                // Stem tokens with Porter stemmer
                stem = true;
            else if (flag.equals("-feedback"))
                // Use relevance feedback
                feedback = true;
            else {
                throw new IllegalArgumentException("Unknown flag: "+ flag);
            }
        }


        // Create an inverted phrase index for the files in the given directory.
        InvertedPhraseIndex index = new InvertedPhraseIndex(new File(dirName), docType, stem, feedback);
        // Interactively process queries to this index.
        index.processQueries();
    }

    /**
     * Index the documents in dirFile.
     */
    protected void indexDocuments() {
        if (!tokenHash.isEmpty() || !docRefs.isEmpty()) {
            // Currently can only index one set of documents when an index is created
            throw new IllegalStateException("Cannot indexDocuments more than once in the same InvertedIndex");
        }
        // Get an iterator for the documents
        DocumentIterator docIter = new DocumentIterator(dirFile, docType, stem);

        HashMapVector bigramVector = new HashMapVector();
        System.out.println("Reviewing documents in " + dirFile);
        // Loop to determine all bigrams and needed one word tokens
        while (docIter.hasMoreDocuments()) {
            FileDocument doc = docIter.nextDocument();

            // Create a document vector for this document that contains all of the document's bigrams
            System.out.print(doc.file.getName() + ",");
            bigramVector = doc.hashMapBigramVectorFromVector(bigramVector);
        }

        // Get the top common bigrams and place them in an ArrayList
        ArrayList<String> words = bigramVector.getCommonBigrams(maxPhrases);

        // Get an iterator for the documents
        DocumentIterator docIter2 = new DocumentIterator(dirFile, docType, stem);
        System.out.println("Indexing documents in " + dirFile);
        // Loop to find occurrences of only the most common bigrams from the previous
        // array list
        while (docIter2.hasMoreDocuments()) {
            FileDocument doc = docIter2.nextDocument();
            System.out.print(doc.file.getName() + ",");
            HashMapVector vector = doc.hashMapBigramVectorFromArrayList(words);
            indexDocument(doc, vector);
        }
        // Now that all documents have been processed, we can calculate the IDF weights for
        // all tokens and the resulting lengths of all weighted document vectors.
        computeIDFandDocumentLengths();
        System.out.print("Indexed " + docRefs.size() + " documents with " + size() + " unique terms.\n");
    }

    /**
     * Enter an interactive user-query loop, accepting queries and showing the retrieved
     * documents in ranked order.
     */
    public void processQueries() {

        System.out.println("Now able to process queries. When done, enter an empty query to exit.");
        // Loop indefinitely answering queries
        do {
            // Get a query from the console
            String query = UserInput.prompt("\nEnter query:  ");
            // If query is empty then exit the interactive loop
            if (query.equals(""))
                break;

            HashMapVector queryVector;
            // If there is a space in the query, then the user is searching for a bigram
            // therefore, make a bigram hashmapvector
            if (query.contains(" "))
                queryVector = (new TextStringDocument(query, stem)).hashMapBigramVector();
            // Otherwise, queryVector is a normal hashmapvector
            else 
                queryVector = (new TextStringDocument(query, stem)).hashMapVector();
            Retrieval[] retrievals = retrieve(queryVector);
            presentRetrievals(queryVector, retrievals);
        }
        while (true);
    }
}
