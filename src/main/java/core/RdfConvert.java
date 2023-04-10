/*
 * Licensed under the EUPL-1.2-or-later.
 * Copyright (c) 2020, gridDigIt Kft. All rights reserved.
 * @author Chavdar Ivanov
 */
package core;

import application.MainController;
import javafx.stage.FileChooser;
import org.apache.commons.io.FileUtils;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.vocabulary.*;

import java.io.*;
import java.util.*;


public class RdfConvert {

    public static Model modelInheritance;

    //RDF conversion
    public static void rdfConversion(File file, List<File> files, String sourceFormat, String targetFormat, String xmlBase,RDFFormat rdfFormat,
                                     String showXmlDeclaration, String showDoctypeDeclaration, String tab,String relativeURIs, Boolean modelUnionFlag,
                                     Boolean inheritanceOnly,Boolean inheritanceList, Boolean inheritanceListConcrete, Boolean addowl, Boolean modelUnionFlagDetailed) throws IOException {

        Lang rdfSourceFormat;
        switch (sourceFormat) {
            case "RDF XML (.rdf or .xml)":
                rdfSourceFormat=Lang.RDFXML;
                break;

            case "RDF Turtle (.ttl)":
                rdfSourceFormat=Lang.TURTLE;
                break;

            case "JSON-LD (.jsonld)":
                rdfSourceFormat=Lang.JSONLD;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + sourceFormat);
        }
        List<File> modelFiles = new LinkedList<File>();
        if (!modelUnionFlagDetailed) {
            if (!modelUnionFlag && file != null) {
                modelFiles.add(file);
            } else {
                modelFiles = files;
            }
        }

        Model model;

