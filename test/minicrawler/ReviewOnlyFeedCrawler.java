package com.shopstyle.nutchx.minicrawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.shopstyle.nutchx.FeedCrawler;
import com.shopstyle.nutchx.NutchConstants;

public class ReviewOnlyFeedCrawler extends FeedCrawler
{
    private static final Logger log = Logger.getLogger(ReviewOnlyFeedCrawler.class);

    public ReviewOnlyFeedCrawler() {
        this.setCrawlType("ReviewOnlyCrawl");
        this.setSkipRegEx(true);
        this.setSkipXPath(false);
    }

    @Override
    /**
     * Writing the output to the XML file.
     */
    protected void processProducts(Map matchedValuesMap)
    {
        List<String> retailer = (List)matchedValuesMap.get(NutchConstants.retailer);
        List<String> productId = (List)matchedValuesMap.get(NutchConstants.productId);
        List<String> reviewId = (List)matchedValuesMap.get(NutchConstants.reviewId);
        List<String> title = (List)matchedValuesMap.get(NutchConstants.title);
        List<String> description = (List)matchedValuesMap.get(NutchConstants.description);
        List<String> starRating = (List)matchedValuesMap.get(NutchConstants.starRating);
        List<String> author = (List)matchedValuesMap.get(NutchConstants.author);
        List<String> date = (List)matchedValuesMap.get(NutchConstants.date);

        StringBuilder builder = new StringBuilder();

        Integer[] lengths = {new Integer(retailer.size()), new Integer(productId.size()), new Integer(reviewId.size()), new Integer(title.size()), new Integer(description.size()), new Integer(starRating.size()), new Integer(author.size()), new Integer(date.size())};

        List<Integer> lengthsList = Arrays.asList(lengths);

        if (validList(retailer, 0) && validList(productId, 0)) {
            for (int i = 0; i < Collections.max(lengthsList); i++) {
                builder.append("<Review>\n");

                builder.append("    <Retailer>" + retailer.get(0) + "</Retailer>\n");
                builder.append("    <ProductId>" + productId.get(0) + "</ProductId>\n");

                builder.append("    <ReviewId>");
                if (validList(reviewId, i)) {
                    builder.append(reviewId.get(i).trim());
                }
                builder.append("</ReviewId>\n");

                builder.append("    <Title>");
                if (validList(title, i)) {
                    builder.append(title.get(i).trim());
                }
                builder.append("</Title>\n");

                builder.append("    <Description>");
                if (validList(description, i)) {
                    builder.append(description.get(i).trim());
                }
                builder.append("</Description>\n");

                builder.append("    <StarRating>");
                if (validList(starRating, i)) {
                    builder.append(starRating.get(i).trim());
                }
                builder.append("</StarRating>\n");

                builder.append("    <Author>");
                if (validList(author, i)) {
                    builder.append(author.get(i).trim());
                }
                builder.append("</Author>\n");

                builder.append("    <Date>");
                if (validList(date, i)) {
                    builder.append(date.get(i).trim());
                }
                builder.append("</Date>\n");

                builder.append("</Review>\n");
            }
        }

        try {
            FileWriter writer = new FileWriter(new File("output/result.xml"), true);
            BufferedWriter bw = new BufferedWriter(writer);

            bw.write(builder.toString());
            bw.close();
        }
        catch (IOException e) {
            log.error("Unable to write to output/result.xml", e);
        }
    }

    private boolean validList(List list, int i) {
        return list != null && !list.isEmpty() && i < list.size();
    }
}
