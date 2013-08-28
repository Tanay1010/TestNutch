package com.shopstyle.nutchx.feed;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.shopstyle.nutchx.FeedParser.RetailerFeed;
import com.shopstyle.util.ConfigProperties;

public class XMLFeed implements RetailerFeed
{
    private final NodeList xmlNodes;
    private final Map attributeMapping;
    private int currentIndex;

    public XMLFeed(String fileLocation, String productTagName, String attributeMappingKey)
        throws IOException, SAXException,
        ParserConfigurationException
    {
        File fXmlFile = new File(fileLocation);
        
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);
      
        doc.getDocumentElement().normalize();
        xmlNodes = doc.getElementsByTagName(productTagName);
   
        currentIndex = 0;

        attributeMapping = ConfigProperties.getInstance().getMap(
            "com.shopstyle.nutchx.feed.XMLFeed.".concat(attributeMappingKey));

    }

    @Override
    public RetailerProduct nextProduct()
    {
        if (currentIndex < xmlNodes.getLength()) {
            XMLProduct product = new XMLProduct(xmlNodes.item(currentIndex), attributeMapping);
//            System.out.println(product);
//            System.exit(0);
            currentIndex++;
            return product;
        }
        else {
            return null;
        }
    }
}
