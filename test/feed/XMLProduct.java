package com.shopstyle.nutchx.feed;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.apache.nutch.util.StringUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.shopstyle.nutchx.NutchConstants;
import com.shopstyle.util.XPathUtil;

public class XMLProduct extends RetailerProduct
{
    private Node product;
    private final Map attributeMapping;

    private static Logger log = Logger.getLogger(XMLProduct.class);

    public XMLProduct(Node product, Map attributeMapping)
    {
        try {
            // DCA - decouple node from XML Document
            String blockText = XPathUtil.convertNodeToText(product);
            this.product = XPathUtil.convertStringToXMLNode(blockText);
        }
        catch (Exception e) {
            log.warn("Could not create XML Product", e);
        }

        this.attributeMapping = attributeMapping;
        Map stripFromValueMap = (Map) this.attributeMapping.get("stripFromValue");
        this.setStripFromValueMap(stripFromValueMap);
        Map addToValueMap = (Map) this.attributeMapping.get("addToValue");
        this.setAddToValueMap(addToValueMap);
    }

    @Override
    protected String getAttributeFromFeed(String attribute)
    {
        String result = null;
        Object attributeMapValue = attributeMapping.get(attribute);
        String combiner = (String) attributeMapping.get(attribute.concat("Combiner"));
        if (StringUtil.isEmpty(combiner)) {
            combiner = ";";
        }
        if (attributeMapValue instanceof List) {
            Iterator xPathList = ((List) attributeMapValue).iterator();
            while (xPathList.hasNext()) {
                String xPathString = (String) xPathList.next();
                result = result.concat(combiner).concat(evaluateXPath(xPathString, combiner));
            }
        }
        else {
            String xPathString = (String) attributeMapValue;
            if (NutchConstants.retailer.equals(attribute)) {
                return xPathString;
            }
            result = evaluateXPath(xPathString, combiner);
        }
        return result;
    }

    private String evaluateXPath(String xPathString, String combiner)
    {
        String result = null;
        if (!StringUtil.isEmpty(xPathString)) {
            try {
                NodeList nodes = (NodeList) XPathUtil.evaluateXPath(xPathString, product,
                    XPathConstants.NODESET);
                int listLength = nodes.getLength();
                if (listLength == 1) {
                    Node node = nodes.item(0);
                    result = node.getTextContent();
                }
                else {
                    for (int i = 0; i < listLength; i++) {
                        Node node = nodes.item(i);
                        if (null == result) {
                            result = node.getTextContent();
                        }
                        else {
                            result = result.concat(combiner).concat(node.getTextContent());
                        }
                    }
                }
            }
            catch (XPathExpressionException e) {
                log.warn("Could not evaluate expression, expression was " + xPathString, e);
            }
        }
        return result;
    }

    @Override
    public boolean addProduct()
    {
        return true;
    }
}
