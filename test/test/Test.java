package com.shopstyle.nutchx.test;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Element;
import com.shopstyle.nutchx.NutchConstants;
import com.shopstyle.nutchx.XMLUtil;

public class Test
{
    XMLUtil xml;
//    private XPath xpath;
    private static Map<String, Map<Pattern, String>> allPatterns;
    private static Map<String, List> fields=new HashMap();
    private static Map<String, Map<String, ArrayList>> allPatternsX;
    static Map<String, ArrayList> patternsX;
    public Test(){

    }

    public static void main(String[] args){
          new Test().applyPattern();
//        Properties p = System.getProperties();
//        if(p != null){
//            Set keys = p.keySet();
//            Iterator itr = keys.iterator();
//            while( itr.hasNext()){
//                System.out.println("Key = "+itr.next()+" value = "+p.getProperty((String)itr.next()));
//
//            }
       // }
      Calendar cal = new GregorianCalendar();
      cal.set(2012,07,03);
      Calendar curr = new GregorianCalendar();
      long o =  curr.getTime().getTime() - cal.getTime().getTime();
        System.out.println(  " Date diff = "+  o);
//        System.out.println("System property = "+System.getenv("HOME")+"  ---  "+NutchConstants.command4MiniCrawler);
    }

    private void applyPattern(){
        try {
            initPatterns();
            //XPathFactory xfactory = XPathFactory.newInstance();
            //xpath = xfactory.newXPath();
            //initXPath();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        filter();
        //System.out.println((Pattern.compile(pattern)).matcher(content).group() );
    }


    private static void initPatterns() throws IOException
    {
        allPatterns = new HashMap<String, Map<Pattern, String>>();
        BufferedReader reader = new BufferedReader(new FileReader("/Users/cpallapothula/trunk/nutchx/pricepatterns.txt"));
        String line;
        Map<Pattern, String> patterns = null;
        while ((line = reader.readLine()) != null) {
            String[] pat = line.split("\\t");
            if (pat.length > 1) {
                    patterns.put(Pattern.compile(pat[1], Pattern.DOTALL), pat[0]);
            }
            else if (line.length() > 0) { // site name
                    patterns = new HashMap<Pattern, String>();
                    allPatterns.put(line, patterns);
            }
        }
    }

    public static void initXPath() throws IOException
    {
        System.out.println("Initializing Xpath");
        allPatternsX = new HashMap<String, Map<String, ArrayList>>();
        BufferedReader reader = new BufferedReader(new FileReader("/Users/cpallapothula/trunk/nutchx/pricepatterns.txt"));
        String line;
        patternsX = new HashMap<String, ArrayList>();

        while ((line = reader.readLine()) != null) {
            String[] xpath = line.split("\\t");
            if (xpath.length > 1) {
                ArrayList values = null;
                if(patternsX.get(xpath[0]) == null){
                    values = new ArrayList();
                    values.add(xpath[1]);
                }else{
                    values = patternsX.get(xpath[0]);
                    values.add(xpath[1]);
                }
                patternsX.put(xpath[0], values);
            }
            else if (line.length() > 0) { // site name
                allPatternsX.put(line, patternsX);
            }
        }
        System.out.println("End of Initializing Xpath="+allPatternsX.size()+" xpath size"+patternsX.size());
    }

//    private Map<String, ArrayList> getPatterns(String url)
//    {
//        Matcher m = Pattern.compile("http://([^/]*)").matcher(url);
//        m.find();
//        String host = m.group(1);
//        return allPatternsX.get(host);
//    }

    public void filter() {

        HashMap fields = new HashMap<String, List<String>>();

            String url = "www.footcandyshoes.com";
            String price=null;
            String salePrice=null;
            try {
                String text = readFile();
                Map<Pattern, String> patterns = allPatterns.get(url);
                for (Pattern pat : patterns.keySet()) {
                    String key = patterns.get(pat);
                    Matcher m = pat.matcher(text);

                    if (m.groupCount() == 0) {
                        System.out.println("Ret handle = "+pat.toString()); // e.g.
                        continue;
                    }

                    int start = 0;
                    // match each pattern repeatedly (e.g. for category tags)
                    while (start < text.length()) {
                        if (m.find(start)) {
                            String value = m.group(1);
                            if ("Price".equals(key)) {
                                price = value;
                                System.out.println("Price = "+value);
                            }
                            else if ("SalePrice".equals(key)) {
                              salePrice = value;
                            }
                            else if ("ProductId".equals(key)) {
                                System.out.println("ProductId = "+value);
                            }
                            start = m.end();
                            fields.put(key, value);
                        }
                        else {
                            break;
                        }
                      //Make sure the salePrivce is less than the actual prive. Some retailers like foot candy present the data in reverse.
                        if(price != null && salePrice != null&&price.substring(1)!=null&&Double.parseDouble(price.substring(1)) < Double.parseDouble(salePrice.substring(1))){
                            String temp= salePrice;
                            salePrice = price;
                            price = temp;
                        }
                        System.out.println("Sale Price = "+salePrice);
                        System.out.println("Price = "+price);

                    }
                }
            }
            catch (Exception e) {
                System.out.println("Error Occured "+e.getMessage());
                // TODO Auto-generated catch block

            }
            Iterator<String> itr = fields.keySet().iterator();
            while( itr.hasNext()){
                String k = itr.next();
                System.out.println("ky = "+ k+" value "+fields.get(k));
            }
            xml = new XMLUtil();
            xml.constructDocumentWithRootElement("Products");
            writeValuesToProduct(xml.getDocument().createElement("Product"), fields.keySet());
            writeXMLOutput();

    }
//    private String lookupInXpath(ArrayList<String> xpathExpr, String htmlText, String document)
//    {
//            String result = null;
//            try {
//                org.xml.sax.InputSource inputSoure = new InputSource(new FileReader("/Users/cpallapothula/test/footcandy.html"));
//                for(int i=0; i<xpathExpr.size(); i++){
//                    XPathExpression expr = xpath.compile(xpathExpr.get(i));
//                    result = (String)expr.evaluate(inputSoure,XPathConstants.STRING);
//                    if(result != null){
//                        continue;
//                    }
//                }
//            }
//            catch (XPathExpressionException e) {
//                // TODO Auto-generated catch block
//                System.out.println("Error Occured "+e.getMessage());
//                //e.printStackTrace();
//            }
//            catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//            return result;
//    }

    private String readFile(){
         StringBuffer str = new StringBuffer();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/Users/cpallapothula/test/footcandy.html"));
            String content;
            while ((content = reader.readLine()) != null) {
                str.append(content);
            }
        }
        catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return str.toString();
    }