        if (modelUnionFlagDetailed) {
            //put first the main RDF
//            FileChooser filechooser = new FileChooser();
//            filechooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Main RDF file", "*.rdf","*.xml", "*.ttl"));
//            filechooser.setInitialDirectory(new File(MainController.prefs.get("LastWorkingFolder","")));
//            File fileDet1=null;
            List<File> fileDet1 = util.ModelFactory.filechoosercustom(true,"Main RDF file", List.of("*.rdf","*.xml", "*.ttl"));
//            try {
//                fileDet1 = filechooser.showOpenDialog(null);
//            }catch (Exception e){
//                filechooser.setInitialDirectory(new File("C:/"));
//                fileDet1 = filechooser.showOpenDialog(null);
//            }
            if (fileDet1.get(0)!=null) {
                modelFiles.add(fileDet1.get(0));
            }


//            FileChooser filechooser1 = new FileChooser();
//            filechooser1.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Deviation RDF file", "*.rdf","*.xml", "*.ttl"));
//            filechooser1.setInitialDirectory(new File(MainController.prefs.get("LastWorkingFolder","")));
//            File fileDet2=null;
            List<File> fileDet2 = util.ModelFactory.filechoosercustom(true,"Deviation RDF file", List.of("*.rdf","*.xml", "*.ttl"));
//            try {
//                fileDet2 = filechooser1.showOpenDialog(null);
//            }catch (Exception e){
//                filechooser1.setInitialDirectory(new File("C:/"));
//                fileDet2 = filechooser1.showOpenDialog(null);
//            }
            if (fileDet2.get(0)!=null) {
                modelFiles.add(fileDet2.get(0));
            }


//            FileChooser filechooser2 = new FileChooser();
//            filechooser2.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Extended RDF file", "*.rdf","*.xml", "*.ttl"));
//            filechooser2.setInitialDirectory(new File(MainController.prefs.get("LastWorkingFolder","")));
//            File fileDet3=null;
            List<File> fileDet3 = util.ModelFactory.filechoosercustom(true,"Extended RDF file", List.of("*.rdf","*.xml", "*.ttl"));
//            try {
//                fileDet3 = filechooser2.showOpenDialog(null);
//            }catch (Exception e){
//                filechooser2.setInitialDirectory(new File("C:/"));
//                fileDet3 = filechooser2.showOpenDialog(null);
//            }
            if (fileDet3.get(0)!=null) {
                modelFiles.add(fileDet3.get(0));
            }

            model = org.apache.jena.rdf.model.ModelFactory.createDefaultModel();
            Model modelOrig = org.apache.jena.rdf.model.ModelFactory.createDefaultModel();
            Map<String, String> prefixMap = model.getNsPrefixMap();
            int count=1;
            for (File modelFile : modelFiles) {
                Model modelPart = ModelFactory.createDefaultModel();
                InputStream inputStream = new FileInputStream(modelFile.toString());
                RDFDataMgr.read(modelPart, inputStream, xmlBase, rdfSourceFormat);
                prefixMap.putAll(modelPart.getNsPrefixMap());
                model.add(modelPart);
                if (count==1){
                    modelOrig=modelPart;
                }
                count=count+1;
            }
            model.setNsPrefixes(prefixMap);

            List<Statement> stmtToDeleteClass = new LinkedList<>();
            for (StmtIterator i = model.listStatements(new SimpleSelector(null, ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#","belongsToCategory"),(RDFNode) null)); i.hasNext(); ) {
                Statement stmt = i.next();
                if (stmt.getObject().asResource().getLocalName().equals("Package_LTDSnotDefined")){
                    //delete all classes
                    List<Statement> stdelete=model.listStatements(new SimpleSelector(stmt.getSubject(), null, (RDFNode) null)).toList();
                    stmtToDeleteClass.addAll(stdelete);
                    //delete all attributes and associations with domain of the deleted classes
                    for (Statement stmpProp : stdelete) {
                        List<Statement> stdeleteProp = model.listStatements(new SimpleSelector(null, RDFS.domain, stmpProp.getSubject())).toList();
                        stmtToDeleteClass.addAll(stdeleteProp);
                        for (Statement stmpProp1 : stdeleteProp) {
                            if (stmpProp1.getSubject().getLocalName().split("\\.", 2)[0].equals(stmpProp.getSubject().getLocalName())) {
                                List<Statement> stdeleteProp1 = model.listStatements(new SimpleSelector(stmpProp1.getSubject(), null, (RDFNode) null)).toList();
                                stmtToDeleteClass.addAll(stdeleteProp1);
                            }
                        }

                        //delete all attributes and associations with range of the deleted classes

                        List<Statement> stdeleteProp1 = model.listStatements(new SimpleSelector(null, RDFS.range, stmpProp.getSubject())).toList();
                        stmtToDeleteClass.addAll(stdeleteProp1);
                        for (Statement stmpProp2 : stdeleteProp1) {
                            List<Statement> stdeleteProp2 = model.listStatements(new SimpleSelector(stmpProp2.getSubject(), null, (RDFNode) null)).toList();
                            stmtToDeleteClass.addAll(stdeleteProp2);
                        }

                    }

                }
            }



            for (StmtIterator i = model.listStatements(new SimpleSelector(null, RDFS.label,ResourceFactory.createLangLiteral("LTDSnotDefined","en"))); i.hasNext(); ) {
                Statement stmt = i.next();
                List<Statement> stdelete=model.listStatements(new SimpleSelector(stmt.getSubject(), null, (RDFNode) null)).toList();
                stmtToDeleteClass.addAll(stdelete);
            }

            model.remove(stmtToDeleteClass);

            List<Statement> stmtToDeleteProperty = new LinkedList<>();
            for (StmtIterator i = model.listStatements(new SimpleSelector(null, ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#","stereotype"),(RDFNode) null)); i.hasNext(); ) {
                Statement stmt = i.next();
                if (stmt.getObject().toString().equals("LTDSnotDefined")){
                    List<Statement> stdelete=model.listStatements(new SimpleSelector(stmt.getSubject(), null, (RDFNode) null)).toList();
                    stmtToDeleteProperty.addAll(stdelete);
                }
            }

            //delete double multiplicity
            for (StmtIterator k = model.listStatements(new SimpleSelector(null, ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#","multiplicity"),(RDFNode) null)); k.hasNext(); ) {
                Statement stmt = k.next();
                List<Statement> multi = model.listStatements(new SimpleSelector(stmt.getSubject(), ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#","multiplicity"),(RDFNode) null)).toList();
                if (multi.size()>1){
                    for (Statement stmtM : multi) {
                        if (modelOrig.contains(stmtM)) {
                            stmtToDeleteProperty.add(stmtM);
                        }
                    }
                }
            }

            model.remove(stmtToDeleteProperty);

        }else{
            // load all models
            model = util.ModelFactory.modelLoad(modelFiles,xmlBase,rdfSourceFormat);
        }



        //in case only inheritance related structure should be converted
        if(inheritanceOnly){
            model = modelInheritance(model,inheritanceList,inheritanceListConcrete);
        }

        List<Statement> stmttoadd = new LinkedList<>();
        String rdfNs = "http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#";
        if (addowl){
            for (ResIterator i = model.listSubjectsWithProperty(RDF.type); i.hasNext(); ) {
                Resource resItem = i.next();
                //String className = resItem.getRequiredProperty(RDF.type).getObject().asResource().getLocalName();

                RDFNode obje=resItem.getRequiredProperty(RDF.type).getObject();
                if (obje.equals(RDFS.Class)){
                    stmttoadd.add(ResourceFactory.createStatement(resItem,RDF.type,OWL2.Class));
                }else if (obje.equals(RDF.Property)) {
                    stmttoadd.add(ResourceFactory.createStatement(resItem,RDF.type,OWL2.ObjectProperty));
                }

                for (NodeIterator k = model.listObjectsOfProperty(resItem, model.getProperty(rdfNs, "stereotype")); k.hasNext(); ) {
                    RDFNode resItemNodeDescr = k.next();
                    if (resItemNodeDescr.toString().equals("enum")){
                        stmttoadd.add(ResourceFactory.createStatement(resItem,RDF.type,OWL2.NamedIndividual));
                        break;
                    }
                    if (resItemNodeDescr.toString().equals("CIMDatatype")){
                        stmttoadd.add(ResourceFactory.createStatement(resItem,RDF.type,OWL2.DatatypeProperty));
                        break;
                    }
                    if (resItemNodeDescr.toString().equals("Primitive")){
                        stmttoadd.add(ResourceFactory.createStatement(resItem,RDF.type,OWL2.DatatypeProperty));
                        break;
                    }
                }
            }
        }

        model.add(stmttoadd);

        String filename="";
        if (!modelUnionFlag && file!=null) {
            filename=file.getName().split("\\.",2)[0];
        }else{
            filename="MultipleModels";
        }

        switch (targetFormat) {
            case "RDF XML (.rdf or .xml)":
                OutputStream outXML = fileSaveDialog("Save RDF XML for: "+filename, "RDF XML", "*.rdf");
                if (outXML!=null) {
                    try {
                        Map<String, Object> properties = new HashMap<>();
                        properties.put("showXmlDeclaration", showXmlDeclaration);
                        properties.put("showDoctypeDeclaration", showDoctypeDeclaration);
                        //properties.put("blockRules", RDFSyntax.propertyAttr.toString()); //???? not sure
                        properties.put("xmlbase", xmlBase);
                        properties.put("tab", tab);
                        //properties.put("prettyTypes",new Resource[] {ResourceFactory.createResource("http://iec.ch/TC57/61970-552/ModelDescription/1#FullModel")});
                        properties.put("relativeURIs", relativeURIs);


                        // Put a properties object into the Context.
                        Context cxt = new Context();
                        cxt.set(SysRIOT.sysRdfWriterProperties, properties);

                        org.apache.jena.riot.RDFWriter.create()
                                .base(xmlBase)
                                .format(rdfFormat)
                                .context(cxt)
                                .source(model)
                                .output(outXML);
                        if (inheritanceList) {
                            fileSaveDialogInheritance(filename+"Inheritance",xmlBase);
                        }
                    } finally {
                        outXML.close();
                    }
                }
                break;

            case "RDF Turtle (.ttl)":
                OutputStream outTTL = fileSaveDialog("Save RDF Turtle for: "+filename, "RDF Turtle", "*.ttl");
                if (outTTL!=null) {
                    try {
                        model.write(outTTL, RDFFormat.TURTLE.getLang().getLabel().toUpperCase(), xmlBase);
                        if (inheritanceList) {
                            fileSaveDialogInheritance(filename+"Inheritance",xmlBase);
                        }
                    } finally {
                        outTTL.close();
                    }
                }
                break;

            case "JSON-LD (.jsonld)":
                OutputStream outJsonLD = fileSaveDialog("Save JSON-LD for: "+filename, "JSON-LD", "*.jsonld");
                if (outJsonLD!=null) {
                    try {
                        model.write(outJsonLD, RDFFormat.JSONLD.getLang().getLabel().toUpperCase(), xmlBase);
                        if (inheritanceList) {
                            fileSaveDialogInheritance(filename+"Inheritance",xmlBase);
                        }
                    } finally {
                        outJsonLD.close();
                    }
                }
                break;
        }
    }

    //File save dialog for inheritance
    private static void fileSaveDialogInheritance(String filename, String xmlBase) throws IOException {
        OutputStream outInt = fileSaveDialog("Save inheritance for: "+filename, "RDF Turtle", "*.ttl");
        try {
            modelInheritance.write(outInt, RDFFormat.TURTLE.getLang().getLabel().toUpperCase(), xmlBase);
        } finally {
            outInt.close();
        }

    }

    //File save dialog
    private static OutputStream fileSaveDialog(String title, String extensionName, String extension) throws FileNotFoundException {
//        File saveFile;
//        FileChooser filechooserS = new FileChooser();
//        filechooserS.getExtensionFilters().addAll(new FileChooser.ExtensionFilter(extensionName, extension));
//        filechooserS.setInitialFileName(title.split(": ", 2)[1]);
//        filechooserS.setInitialDirectory(new File(MainController.prefs.get("LastWorkingFolder","")));
//        filechooserS.setTitle(title);
        File saveFile = util.ModelFactory.filesavecustom(extensionName, List.of(extension), title,title.split(": ", 2)[1]);
        try {
//            try {
//                saveFile = filechooserS.showSaveDialog(null);
//            } catch (Exception e) {
//                filechooserS.setInitialDirectory(new File(String.valueOf(FileUtils.getUserDirectory())));
//                saveFile = filechooserS.showSaveDialog(null);
//            }
            OutputStream out = null;
            if (saveFile != null) {
                //MainController.prefs.put("LastWorkingFolder", saveFile.getParent());
                out = new FileOutputStream(saveFile);
            }
            return out;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    //Creates another model that contains only inheritance related properties
    public static Model modelInheritance(Model model, Boolean inheritanceList, Boolean inheritanceListConcrete)  {
        Model modelProcessed = ModelFactory.createDefaultModel();
        modelProcessed.setNsPrefixes(model.getNsPrefixMap());
        modelInheritance = ModelFactory.createDefaultModel();
        modelInheritance.setNsPrefixes(model.getNsPrefixMap());
        modelInheritance.setNsPrefix("owl",OWL2.NS);

        for (StmtIterator i = model.listStatements(); i.hasNext(); ) {
            Statement stmt = i.next();
            if (!inheritanceList) {
                if (stmt.getPredicate().equals(RDF.type) || stmt.getPredicate().equals(RDFS.subClassOf) || stmt.getPredicate().equals(RDFS.subPropertyOf) ||
                        stmt.getPredicate().equals(RDFS.domain) || stmt.getPredicate().equals(RDFS.range)) {
                    modelProcessed.add(stmt);
                }
            }else {
                if (stmt.getPredicate().equals(RDF.type) || stmt.getPredicate().equals(RDFS.subClassOf) || stmt.getPredicate().equals(RDFS.subPropertyOf) ||
                        stmt.getPredicate().equals(RDFS.domain) || stmt.getPredicate().equals(RDFS.range)) {
                    modelProcessed.add(stmt);
                    if (stmt.getPredicate().equals(RDF.type)) {
                        Resource stmtSubject = stmt.getSubject();
                        modelInheritance=inheritanceStructure(stmtSubject, stmtSubject, modelInheritance, model, inheritanceListConcrete);

                    }


                }
            }
        }

        return modelProcessed;
    }

    // Adds the inheritance structure in the model
    private static Model inheritanceStructure(Resource stmtSubject, Resource res, Model modelInheritance, Model model, Boolean inheritanceListConcrete) {

            for (ResIterator j = model.listSubjectsWithProperty(RDFS.subClassOf,res); j.hasNext(); ) {
                Resource resSub = j.next();
                //check if the class is concrete

                if (inheritanceListConcrete) {
                    boolean addConcrete=false;
                    if (model.listObjectsOfProperty(resSub,ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#stereotype")).hasNext()) {
                        for (NodeIterator k = model.listObjectsOfProperty(resSub,ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#stereotype")); k.hasNext(); ) {
                            RDFNode objC = k.next();
                            if (objC.isResource()) {
                                if (objC.toString().equals("http://iec.ch/TC57/NonStandard/UML#concrete")) {
                                    addConcrete = true;
                                }
                            }
                        }
                    }//TODO add else if to support other ways to identify if the class is concrete
                    if (addConcrete) {
                        modelInheritance.add(stmtSubject, OWL2.members, resSub);
                        modelInheritance.add(stmtSubject, RDF.type, OWL2.Class);
                    }
                }else{
                    modelInheritance.add(stmtSubject, OWL2.members, resSub);
                    modelInheritance.add(stmtSubject, RDF.type, OWL2.Class);
                }
                modelInheritance=inheritanceStructure(stmtSubject, resSub, modelInheritance, model, inheritanceListConcrete);
            }


        return modelInheritance;
    }

    // convert CGMES xml to SKOS for the reference data
    public static void refDataConvert() throws IOException {

        // load the data
        // change the format
        // save it in xml


        String xmlBase = "http://iec.ch/TC57/CIM100";
        Map<String, RDFDatatype> dataTypeMap = new HashMap<>();


        //if the datatypes map is from RDFS - make union of RDFS and generate map
        if (MainController.IDmapList != null) {// the file is selected
            for (File item : MainController.IDmapList) {
                Properties properties = new Properties();
                properties.load(new FileInputStream(item.toString()));
                for (Object key : properties.keySet()) {
                    String value = properties.get(key).toString();
                    RDFDatatype valueRDFdatatype = DataTypeMaping.mapFromMapDefaultFile(value);
                    dataTypeMap.put(key.toString(), valueRDFdatatype);
                }
            }
        }
        // if model 1 is more that 1 zip or xml - merge

        Model model1single = null;

        Model model1 = org.apache.jena.rdf.model.ModelFactory.createDefaultModel();
        Map prefixMap = model1.getNsPrefixMap();

        for (File item : MainController.IDModel1) {
            if (item.getName().toLowerCase().endsWith(".zip")) {

                model1single = util.ModelFactory.unzip(item, dataTypeMap, xmlBase, 2);
            } else if (item.getName().toLowerCase().endsWith(".xml")) {
                InputStream inputStream = new FileInputStream(item);

                model1single = util.ModelFactory.modelLoadXMLmapping(inputStream, dataTypeMap, xmlBase);

            }
            prefixMap.putAll(model1single.getNsPrefixMap());
            model1.add(model1single);
        }
        model1.setNsPrefixes(prefixMap);

        Model modelResult = org.apache.jena.rdf.model.ModelFactory.createDefaultModel();
        modelResult.setNsPrefixes(prefixMap);
        modelResult.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
        modelResult.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
        modelResult.setNsPrefix("dc", "http://purl.org/dc/elements/1.1/");
        modelResult.setNsPrefix("dcterms", DCTerms.NS);


        Resource schemeRes = ResourceFactory.createResource("http://publications.europa.eu/resource/authority/baseVoltage");
        modelResult.add(schemeRes,RDF.type, SKOS.ConceptScheme  );//ResourceFactory.createProperty("http://www.w3.org/2004/02/skos/core#ConceptScheme")
        modelResult.add(schemeRes,OWL2.versionInfo, ResourceFactory.createPlainLiteral("22 Nov 2021") );
        modelResult.add(schemeRes,RDFS.label, ResourceFactory.createPlainLiteral("Base Voltage") );
        modelResult.add(schemeRes,SKOS.prefLabel, ResourceFactory.createPlainLiteral("Base Voltage") );


        for (StmtIterator i = model1.listStatements(new SimpleSelector((Resource) null , RDF.type, ResourceFactory.createProperty(xmlBase,"#BaseVoltage"))); i.hasNext(); ) {
            Statement stmtItem = i.next();

            //get the object of the BaseVoltage.nominalVoltage attribute
            String nominalVoltage= model1.listStatements(new SimpleSelector(stmtItem.getSubject() , ResourceFactory.createProperty(xmlBase,"#BaseVoltage.nominalVoltage"), (RDFNode) null)).next().getObject().asLiteral().getString();

            // Create the concept for that base voltage
            Resource resNewStmt=ResourceFactory.createResource("http://publications.europa.eu/resource/authority/baseVoltage"+"/"+nominalVoltage+"kV");
            modelResult.add(resNewStmt,RDF.type, SKOS.Concept  );
            modelResult.add(resNewStmt, ResourceFactory.createProperty(xmlBase,"#IdentifiedObject.mRID"), stmtItem.getSubject().asResource().getLocalName().split("_",2)[1]);
            modelResult.add(resNewStmt,SKOS.inScheme, schemeRes );


            for (StmtIterator k = model1.listStatements(new SimpleSelector(stmtItem.getSubject() , (Property) null, (RDFNode) null)); k.hasNext(); ) {
                Statement stmtItemForClass = k.next();
                switch (stmtItemForClass.getPredicate().asResource().getLocalName()) {
                    case "IdentifiedObject.name":
                        modelResult.add(resNewStmt, stmtItemForClass.getPredicate(), stmtItemForClass.getObject());
                        modelResult.add(resNewStmt, SKOS.prefLabel, stmtItemForClass.getObject());
                        modelResult.add(resNewStmt, DCTerms.identifier, stmtItemForClass.getObject());
                        break;
                    case "IdentifiedObject.shortName":
                    case "IdentifiedObject.description":
                    case "BaseVoltage.nominalVoltage":
                        modelResult.add(resNewStmt, stmtItemForClass.getPredicate(), stmtItemForClass.getObject());
                        break;
                }
            }
        }
        //do the export of modelResult
        OutputStream outInt = fileSaveDialog("Save ref data: ....", "RDF XML", "*.xml");
        //modelResult.write(outInt, RDFFormat..getLang().getLabel().toUpperCase(), xmlBase);
        Map<String, Object> properties = new HashMap<>();
        properties.put("showXmlDeclaration", "true");
        properties.put("showDoctypeDeclaration", "false");
        //properties.put("showXmlEncoding", showXmlEncoding); // works only with the custom format
        //properties.put("blockRules", "daml:collection,parseTypeLiteralPropertyElt,"
        //        +"parseTypeResourcePropertyElt,parseTypeCollectionPropertyElt"
        //        +"sectionReification,sectionListExpand,idAttr,propertyAttr"); //???? not sure
       //if (putHeaderOnTop) {
        //    properties.put("prettyTypes", new Resource[]{ResourceFactory.createResource(headerClassResource)});
       // }
        properties.put("xmlbase", xmlBase);
        properties.put("tab", "2");
        properties.put("relativeURIs", "true");




        // Put a properties object into the Context.
        Context cxt = new Context();
        cxt.set(SysRIOT.sysRdfWriterProperties, properties);


        org.apache.jena.riot.RDFWriter.create()
                .base(xmlBase)
                .format(RDFFormat.RDFXML_ABBREV)
                .context(cxt)
                .source(modelResult)
                .output(outInt);

    }

}
