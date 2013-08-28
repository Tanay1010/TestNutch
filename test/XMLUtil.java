package com.shopstyle.nutchx;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;



public class XMLUtil
{
    private static String rootElementName;
    static Document doc = null;
    public XMLUtil(){

    }

    public void constructDocumentWithRootElement(String rootElement){
        rootElementName = rootElement;
        buildDocumentWithRootElement();
    }

    public static Document parseXMLDocument(String xmlFile)
    {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder;
        Document xmldoc = null;
        try {
            builder = factory.newDocumentBuilder();
            xmldoc = builder.parse(xmlFile);
        }
        catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return xmldoc;
    }

    public static Document buildDocumentWithRootElement(){

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder;
            docBuilder = docFactory.newDocumentBuilder();
            doc =  docBuilder.newDocument();
            Element rootElement = doc.createElement(rootElementName);
            doc.appendChild(rootElement);
        }
        catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return doc;
    }

    public void addElementToParent(Element parent, Element child){

        if(parent != null && child != null){
            parent.appendChild(child);
        }

    }

    public void addTextElementToParent(Element parent, String childName, String childValue){


        if(childName != null && childValue != null){
            Element c = doc.createElement(childName);
            c.appendChild(doc.createTextNode(childValue));
            addElementToParent(parent,  c);
        }

    }

    private Element addTextElementToParent(String childName, String childValue){


        if(childName != null && childValue != null){
            Element c = doc.createElement(childName);
            c.appendChild(doc.createTextNode(childValue));
            return c;
        }
        return null;

    }

    public void addMultiElement(Element parent, String child, String name, List<String> values)
    {

        if(child != null){
            Element childElement = getElementByName(child);
            if(childElement == null){
                childElement = doc.createElement(child);
            }
            for (String value : values) {
                addTextElementToParent(childElement,name, value);
            }
            addElementToParent(parent,childElement);
        }


    }

    public List addElements4values(String parent, List<String> values)
    {
        List<Element> elements = new ArrayList();
        if(parent != null && values.size() > 0){
            for (String value : values) {
                if(value != null && !"".equals(value.trim())){
                    elements.add(addTextElementToParent(parent, value));
                }
            }
        }
        return elements;
    }

    public void addGroupElements(Element grandParent, String parent, List<List> values)
    {

        if(parent != null && values.size() > 0){

            int maxSizeValue = 0;
            for (int i=0; i<values.size(); i++) {
                if(values.get(i) != null && values.get(i).size() > maxSizeValue){
                    maxSizeValue = values.get(i).size();
                }
            }
            for (int i=0; i<maxSizeValue;i++) {
                Element p = doc.createElement(parent);
                for(int j = 0; j<values.size(); j++){
                    if(values.get(j) != null && values.get(j).size() > i){
                        Element temp =  (Element)values.get(j).get(i);
                        p.appendChild(temp);
                    }

                    if(grandParent != null) {
                        grandParent.appendChild(p);
                    }
                }
            }
        }

    }

    private Element getElementByName(String element){
        NodeList list = getRootElement().getElementsByTagName(element);
        if(list != null){
            return (Element)list.item(0);
        }
        return null;
    }

    public Element addElementToParent(Element parent, String child){

        if(child != null){
            Element c = doc.createElement(child);
            addElementToParent(parent,  c);
            return c;
        }
        return null;

    }

    public void addElementToParent(String parent, String child){

        if(child != null){
            Element p = getElementByName(parent);
            if(p == null){
                p = doc.createElement(parent);
            }
            Element c = doc.createElement(child);
            addElementToParent(p,  c);
        }

    }

    public Element getRootElement(){
        return doc.getDocumentElement();
    }

    public Document getDocument(){
        return doc;
    }

    public void writeToFile(String dirName, String fileName){
        try{
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(dirName+"/"+fileName));

            // Output to console for testing
            // StreamResult result = new StreamResult(System.out);

            transformer.transform(source, result);

            //System.out.println("File saved!");

      } catch (TransformerException tfe) {
            tfe.printStackTrace();
      }
    }

    public Document parseXmlFile(String fileName){
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = docFactory.newDocumentBuilder();
            doc = docBuilder.parse(new File(fileName));
            return doc;
        }
        catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
