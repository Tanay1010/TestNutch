package com.shopstyle.nutchx;

public class NutchConstants
{
    public static String xpathFileName = "ssxpaths.txt";
    public static String patternsFileName = "sspatterns.txt";

    public static String outOfStock = "OutOfStock";

    public static String nutchUrlDir = System.getenv("HOME")+"/apache-nutch-1.4-bin/runtime/local/urls/";
    public static String affiliateFeedDir = System.getenv("HOME")+"/affiliateFeeds/incoming/";
    public static String inputDir = System.getenv("HOME")+"/inputFeeds/incoming/";

    public static String crawlerOutputDir = System.getenv("HOME")+"/service/data/feed/image/";
    public static String command4MiniCrawler = System.getenv("HOME")+"/apache-nutch-1.4-bin/runtime/local/bin/nutch crawl urls -dir crawl -depth 1 -topN 2";


    public static String global = "Global";

    public static String[] productNotAvailable = new String[]{"it has sold out",
                                                               "Product Not Available",
                                                               "This product is currently unavailable",
                                                               " Product you requested is no longer available",
                                                               "Something happened with your request",
                                                               "item has sold out",
                                                               "product is no longer available"};

    /**
     * Tag Names
     */
    public static String products = "Products";
    public static String product = "Product";
    public static String retailer = "Retailer";
    public static String productId = "ProductId";
    public static String name = "Name";
    public static String brand = "Brand";
    public static String price = "Price";
    public static String salePrice = "SalePrice";
    public static String description = "Description";
    public static String productUrl = "ProductURL";
    public static String imageURL = "ImageURL";
    public static String category = "Category";
    public static String part = "Part";
    public static String size = "Size";
    public static String color = "Color";
    public static String swatchURL = "SwatchURL";
    public static String instock = "InStock";
    public static String sku = "Sku";
    public static String time = "Time";

    /**
     * Review Only Crawl Tag Names
     */
    public static String reviewId = "ReviewId";
    public static String title = "Title";
    public static String starRating = "StarRating";
    public static String author = "Author";
    public static String date = "Date";


    /**
     * Config Keys
     */
    public static String colorSwatchURL = "Color:SwatchURL";
    public static String colorImageURL = "Color:ImageURL";
    public static String price1 = "Price1";
    public static String cutOffStart = "CutOffStart";
    public static String cutOffEnd = "CutOffEnd";
}
