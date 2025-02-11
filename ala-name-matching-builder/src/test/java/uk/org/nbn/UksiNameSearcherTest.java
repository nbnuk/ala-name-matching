

package uk.org.nbn;

import au.org.ala.names.model.*;
import au.org.ala.names.search.ALANameSearcher;
import au.org.ala.names.search.HomonymException;
import au.org.ala.names.search.SPPException;
import au.org.ala.names.search.SearchResultException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.commons.io.IOUtils;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.nameparser.PhraseNameParser;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assert.fail;

/**
 * @author Natasha, Tommy
 */
public class UksiNameSearcherTest {
    private static ALANameSearcher searcher;

    @org.junit.BeforeClass
    public static void init() throws Exception {
        searcher = new ALANameSearcher("/data/lucene/index-under-test");
    }


    @Test
    public void indexSmokeTest() throws Exception {

        String line;
        try (BufferedReader br = new BufferedReader(new FileReader("/data/lucene/uksi-namatching-index-test/derived-test-data-from-EXPECTED-INDEX-2021-12-13.txt"))) {

            ObjectMapper mapper = new ObjectMapper();
            CSVReader reader = new CSVReaderBuilder(new FileReader("/data/lucene/uksi-namatching-index-test/sources/vernacular.csv")).withSkipLines(1).build();
            int count = 0;
            String[] record;
            while ((record = reader.readNext()) != null && count++ < 1000000) {
                line = br.readLine();
                    NameSearchResult actualNsr = searcher.searchForRecordByID(record[1]);

                    Map expectedNsrAsMap = mapper.readValue(line, HashMap.class);
                    assertEquals(expectedNsrAsMap.get("id"), actualNsr.getId());
                    assertEquals(expectedNsrAsMap.get("lsid"), actualNsr.getLsid());
                    assertEquals(Integer.parseInt((String)expectedNsrAsMap.get("right"))-Integer.parseInt((String)expectedNsrAsMap.get("left")),
                        Integer.parseInt(actualNsr.getRight())-Integer.parseInt(actualNsr.getLeft()));
                    assertEquals(expectedNsrAsMap.get("matchType"), actualNsr.getMatchType().name());
                    assertEquals(expectedNsrAsMap.get("acceptedLsid"), actualNsr.getAcceptedLsid());
                    assertEquals(expectedNsrAsMap.get("rank"), actualNsr.getRank().name());
                    assertEquals(expectedNsrAsMap.get("nomenclaturalStatus"), actualNsr.getNomenclaturalStatus());
                    assertEquals(expectedNsrAsMap.get("establishmentMeans"), actualNsr.getEstablishmentMeans());
                    assertEquals(((Map) expectedNsrAsMap.get("rankClassification")).get("scientificName"), actualNsr.getRankClassification().getScientificName());
            }
        }
        catch (Exception e){
            e.printStackTrace();
            fail();
        }
        finally{
        }

    }


    @Test
    public void testMultipleMisappliedResolution3() throws Exception {

        String nameID = "NBNSYS0000167460";
        String taxonID = "NBNSYS0000000027";
        NameSearchResult nsr = searcher.searchForRecordByID(nameID); //NBNSYS0000000027");
       // NameSearchResult nsr = searcher.searchForRecordByLsid(taxonID);
        assertEquals(taxonID, nsr.getId());
        assertEquals(taxonID, nsr.getLsid());
        assertEquals(MatchType.TAXON_ID, nsr.getMatchType());
        System.out.println(nsr.getRankClassification().getScientificName());
        assertEquals("Vanellus vanellus", nsr.getRankClassification().getScientificName());


        NameSearchResult nsr2 = searcher.searchForRecordByLsid(nameID);
        assertEquals(taxonID, nsr.getId());
        assertEquals(taxonID, nsr.getLsid());
        assertEquals(MatchType.TAXON_ID, nsr.getMatchType());
        assertEquals("Vanellus vanellus", nsr.getRankClassification().getScientificName());


        String lsid = getCommonNameLSID("Green Plover");
        String sciName = getCommonName("Green Plover");
        assertEquals(taxonID, lsid);
        assertEquals("Vanellus vanellus", sciName);
    }

    @Test
    public void testIgnoredHomonyms1() {
        //test that Macropus throws an exception in normal situations
        try {
            searcher.searchForLSID("Macropus");
        } catch (SearchResultException e) {
            assertTrue(e instanceof HomonymException);
            assertEquals(1, e.getResults().size());
        }
    }

