package com.shopstyle.nutchx;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Logger;
import org.apache.nutch.parse.HTMLMetaTags;
import org.apache.nutch.parse.HtmlParseFilter;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.protocol.Content;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.shopstyle.util.ConfigProperties;
import com.shopstyle.util.XPathUtil;

public class FeedCrawler implements HtmlParseFilter
{
    private Configuration conf;

    private String retailerHandle = "";
    private String crawlType = "PriceOnlyCrawl";
    private final Map xPathExpressions = new HashMap();
    private final Map regExPatterns = new HashMap();

    private boolean skipXPath = false;
    private boolean skipRegEx = false;

    private static Logger log = Logger.getLogger(FeedCrawler.class);

    /**
     * Scan the HTML document of the product details page of the retailer.
     */
    @Override
    public ParseResult filter(Content content, ParseResult parseResult, HTMLMetaTags metaTags, DocumentFragment doc)
    {
        String url = null; // Y
        try {
            url = content.getUrl(); // Y
            String host = getHost(url); // Y

            if (!isProductPage(host, url)) { // Y
                return parseResult;
            }

            String text = getContentAsText(content); // Y
            Document cleanDoc = XPathUtil.convertStringToNode(text); // Y

            if (!isAvailable(host, text)) { // N
                return parseResult;
            }

            // Get list of keys that we want to parse
            List<String> keyList = ConfigProperties.getInstance().getList(
                "com.shopstyle.nutchx.crawler.FeedCrawler.".concat(crawlType)); // Y

            String xPathItemBlock = ConfigProperties.getInstance().getString("com.shopstyle.nutchx.crawler.FeedCrawler." + this.crawlType + ".XPathItemBlock.".concat(host)); // Y
            if (StringUtils.isNotBlank(xPathItemBlock)) {
                NodeList itemBlocks = (NodeList) XPathUtil.evaluateXPath(xPathItemBlock, cleanDoc, XPathConstants.NODESET); // Y

                int length = itemBlocks.getLength(); // Y
                // Case in which there is no uniform way to access the product attributes
                if(length==0)
                {
                    HashMap matchedValuesMap = getAllMatchedValues(host, keyList, text, cleanDoc);
                    matchedValuesMap.put(NutchConstants.productUrl, url);
                    processProducts(matchedValuesMap);
                }

                // Case in which there is a uniform way to access the product attributes
                for (int i = 0; i < length; i++) {
                    Node itemBlock = itemBlocks.item(i);
                    String blockText = XPathUtil.convertNodeToText(itemBlock);
                    Document document = XPathUtil.convertStringToNode(blockText);

                    HashMap matchedValuesMap = getAllMatchedValuesForItemBlock(host, keyList, blockText, document);
                    matchedValuesMap.put(NutchConstants.productUrl, url);

                    // Required for Review Only Crawls, since there are multiple reviews for one product
                    if (((List)matchedValuesMap.get(NutchConstants.productId)).isEmpty()) {
                        List keys = new LinkedList();
                        keys.add(NutchConstants.productId);

                        HashMap additionalMatchedValuesMap = getAllMatchedValues(host, keys, text, cleanDoc);

                        matchedValuesMap.put(NutchConstants.productId, additionalMatchedValuesMap.get(NutchConstants.productId));
                    }

                    processProducts(matchedValuesMap);
                }
            }
            else {
                HashMap matchedValuesMap = getAllMatchedValues(host, keyList, text, cleanDoc);

                matchedValuesMap.put(NutchConstants.productUrl, url);
                processProducts(matchedValuesMap);
            }
        }
        catch (Exception e) {
            log.warn("Error in filter for " +  url, e);
        }
        return parseResult;
    }

    /*private HashMap getMatchedValues(String host, List<String> keyList, String text, Object cleanHTML)
    {
        HashMap matchedValuesMap = new HashMap();
        for (String key : keyList) {
            String value = getValue(host, key, text, cleanHTML);
            if (value != null && value.length() > 0) {
                if (key.equals(NutchConstants.retailer)) {
                    setRetailerHandle(value);
                }
                matchedValuesMap.put(key, value);
            }
        }
        return matchedValuesMap;
    }*/

    private HashMap getAllMatchedValues(String host, List<String> keyList, String text, Object cleanHTML)
    {
        HashMap matchedValuesMap = new HashMap();
        for (String key : keyList) {
            List<String> values = getValues(host, key, text, cleanHTML);
            if (values != null) {
                matchedValuesMap.put(key, values);
            }
        }

        return matchedValuesMap;
    }

