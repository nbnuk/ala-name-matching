/*
 * Copyright (C) 2014 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */
package au.org.ala.names.search;

import au.org.ala.names.lucene.analyzer.LowerCaseKeywordAnalyzer;
import au.org.ala.names.model.LinnaeanRankClassification;
import au.org.ala.names.model.MatchMetrics;
import au.org.ala.names.model.VernacularType;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field.Store;

import java.io.File;
import java.util.Collection;

/**
 * Creates the Lucene index based on the names that are exported from
 * http://code.google.com/p/ala-portal/source/browse/trunk/ala-names-generator/src/main/resources/create-dumps.sql
 * @Deprecated
 * @author Natasha
 */
public class UksiNameIndexer extends ALANameIndexer{


    public void addAdditionalName(String lsid, String scientificName, String author, LinnaeanRankClassification cl, String nomenclaturalStatus) throws Exception {

        if (cbIndexWriter == null)
            cbIndexWriter = createIndexWriter(new File(indexDirectory + File.separator + "cb"), LowerCaseKeywordAnalyzer.newInstance(), false);

        Document doc = createALAIndexDocument(scientificName, "-1", lsid, author, cl, new UksiIndexFields(nomenclaturalStatus,null,null));
        cbIndexWriter.addDocument(doc);

    }



    protected Document createCommonNameDocument(String cn, String sn, String lsid, String language, boolean checkAccepted, String priorityStatus, String commonNameID) {
        Document doc = createCommonNameDocument(cn, sn, lsid, language, checkAccepted);

        if (priorityStatus != null && priorityStatus != "") {
            NameIndexField.PRIORITY_STATUS.store(priorityStatus, doc);

            VernacularType type = VernacularType.forTerm(priorityStatus, VernacularType.COMMON);
            Integer priority = type.getPriority();
            doc.add(new NumericDocValuesField(NameIndexField.PRIORITY.toString(), priority));
            NameIndexField.PRIORITY.store(priority, doc);
        }

        if(commonNameID != null) {
            NameIndexField.ID.store(commonNameID, doc);
        }

        return doc;
    }


    public Document createALAIndexDocument(String name, String id, String lsid, String author, LinnaeanRankClassification cl,  UksiIndexFields uksiIndexFields   ){
        return createALAIndexDocument(name,id, lsid, author,null,null, 0, 0, cl, null, null, MatchMetrics.DEFAULT_PRIORITY, uksiIndexFields);
    }


    public Document createALAIndexDocument(String name, String id, String lsid, String author, String rank, String rankId, int left, int right, LinnaeanRankClassification cl, String nameComplete, Collection<String> otherNames, int priority, UksiIndexFields uksiIndexFields){
       String nc = buildNameComplete(name, author, nameComplete, uksiIndexFields.nomenclaturalStatus);

        Document doc = super.createALAIndexDocument(name, id, lsid, author, rank, rankId, left, right, cl, nc, otherNames, priority);
        addUksiFieldsToALAIndexDocument(doc, uksiIndexFields);
        return doc;


    }



    protected Document createALAIndexDocument(String name, String id, String lsid, String rank, String rankString,
                                              String kingdom, String kid, String phylum, String pid, String clazz, String cid, String order,
                                              String oid, String family, String fid, String genus, String gid,
                                              String species, String sid, int left, int right, String acceptedConcept, String specificEpithet,
                                              String infraspecificEpithet, String author, String nameComplete, Collection<String> otherNames,
                                              int priority, UksiIndexFields uksiIndexFields) {
        String nc = buildNameComplete(name, author, nameComplete, uksiIndexFields.nomenclaturalStatus);

        Document doc = createALAIndexDocument(name, id, lsid, rank, rankString,
                kingdom, kid, phylum, pid, clazz, cid, order,
                oid, family, fid, genus, gid,
                species, sid, left, right, acceptedConcept, specificEpithet,
                infraspecificEpithet, author, nameComplete, otherNames,
        priority);

        addUksiFieldsToALAIndexDocument(doc, uksiIndexFields);
        return doc;

    }


    protected Document createALASynonymDocument(String scientificName, String author, String nameComplete, Collection<String> otherNames, String id, String lsid, String nameLsid, String acceptedLsid, String acceptedId, int priority, String synonymType, String nomenclaturalStatus) {
       Document doc = createALASynonymDocument(scientificName, author, nameComplete, otherNames, id, lsid, nameLsid, acceptedLsid, acceptedId, priority, synonymType);
        //this is all that was actually added
       if (StringUtils.trimToNull(nomenclaturalStatus) != null) {
           NameIndexField.NOMENCLATURAL_STATUS.store(nomenclaturalStatus, doc);
        }
        return doc;
    }



    protected String buildNameComplete(String name, String author, String nameComplete, String nomenclaturalStatus) {
        if (StringUtils.isNotBlank(nameComplete))
            return nameComplete;
        StringBuilder ncb = new StringBuilder(64);
        if (name != null)
            ncb.append(name);
        ncb.append(" ");
        if (author != null)
            ncb.append(author);
        ncb.append(" ");
        if (nomenclaturalStatus != null)
            ncb.append(nomenclaturalStatus);
        return ncb.toString().trim();
    }

    protected class UksiIndexFields{
        String nomenclaturalStatus;
        String establishmentMeans;
        String habitat;

        UksiIndexFields(Document doc){
            this(
                    doc.get(NameIndexField.NOMENCLATURAL_STATUS.toString()), /*RR ??*/
                    //do we actually want to put the nomenclaturalStatus in its own field, or simply use it to build the nameComplete?
                    doc.get(NameIndexField.ESTABLISHMENT_MEANS.toString()),
                    doc.get(NameIndexField.HABITAT.toString())
            );
        }

        UksiIndexFields(String nomenclaturalStatus, String establishmentMeans, String habitat){
            this.nomenclaturalStatus = nomenclaturalStatus;
            //do we actually want to put the nomenclaturalStatus in its own field, or simply use it to build the nameComplete?
            this.establishmentMeans = establishmentMeans;
            this.habitat = habitat;
        }

        public String toString(){
            return String.format("nomenclaturalStatus:%s establishmentMeans:%s habitat:%s",nomenclaturalStatus,establishmentMeans,habitat);
        }
    }

    private void addUksiFieldsToALAIndexDocument(Document doc, UksiIndexFields uksiIndexFields){
        if (StringUtils.trimToNull(uksiIndexFields.nomenclaturalStatus) != null) {
            NameIndexField.NOMENCLATURAL_STATUS.store(uksiIndexFields.nomenclaturalStatus, doc);
        }

        if (StringUtils.trimToNull(uksiIndexFields.establishmentMeans) != null) {
            NameIndexField.ESTABLISHMENT_MEANS.store(uksiIndexFields.establishmentMeans, doc);
        }

        if (StringUtils.trimToNull(uksiIndexFields.habitat) != null) {
            NameIndexField.HABITAT.store(uksiIndexFields.habitat, doc);
        }
    }

}
