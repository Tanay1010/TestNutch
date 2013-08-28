package com.shopstyle.nutchx.minicrawler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.shopstyle.importer.ProductBuilder;
import com.shopstyle.importer.ProductImportDriverService;
import com.shopstyle.nutchx.FeedCrawler;
import com.shopstyle.nutchx.NutchConstants;
import com.shopstyle.nutchx.NutchWrapper.SingletonHolder;

public class PriceOnlyFeedCrawlerToDB extends FeedCrawler
{
    private static final Logger log = Logger.getLogger(PriceOnlyFeedCrawlerToDB.class);

    public PriceOnlyFeedCrawlerToDB()
    {
        setCrawlType("PriceOnlyCrawl");
    }

    @Override
    /**
     * Writing the output to the XML file.
     */
    protected void processProducts(Map matchedValuesMap)
    {
        String retailer = (String)((List)matchedValuesMap.get(NutchConstants.retailer)).get(0);
        String retailerProductId = (String)((List)matchedValuesMap.get(NutchConstants.productId)).get(0);
        String productUrl = (String)((List)matchedValuesMap.get(NutchConstants.productUrl)).get(0);
        String currencyAndPriceRange = (String)((List)matchedValuesMap.get(NutchConstants.price)).get(0);
        String salePrice = (String)((List)matchedValuesMap.get(NutchConstants.salePrice)).get(0);

        if (StringUtils.isNotBlank(retailer) && StringUtils.isNotBlank(retailerProductId) &&
                StringUtils.isNotBlank(currencyAndPriceRange)) {

            final ProductBuilder pb = new ProductBuilder();
            pb.setFeedRetailerName(retailer);
            pb.setRetailerProductId(retailerProductId);
            //CP: Please do not add productURL for SFU. this will override the database URL.
            //pb.addProductUrl(productUrl, null, null);   //let NPE be thrown if productUrl is null
            pb.setCurrencyAndPriceRange(currencyAndPriceRange);
            if (StringUtils.isNotBlank(salePrice)) {
                pb.setSalePriceRange(salePrice);
            }
            log.info("Thread " + Thread.currentThread().getId() + " producing: " + pb);

            ExecutorService executorService = SingletonHolder.Instance.getExecutorService();
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    ProductImportDriverService driver = SingletonHolder.Instance.getDriver();
                    log.info("Thread " + Thread.currentThread().getId() + " consuming: " + pb);
                    driver.processProductService(pb);
                }
            });
        }
        else {
            log.warn(String.format("Skipped product {%s} due to blank values {retailer:%s, retailerProductId:%s, currencyAndPriceRange:%s}",
                    productUrl, retailer, retailerProductId, currencyAndPriceRange));
        }
    }
}
