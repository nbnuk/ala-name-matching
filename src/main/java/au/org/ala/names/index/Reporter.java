package au.org.ala.names.index;

import java.util.List;

/**
 * Report errors and issues in the taxonomy.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public interface Reporter {
    /**
     * Add a report.
     * <p>
     * Message codes are retrieved using a message bundle pointing to <code>taxonomy.properties</code>
     * Arguments are arbitrary strings and are not given specific meanings as in {@link #report(IssueType, String, TaxonomicElement, List<TaxonomicElement>)}.
     * </p>
     * <ul>
     *     <li>{0} The taxonID of the source element, either a name or a proper taxonID</li>
     *     <li>{1} Any attached scientific namer</li>
     *     <li>{2} Any attached authorship</li>
     *     <li>{3+} Any additional arguments</li>
     * </ul>
     *
     * @param type The type of report
     * @param code The message code to use for the readable version of the report
     * @param taxonID A specific taxonomic ID
     * @param scientificName A specific scientific name
     * @param scientificNameAuthorship A specific authorship
     * @param args Additional arguments for the report message
     */
    void report(IssueType type, String code, String taxonID, String scientificName, String scientificNameAuthorship, String... args);

    /**
     * Add a report.
     * <p>
     * Message codes are retrieved using a message bundle pointing to <code>taxonomy.properties</code>
     * These are formatted with a message formatter and have the following arguments:
     * </p>
     * <ul>
     *     <li>{0} The taxonID of the first element, either a name or a proper taxonID</li>
     *     <li>{1} The scientific name of the first element</li>
     *     <li>{2} Any authorship of the first element</li>
     *     <li>{3} Any associated taxon ids</li>
     *     <li>{4} The first taxon element</li>
     *     <li>{5+} Any associated taxon elements</li>
     * </ul>
     *
     * @param type The type of report
     * @param code The message code to use for the readable version of the report
     * @param main The main element
     * @param associated The elements that impact the report.
     */
    void report(IssueType type, String code, TaxonomicElement main, List<? extends TaxonomicElement> associated);
}
