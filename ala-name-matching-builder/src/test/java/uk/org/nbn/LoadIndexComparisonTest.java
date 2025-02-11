

package uk.org.nbn;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

/**
 * @author Natasha, Tommy
 */
public class LoadIndexComparisonTest {

    private final static String INDEX_TO_TEST_AGAINST = "/data/lucene/uksi-namatching-index-test/expected-load-index.csv";
    private final static String INDEX = "/data/lucene/nmload-tmp";

    @Test
    public void comparisonTest() throws Exception {
        IndexSearcher indexSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(INDEX).toPath())));
        CSVReader reader = new CSVReaderBuilder(new FileReader(INDEX_TO_TEST_AGAINST)).withSkipLines(1).build();
        int count = 0;
        String[] record;
        while ((record = reader.readNext()) != null && count++ < 1000000) {
            TermQuery tq = new TermQuery(new Term("id", record[0]));
            TopDocs hits = indexSearcher.search(tq, 1);
            Document doc2 = indexSearcher.doc(hits.scoreDocs[0].doc);
            assertEquals(record[0], Objects.toString(doc2.get("id"),""));
            assertEquals(record[1], Objects.toString(doc2.get("lsid"),""));
            assertEquals(record[2], Objects.toString(doc2.get("parent_id"),""));
            assertEquals(record[3], Objects.toString(doc2.get("name"),""));
            assertEquals(record[4], Objects.toString(doc2.get("author"),""));
            assertEquals(record[5], Objects.toString(doc2.get("name_complete"),""));
            assertEquals(record[6], Objects.toString(doc2.get("establishment_means"),""));
            assertEquals(record[7], Objects.toString(doc2.get("habitat"),""));
            assertEquals(record[8], Objects.toString(doc2.get("rank"),""));
            //???assertEquals(record[9], Objects.toString(doc2.get("rank_id"),""));
            assertEquals(record[10], Objects.toString(doc2.get("is_synonym"),""));
        }

    }

    /**
     * ***This is not part of the test. It was used to create a csv file from the temopary load index. Note: works with lucene 6.6.6
     */
//    public static void main(String[] args) throws Exception {
//        saveTempLoadIndexToFile("/data/lucene/nmload-tmp","/data/lucene/uksi-namatching-index-test/expected-load-index.csv");
//    }

    private static void saveTempLoadIndexToFile(String indexDirectory, String csvFilename) throws IOException{
        CSVWriter csvWriter = new CSVWriter(new FileWriter(csvFilename));
        IndexReader index1Reader = DirectoryReader.open(FSDirectory.open(new File(indexDirectory).toPath()));
        csvWriter.writeNext(new String[]{"id","lsid","parent_id","name","author","name_complete","establishment_means","habitat",
                                        "rank","rank_id","is_synonym"});
        int max = index1Reader.maxDoc()>1000000?1000000:index1Reader.maxDoc();
        for (int i = 0; i < max; i++) {
            Document doc1 = index1Reader.document(i);
            csvWriter.writeNext(new String[]{doc1.get("id"),doc1.get("lsid"),doc1.get("parent_id"),doc1.get("name"),doc1.get("author"),
                    doc1.get("name_complete"),doc1.get("establishment_means"),doc1.get("habitat"),doc1.get("rank"),
                    doc1.get("rank_id"),doc1.get("is_synonym")});
        }
        csvWriter.close();
    }

}