    private HashMap getAllMatchedValuesForItemBlock(String host, List<String> keyList, String text, Object cleanHTML) {
        HashMap hashMap = getAllMatchedValues(host, keyList, text, cleanHTML);

        for (Object obj : hashMap.keySet()) {
            String key = (String)obj;

            List values = (List)hashMap.get(key);

            StringBuilder builder = new StringBuilder();

            for (Object value : values) {
                builder.append(value.toString());

                //if (((String)value).charAt(((String)value).length() - 1) != ' ') {
                //    builder.append(" ");
                //}
            }

            values.clear();

            if (StringUtils.isNotBlank(builder.toString())) {
                values.add(builder.toString());
            }
        }

        return hashMap;
    }

    /*private String getValue(String host, String key, String text, Object cleanHTML)
    {
        if (!getSkipXPath()) {
            Map xPathExpressions = getXPathExpressions(host);
            if (!xPathExpressions.isEmpty()) {
                Object expressionObj = xPathExpressions.get(key);
                if (key.equals(NutchConstants.retailer)) {
                    return (String) expressionObj;
                }
                else {
                    String matchedValue = getValue(cleanHTML, expressionObj);
                    if (StringUtils.isNotBlank(matchedValue)) {
                        return matchedValue;
                    }
                }
            }
        }

        if (!getSkipRegEx()) {
            Map regExPatterns = getPatterns(host);
            if (!regExPatterns.isEmpty()) {
                Object patternObj = regExPatterns.get(key);
                if (key.equals(NutchConstants.retailer)) {
                    return ((Pattern) patternObj).pattern();
                }
                else {
                    Matcher m = getMatcher(text, patternObj);
                    if (m != null) {
                        String matchedValue = m.group(m.groupCount());
                        matchedValue = matchedValue.replaceAll("<(.|\n)*?>", "");
                        return matchedValue;
                    }
                }
            }
        }

        return null;
    }*/

    private List<String> getValues(String host, String key, String text, Object cleanHTML) {
        List<String> values = new LinkedList<String>();

        if (!getSkipXPath()) {
            Map xPathExpressions = getXPathExpressions(host);
            if (!xPathExpressions.isEmpty()) {
                Object expressionObj = xPathExpressions.get(key);

                if (key.equals(NutchConstants.retailer)) {
                    values.add((String) expressionObj);
                }
                else {
                    List<String> matchedValues = getValue(cleanHTML, expressionObj);
                    if (matchedValues != null && !matchedValues.isEmpty()) {
                        values.addAll(matchedValues);
                    }
                }
            }

            return values;
        }

        if (!getSkipRegEx()) {
            Map regExPatterns = getPatterns(host);

            if (!regExPatterns.isEmpty()) {
                Object patternObj = regExPatterns.get(key);

                if (key.equals(NutchConstants.retailer)) {
                    values.add(((Pattern) patternObj).pattern());
                }
                else {
                    Matcher m = getMatcher(text, patternObj);
                    if (m != null) {
                        String matchedValue = m.group(m.groupCount());
                        matchedValue = matchedValue.replaceAll("<(.|\n)*?>", "");
                        values.add(matchedValue);
                    }
                }
            }

            return values;
        }

        return values;
    }

    private String getHost(String url)
    {
        Matcher m = Pattern.compile("http://([^/]*)").matcher(url);
        m.find();
        String host = m.group(1);
        return host;
    }

    private boolean isProductPage(String host, String url)
    {
        String configProductPage = ConfigProperties.getInstance().getString(
            "com.shopstyle.nutchx.crawler.FeedCrawler.RegExProductPage.".concat(host));

        if (null != configProductPage) {
            Pattern p = Pattern.compile(configProductPage);
            Matcher m = getMatcher(url, p);
            if (m != null) {
                return true;
            }
            else {
                return false;
            }
        }
        else {
            return true;
        }
    }

    private String getContentAsText(Content content) throws IOException
    {
        String text = new String(content.getContent(), "UTF-8");
        return text;
    }

    private boolean isAvailable(String host, String text)
    {
        String configOutOfStock = ConfigProperties.getInstance().getString(
            "com.shopstyle.nutchx.crawler.FeedCrawler.RegExOutOfStock.".concat(host));

        if (null != configOutOfStock) {
            Pattern p = Pattern.compile(configOutOfStock);
            Matcher m = getMatcher(text, p);
            if (m != null) {
                return false;
            }
            else {
                return true;
            }
        } else {
            return true;
        }
    }