    @Test
    public void testIgnoredHomonyms2() {
        //test that Macropus doesn't throw and exception when "ignoreHomonyms" is set
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName("Macropus");
            cl.setGenus("Macropus");
            //NameSearchResult nsr =searcher.searchForRecord(cl.getId(), cl, null, true,true);
            String lsid = searcher.searchForLSID("Macropus", false, true);
            assertEquals("NBNSYS0000134841", lsid);
        } catch (Exception e) {
            fail("ignored homonyms should not throw exception " + e.getMessage());
        }
    }

    @Test
    public void testIgnoredHomonyms4() {
        //test that Agathis is resolvable with a kingdom
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName("Agathis");
            cl.setGenus("Agathis");
            cl.setKingdom("Animalia");
            NameSearchResult nsr = searcher.searchForRecord(cl.getScientificName(), cl, null, true, true);
            assertEquals("NHMSYS0020929963", nsr.getLsid());
        } catch (Exception e) {
            fail("A kingdom was supplied and should be resolvable. " + e.getMessage());
        }
    }

    @Test
    public void testRecordSearchWithoutScientificName() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification(null, null, null, "Hemiptera", "Pentatomidae", null, null);
            System.out.println(searcher.searchForRecord(cl, true));
            System.out.println("Lilianae::: " + searcher.searchForRecord("Lilianae", null));
            System.out.println("Leptospermum: " + searcher.searchForRecord("Leptospermum", null));
            //searcher.searchForLSID("Pulex (Pulex)");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSpeciesConstructFromClassification() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();

        } catch (Exception e) {

        }
    }

    @Test
    public void testsStrMarker1(){
        try {
            NameSearchResult nsr;
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setKingdom("Plantae");
            cl.setGenus("Test");
            cl.setScientificName("Macropus rufus");
            nsr = searcher.searchForRecord(cl, true);
            assertNotNull(nsr);
            assertEquals("NBNSYS0000134841", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }


    @Test
    public void testsStrMarker2(){
        try {
            NameSearchResult nsr;
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setKingdom("Plantae");
            cl.setGenus("Test");
            cl.setScientificName("Osphranter rufus");
            nsr = searcher.searchForRecord(cl, true);
            assertNotNull(nsr);
            assertEquals("NBNSYS0100004757", nsr.getLsid());
        } catch (SearchResultException ex) {
            fail("Not expecting exception " + ex);
        }
    }



    @Test
    public void catchAllSpeciesTest() {

        String name = "sp";
        try {
            String lsid = searcher.searchForLSID(name);
            fail("A rank marker should not match to a name");
        } catch (Exception e) {
            assertEquals("Supplied scientific name is a rank marker.", e.getMessage());
        }
    }

    @Test
    /**
     * Test that the spp. does not match.
     */
    public void testGenusNotAllSpecies() {
        try {
            //System.out.println(searcher.searchForLSID("Stackhousia sp. (McIvor River J.R.Clarkson 5201)"));
            String lsid = searcher.searchForLSID("Opuntia spp.");
            fail("Genus spp. test failed to throw exception.");
        } catch (Exception e) {
            assertEquals("Genus spp. test failed", "Unable to perform search. Can not match to a subset of species within a genus.", e.getMessage());
        }
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification(null, "Opuntia");
            cl.setScientificName("Opuntia spp.");
            searcher.searchForLSID(cl, true);
            fail("SPP2 failed to throw a SPP exception");
        } catch (Exception e) {
            assertEquals(SPPException.class, e.getClass());
        }
    }

    @Test
    public void testSearchForRecord() {
        NameSearchResult result = null;
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification(null, "Rhinotia");
            result = searcher.searchForRecord("Rhinotia", cl, RankType.GENUS);
        } catch (SearchResultException e) {
            e.printStackTrace();
            fail("testSearchForRecord failed");
        }
        System.out.println("testSearchForRecord: " + result);
    }

    @Test
    public void testSynonymWithoutRank() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setKingdom("Animalia");
            cl.setScientificName("Gymnorhina tibicen");
            NameSearchResult nsr = searcher.searchForRecord(cl, true, true);
            assertEquals("Gymnorhina tibicen", nsr.getRankClassification().getScientificName());
            assertEquals("(Latham, 1801)", nsr.getRankClassification().getAuthorship());
            nsr = searcher.searchForRecord("Vanellus vanellus", RankType.SPECIES);
            assertEquals("Vanellus vanellus", nsr.getRankClassification().getScientificName());
        } catch (Exception e) {
            fail("Exception");
            e.printStackTrace();
        }
    }

    @Test
    public void testBiocacheName() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName("Darwinia acerosa?");
            cl.setKingdom("Plantae");
            NameSearchResult nsr = searcher.searchForRecord(cl, true, true);
            System.out.println(nsr);
        } catch (Exception e) {
            fail("Exception");
            e.printStackTrace();
        }
    }

    @Test
    public void testRankMarker() {
        try {
            String lsid = searcher.searchForLSID("Macropus sp. rufus");
            System.out.println("SP.:" + lsid);
            lsid = searcher.searchForLSID("Macropus ssp. rufus");
            System.out.println("ssp: " + lsid);
        } catch (Exception e) {
            fail("rank marker test failed");
        }
    }

    @Test
    public void testCultivars() {
        try {
            //species level concept
            System.out.println("Hypoestes phyllostachya: " + searcher.searchForLSID("Hypoestes phyllostachya"));
            //cultivar level concept
            System.out.println("Hypoestes phyllostachya 'Splash': " + searcher.searchForRecord("Hypoestes phyllostachya 'Splash'", null));

        } catch (Exception e) {
            e.printStackTrace();
            fail("testCultivars failed");
        }
    }

    @Test
    public void testHomonym() {
        try {

            LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Simsia");
            List<NameSearchResult> results = searcher.searchForRecords(
                    "Simsia", RankType.getForId(6000), cl, 10);
            printAllResults("hymonyms test 1", results);
            //test to ensure that kingdoms that almost match are being will not report homonym exceptions
            cl.setGenus("Silene");
            cl.setKingdom("Plantae");
            results = searcher.searchForRecords("Silene", RankType.getForId(6000), cl, 10);
            printAllResults("hymonyms test (Silene)", results);

            cl.setGenus("Serpula");
            cl.setKingdom("Animalia");
            cl.setPhylum("ANNELIDA");
            results = searcher.searchForRecords("Serpula", RankType.getForId(6000), cl, 10);
            printAllResults("hymonyms test (Serpula)", results);

            cl.setGenus("Gaillardia");
            cl.setKingdom("Plantae");
            results = searcher.searchForRecords("Gaillardia", RankType.getForId(6000), cl, 10);
            printAllResults("hymonyms test (Gaillardia)", results);

        } catch (SearchResultException e) {
            //			System.err.println(e.getMessage());
            e.printStackTrace();
            printAllResults("HOMONYM EXCEPTION", e.getResults());
            fail("testHomonym failed");
        }
    }

    @Test
    public void testMyrmecia() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification("Animalia", "Arthropoda", "Insecta", "Hymenoptera", "Formicidae", "Myrmecia", null);
            String output = null;
            NameSearchResult nsr = searcher.searchForRecord("Myrmecia", cl, RankType.GENUS);
            if (nsr != null) {
                output = nsr.toString();
            }
            System.out.println("testMyrmecia: " + output);
        } catch (Exception e) {
            e.printStackTrace();
            fail("testMyrmecia failed");
        }
    }

    @Test
    public void testCrossRankHomonyms() {
        try {
            //Patellina is an order and genus
            searcher.searchForLSID("Patellina");
            fail("Cross Homonym Patellina test 1 failed");
        } catch (SearchResultException e) {
            System.out.println(e.getResults());
            assertEquals("Cross Homonysm Patellina test 1 failed to throw correct exception", e.getClass(), HomonymException.class);
        }
    }


    @Test
    public void testAuthorsProvidedInName() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName("Acanthastrea bowerbanki Edwards & Haime, 1857");
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        assertEquals(NameType.SCIENTIFIC, metrics.getNameType());
    }

    @Test
    public void testAffCfSpecies2() throws Exception {
        LinnaeanRankClassification cl = new LinnaeanRankClassification();
        cl.setScientificName("Acacia aff.");
        MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
        //aff. species need to match to the genus
        assertTrue(metrics.getErrors().contains(ErrorType.AFFINITY_SPECIES));
    }

    @Test
    public void testAlternatePhraseName() {
        try {
            LinnaeanRankClassification cl = new LinnaeanRankClassification();
            cl.setScientificName("Senna form taxon 'petiolaris'");
            MetricsResultDTO metrics = searcher.searchForRecordMetrics(cl, true);
            System.out.println(metrics);
        } catch (Exception e) {

        }
    }

    private String getCommonNameLSID(String name) {
        return searcher.searchForLSIDCommonName(name);
    }

    private String getCommonName(String name) {
        NameSearchResult sciName = searcher.searchForCommonName(name);

        return (sciName == null ? null : sciName.getRankClassification().getScientificName());
    }

    private void printAllResults(String prefix, List<NameSearchResult> results) {
        System.out.println("## " + prefix + " ##");
        if (results != null && results.size() != 0) {
            for (NameSearchResult result : results)
                System.out.println(result);
        }
        System.out.println("###################################");
    }


    /**
     * ***This is not part of the test. It was used to dump the expected index to a csv file. Note: works with lucene 6.6.6
     */
