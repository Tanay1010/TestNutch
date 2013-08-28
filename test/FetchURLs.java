package com.shopstyle.nutchx;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.shopstyle.core.bo.Product;
import com.shopstyle.core.bo.ProductUrl;
import com.shopstyle.core.bo.ProductUrl.Type;
import com.shopstyle.core.matcher.RetailerMatcher;
import com.shopstyle.hibernatex.HibernateUtil;
import com.shopstyle.hibernatex.TimestampedObject;
import com.shopstyle.model.Retailer;
import com.shopstyle.util.CollectionUtil;
import com.shopstyle.util.UrlUtil;

public class FetchURLs
{
    private static final Logger log = Logger.getLogger(FetchURLs.class);

    public static void usage() {
        System.err.println("usage: FetchURLs -retailer <retailerHandle> [-stripText <text-to-strip>] [-stripParams <pipe-delimited-param-names>] [-writeUrlsToDir <path-to-urls-dir>]");
    }
    public static void main(String[] args)
    {
        BasicConfigurator.configure();  //log4j config

        try {
            String[] stripText = null;
            String[] stripParams = null;
            String retailerHandle = null;
            String urlsDirPath = null;
            boolean productUrlIsNotDirectUrl = false;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("-stripText")) {
                    i++;
                    if (i >= args.length) {
                        usage();
                        System.exit(-1);
                    }
                    String stripTextAsString = args[i];
                    stripText = stripTextAsString.split("\\s*\\|\\s*");
                }
                if (arg.equals("-stripParams")) {
                    i++;
                    if (i >= args.length) {
                        usage();
                        System.exit(-1);
                    }
                    String stripParamsAsString = args[i];
                    stripParams = stripParamsAsString.split("\\s*\\|\\s*");
                    if (stripParams.length == 0) {
                        System.err.println("invalid -stripParams: " + stripParamsAsString);
                        System.exit(-1);
                    }
                }
                if (arg.equals("-retailer")) {
                    i++;
                    if (i >= args.length) {
                        usage();
                        System.exit(-1);
                    }
                    retailerHandle = args[i];
                }
                if (arg.equals("-writeUrlsToDir")) {
                    i++;
                    if (i >= args.length) {
                        usage();
                        System.exit(-1);
                    }
                    urlsDirPath = args[i];
                }
                if (arg.equals("-productUrlIsNotDirectUrl")) {
                    if (i >= args.length) {
                        usage();
                        System.exit(-1);
                    }
                    productUrlIsNotDirectUrl = true;
                }
            }

            if (retailerHandle == null) {
                System.err.println("-retailer was not supplied and is required");
                System.exit(-1);
            }

            if (urlsDirPath == null) {
                String nutchHome = System.getenv("NUTCH_HOME");
                if (nutchHome == null) {
                    System.err.println("if -writeUrlsToDir is not supplied, NUTCH_HOME must be set");
                    System.exit(-1);
                }
                urlsDirPath = nutchHome + "/urls";
            }

            File urlsDir = new File(urlsDirPath);

            queryAndWriteProductUrls(stripText, stripParams, retailerHandle, urlsDir,
                productUrlIsNotDirectUrl, Integer.MIN_VALUE);
        }
        catch (Exception e) {
            log.error("Uncaught exception: ", e);
            System.exit(-1);
        }
        System.exit(0);
    }

    /**
     * @param stripText a text string that, if found, should be stripped from the URL prior to writing the URL
     * @param stripParams names of query parameters whose name-value pair should be stripped from the URL prior to writing the URL
     * @param retailerHandle - handle of the retailer whose URLs will be fetched
     * @param urlsDirPath - directory into which to write the {retailerHandle}.txt file containing the URLs (example: "/Users/me/apache-nutch-1.4-bin/runtime/local/urls")
     * @return number of URLs written to the file
     */
    public static int queryAndWriteProductUrls(String[] stripText, String[] stripParams,
        String retailerHandle, File urlsDir, boolean productUrlIsNotDirectUrl, int topN)
        throws IOException
    {
        if (StringUtils.isBlank(retailerHandle)) {
            throw new IllegalArgumentException("blank retailerHandle: " + retailerHandle);
        }

        if (urlsDir == null) {
            //throw new IllegalArgumentException("urlsDir was null");
        }

        Session session = HibernateUtil.createSession();
        RetailerMatcher retailerMatcher = new RetailerMatcher(session);
        Retailer retailer = retailerMatcher.matchRetailer(retailerHandle);
        List<String> productUrlList = new ArrayList();
        Criteria criteria = HibernateUtil.createCriteria(session, Product.class)
            .add(Restrictions.eq(Product.Attributes.retailer, retailer))
            .add(Restrictions.eq(Product.Attributes.status, Product.ValidStatus))
            .add(Restrictions.eq(Product.Attributes.latest, Boolean.TRUE));
        criteria.setProjection(Projections.projectionList().add(Projections.property("url"))
            .add(Projections.property(TimestampedObject.Attributes.id)));
        //if (noOfProducts > Integer.MIN_VALUE) {
        //    criteria.setMaxResults(noOfProducts);
        //}
        List<Object[]> idList = criteria.list();
        if (CollectionUtil.isNonEmpty(idList)) {
            for (Object[] idAry : idList) {
                if (!productUrlIsNotDirectUrl) {
                    productUrlList.add((String) idAry[0]);
                }else{
                    criteria = HibernateUtil.createCriteria(session, Product.class)
                        .add(Restrictions.eq("id", idAry[1]))
                        .add(Restrictions.eq("latest", Boolean.TRUE));
                    Product product = (Product) criteria.uniqueResult();
                    if (product != null) {
                        ProductUrl purl = product.getUrlByPreference(Type.DIRECT);
                        if (purl != null) {
                            productUrlList.add(purl.getUrl());
                        }
                    }

                }
            }
        }

        int urlsWritten = 0;
        if (CollectionUtil.isNullOrEmpty(productUrlList)) {
            log.warn("productUrlList was empty for retailerHandle " + retailerHandle);
            return urlsWritten;
        }

        File outputFile = new File(urlsDir, retailerHandle.replace(" ", "_") + ".txt");
        if (!urlsDir.exists()) {
            urlsDir.mkdirs();
        }

        PrintWriter out = null;
        try {
            FileWriter fileWriter = new FileWriter(outputFile);
            out = new PrintWriter(fileWriter, false);      //won't throw exception

            for (String productURL : productUrlList) {
                productURL = stripURL(productURL, stripText, stripParams);
                out.println(productURL);
                urlsWritten++;
            }
        }
        finally {
            //if fileWriter creation failed, it doesn't need to be closed
            if (out != null) {
                out.close();
            }
        }

        log.info(String.format("wrote %d %s URLs to %s", urlsWritten, retailerHandle, outputFile));
        return urlsWritten;
    }

    private static String stripURL(String productURL, String[] stripTexts, String[] stripParams)
    {
        if (StringUtils.isEmpty(productURL)) {
            return productURL;
        }
        int index;
        if (stripParams != null && stripParams.length > 0) {
            for (String param : stripParams) {
                productURL = UrlUtil.removeParameter(productURL, param);
            }
        }
        if (stripTexts != null && stripTexts.length > 0) {
            for (String stripText : stripTexts) {
                if (StringUtils.isNotBlank(stripText) && (index = productURL.indexOf(stripText)) != -1) {
                    productURL = productURL.substring(0, index)
                            + productURL.substring(index + stripText.length(), productURL.length());
                }
            }
        }
        try {
            productURL = URLDecoder.decode(productURL, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return productURL;
    }
}