    private List<String> getValue(Object cleanHTML, Object expressionObj)
    {
        List<String> matchedNodes = null;
        if (expressionObj instanceof String) {
            try {
                matchedNodes = XPathUtil.evaluateXPaths((String) expressionObj, cleanHTML);
            }
            catch (XPathExpressionException e) {
                log.warn("Could not evaluate expressionObj, expressionObj was " + expressionObj, e);
                return null;
            }
        }
        else if (expressionObj instanceof List) {
            for (String expression : (List<String>) expressionObj) {
                try {
                    matchedNodes = XPathUtil.evaluateXPaths(expression, cleanHTML);
                }
                catch (XPathExpressionException e) {
                    log.warn("Could not evaluate expression, expression was " + expression, e);
                    return null;
                }
                if (matchedNodes != null && !matchedNodes.isEmpty()) {
                    break;
                }
            }
        }

        return matchedNodes;
    }

    private Map getXPathExpressions(String host)
    {
        Map expressions = (Map) xPathExpressions.get(host);
        if (expressions == null) {
            Map configexpressions = ConfigProperties.getInstance().getMap(
                "com.shopstyle.nutchx.crawler.FeedCrawler.".concat(this.crawlType).concat(".XPath.").concat(host));
            expressions = new HashMap();
            xPathExpressions.put(host, expressions);
            if (!configexpressions.isEmpty()) {
                for (Object key : configexpressions.keySet()) {
                    Object value = configexpressions.get(key);
                    if (value instanceof String) {
                        expressions.put(key, value);
                    }
                    else if (value instanceof List) {
                        ArrayList valueGroup = new ArrayList();
                        for (String valueInGroup : (List<String>) value) {
                            valueGroup.add(valueInGroup);
                        }

                        expressions.put(key, valueGroup);
                    }
                }
            }
            else {
                System.out.println("Config expressions empty");
            }
        }

        return expressions;
    }

    private Matcher getMatcher(String text, Object patternObj)
    {
        Matcher m = null;
        if (patternObj instanceof Pattern) {
            m = ((Pattern) patternObj).matcher(text);
            if (!m.find()) {
                m = null;
            }
        }
        else if (patternObj instanceof List) {
            for (Pattern pattern : (List<Pattern>) patternObj) {
                m = pattern.matcher(text);
                if (m.find()) {
                    break;
                }
                else {
                    m = null;
                }
            }
        }

        return m;
    }

    private Map getPatterns(String host)
    {
        Map patterns = (Map) regExPatterns.get(host);
        if (patterns == null) {
            Map configPatterns = ConfigProperties.getInstance().getMap("com.shopstyle.nutchx.crawler.FeedCrawler.".concat(this.crawlType).concat(".RegEx.").concat(host));
            patterns = new HashMap();
            regExPatterns.put(host, patterns);
            if (!configPatterns.isEmpty()) {
                for (Object key : configPatterns.keySet()) {
                    Object value = configPatterns.get(key);
                    if (value instanceof String) {
                        patterns.put(key, Pattern.compile((String) value, Pattern.DOTALL));
                    }
                    else if (value instanceof List) {
                        ArrayList valueGroup = new ArrayList();
                        for (String valueInGroup : (List<String>) value) {
                            valueGroup.add(Pattern.compile(valueInGroup, Pattern.DOTALL));
                        }

                        patterns.put(key, valueGroup);
                    }
                }
            }
            else {
                System.out.println("Config Patterns empty");
            }
        }

        return patterns;
    }

    protected void processProducts(Map<String, String> matchedValuesMap)
    {
        System.out.println(matchedValuesMap);

        try {
            FileWriter writer = new FileWriter(new File("output/result.xml"), true);
            BufferedWriter bw = new BufferedWriter(writer);

            bw.write(matchedValuesMap.toString() + "\n");
            bw.close();
        }
        catch (IOException e) {
            log.error("Unable to write to output/result.xml", e);
        }
    }

    @Override
    public void setConf(Configuration conf)
    {
        this.conf = conf;
    }

    @Override
    public Configuration getConf()
    {
        return conf;
    }

    public void setRetailerHandle(String retailerHandle)
    {
        this.retailerHandle = retailerHandle;
    }

    public String getRetailerHandle()
    {
        return retailerHandle;
    }

    public void setCrawlType(String crawlType)
    {
        this.crawlType = crawlType;
    }

    public String getCrawlType()
    {
        return crawlType;
    }

    public boolean getSkipXPath()
    {
        return skipXPath;
    }

    public void setSkipXPath(boolean skip)
    {
        skipXPath = skip;
    }

    public boolean getSkipRegEx()
    {
        return skipRegEx;
    }

    public void setSkipRegEx(boolean skip)
    {
        skipRegEx = skip;
    }
}