//    public static void main(String[] args) throws Exception {
//        saveIndexSearchResultsToFile(new File("/data/lucene/uksi-namatching-index-test/derived-test-data-from-EXPECTED-INDEX-2021-12-13.txt"));
//    }

    public static void saveIndexSearchResultsToFile(File file) throws IOException{
        ALANameSearcher nameSearcher = new ALANameSearcher("/data/lucene/uksi-namatching-index-test/namematching-EXPECTED-INDEX-2021-12-13");
        FileWriter fileWriter = null;
        PrintWriter writer = null;
        try {
            fileWriter = new FileWriter(file);
            writer = new PrintWriter(fileWriter);

            ObjectMapper mapper = new ObjectMapper();
            CSVReader reader = new CSVReaderBuilder(new FileReader("/data/lucene/sources/UKSI_DwCA_TEST_INDEX_2021-12-13/vernacular.csv")).withSkipLines(1).build();
            int count = 0;
            for (String[] record = reader.readNext(); record != null && count < 1000000; record = reader.readNext(), count++) {
                NameSearchResult nsr = nameSearcher.searchForRecordByID(record[1]);
                writer.println(mapper.writeValueAsString(nsr));
            }

        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally{
            if (fileWriter !=null) {
                fileWriter.close();
            }

            if (writer !=null) {
                writer.close();
            }
        }
    }
}