    /**
     *
     */
//    private void printXML(){
//        xml = new XMLUtil();
//        xml.constructDocumentWithRootElement("Products");
//
//        //xml.addTextElementToParent(xml.getRootElement(), "Color", "Black");
//        List l = new ArrayList();
//        l.add("");
//        l.add("Black");
//        l.add("Red");
//        l.add("Brown");
//
//        List url = new ArrayList();
//        url.add("www.black.com");
//        url.add("www.red.com");
//        url.add("www.brown.com");
//
//        List surl = new ArrayList();
//        surl.add("www.sblack.com");
//        surl.add("www.sred.com");
//
//        List consol = new ArrayList();
//        consol.add(xml.addElements4values("Name",l));
//        consol.add(xml.addElements4values("imageurl",url));
//        consol.add(xml.addElements4values("swatchurl",surl));
//
//        xml.addGroupElements(xml.getRootElement(),"Color", consol);
//        xml.addTextElementToParent(xml.getRootElement(), "Brand", "Old Navy");
//        xml.addTextElementToParent(xml.getRootElement(), "Description", "I am writing a test to test the Nuth Crawler. Today's date is 20120531");
//
//        Set keys = new HashSet();
//        keys.add("Color:Name");
//        keys.add("Color:imageurl");
//        keys.add("Color:swatchurl");
//        keys.add("size:Name");
//
//
//
//        writeValuesToProduct(xml.getRootElement(), keys);
//
//        writeXMLOutput();
//
//    }

    private void writeXMLOutput() throws TransformerFactoryConfigurationError
    {
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = null;
        try {
            trans = transfac.newTransformer();
        }
        catch (TransformerConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(xml.getDocument());
        try {
            trans.transform(source, result);
        }
        catch (TransformerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String xmlString = sw.toString();

        System.out.print(xmlString);
    }
    public boolean isProductInvalidOnSite(Set keys){
        if(keys!= null){
            if(keys.contains(NutchConstants.price) && fields.get(NutchConstants.price) == null){
                   return false;
            }


        }
        return true;

    }

//    private void addField(String key, String value)
//    {
//      List<String> values = fields.get(key);
//      if (values == null) {
//        values = new ArrayList<String>();
//        fields.put(key, values);
//      }
//      values.add(value);
//      System.out.println("ky = "+ key+" value "+values);
//
//    }

    private void writeValuesToProduct(Element product, Set keys)
    {
        if(!isProductInvalidOnSite(keys)){
            return;
        }
        Iterator<String> itr = keys.iterator();
        //Logic to group the children under the same parent.
        //This logic only considers to group one level.
        // in the future, we can extend this to multi level of needed.
        String key = "";
        Map<String, Map> groups = new HashMap();
        while(itr.hasNext()){
            key = itr.next();
            if (key.contains(":")) {
                String[] parts = key.split(":");
                Map groupChildElements;
                if(groups.get(parts[0]) != null){
                    groupChildElements = groups.get(parts[0]);
                }else{
                    groupChildElements = new HashMap();
                }
                groupChildElements.put(parts[1], fields.get(key));
                groups.put(parts[0], groupChildElements);
            }
        }
        System.out.println("Done with identifying the groups size === "+groups.size());
        //Now that we have identified the groups, lets create the groupXMLElements.
        Iterator<String> grpKeyItr = groups.keySet().iterator();
        while(grpKeyItr.hasNext()){
            String grpKey = grpKeyItr.next();
            List childElementXML = new ArrayList();

            Map<String, List> childElements = groups.get(grpKey);
            Iterator<String> childElementItr = childElements.keySet().iterator();
            while(childElementItr.hasNext()){
                String childElemKey = childElementItr.next();
                childElementXML.add(xml.addElements4values(childElemKey,childElements.get(childElemKey)));
            }
            xml.addGroupElements(product, grpKey, childElementXML);
            System.out.println("Added group element to Product grp element =  === "+grpKey);
        }

        // Repeating the loop to create XML for the non-group elements
        itr = keys.iterator();
        while(itr.hasNext()){
            key = itr.next();
            List<String> values = fields.get(key);
            if (values != null) {
                if (!key.contains(":")) {
                    for (String value : values) {
                        xml.addTextElementToParent(product,key, value);
                    }
                }
            }
        }

        xml.addTextElementToParent(product,"Time", "20120609");
        xml.addElementToParent(xml.getRootElement(), product);
    }
}
