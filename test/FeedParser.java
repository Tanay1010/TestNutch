package com.shopstyle.nutchx;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.Currency;
import com.shopstyle.core.bo.Color;
import com.shopstyle.core.bo.InStock;
import com.shopstyle.importer.ProductBuilder;
import com.shopstyle.importer.ProductBuilder.ProductBuilderException;
import com.shopstyle.importer.ProductImportDriver2;
import com.shopstyle.importer.ProductImportDriverService;
import com.shopstyle.nutchx.feed.ProductHolder;
import com.shopstyle.nutchx.feed.RetailerProduct;
import com.shopstyle.nutchx.feed.TextFeed;
import com.shopstyle.nutchx.feed.XMLFeed;
import com.shopstyle.nutchx.feed.XMLProduct;

public class FeedParser
{
	private static String categoryDelimiter;
	private static String sizeDelimiter;
	private static String colorDelimiter;
	private static NumberFormat currencyFormatter;
	private static NumberFormat priceStringFormatter;
	private static String urlSplitter;
	private static HashMap products = new HashMap();
	private static SimpleDateFormat sdf;

	private static ProductImportDriverService driver;
	private static Logger log = Logger.getLogger(XMLProduct.class);
	private static String outputXMLLocation;
	private static Document xmlDocumentOutput;

	public static void parseTextFeed(String fileLocation, String delimiter,
			String attributeMappingKey, int skipLines) throws ProductBuilderException, ParseException
			{
		try {
			RetailerFeed retailerFeed = new TextFeed(fileLocation, delimiter, attributeMappingKey,
					skipLines);
			parseFeed(retailerFeed);
		}
		catch (IOException e) {
			log.warn("Could not parse text feed for " + attributeMappingKey, e);
		}
			}

	public static void parseXMLFeed(String fileLocation, String productTagName,
			String attributeMappingKey) throws ProductBuilderException, ParseException, SAXException,
			ParserConfigurationException
			{
		RetailerFeed retailerFeed;
		try {
			retailerFeed = new XMLFeed(fileLocation, productTagName, attributeMappingKey);
			parseFeed(retailerFeed);
		}
		catch (IOException e) {
			log.warn("Could not parse xml feed for " + attributeMappingKey, e);
		}
			}

	private static void parseFeed(RetailerFeed retailerFeed) throws IOException,
	ProductBuilderException, ParseException
	{
		sdf = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z", Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		int counter = 0;
		RetailerProduct product = retailerFeed.nextProduct();
		while (null != product) {
			if (product.addProduct()) {
				String retailerProductId = product.getAttribute(NutchConstants.productId);
				String retailer=product.getAttribute(NutchConstants.retailer);

				if (StringUtils.isNotBlank(retailerProductId)) {
					ArrayList multilinedProduct = (ArrayList) products.get(retailerProductId);
					if (null == multilinedProduct) {
						multilinedProduct = new ArrayList();
						products.put(retailerProductId, multilinedProduct);
					}
					multilinedProduct.add(product);
					counter++;
					if (counter % 1000 == 0) {
						log.info("Processed a batch of 1000 lines.");
					}
				}
			}
			product = retailerFeed.nextProduct();
		}
		log.info(String.format("Processed a total of %d lines.", counter));

		if (products != null) {
			counter = 0;
			Iterator it = products.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pairs = (Map.Entry) it.next();
				ArrayList multilinedProduct = (ArrayList) pairs.getValue();

				storeProduct(multilinedProduct);
				counter++;

				if (counter % 1000 == 0) {
					log.info("Processed a batch of 1000 products.");
				}
			}
			log.info(String.format("Processed a total of %d products.", counter));
		}
	}

	private static void storeProduct(ArrayList multilinedProduct) throws ProductBuilderException,
	ParseException, UnsupportedEncodingException
	{
		RetailerProduct product = (RetailerProduct) multilinedProduct.get(0);
		String retailer = product.getAttribute(NutchConstants.retailer);
		String retailerProductId = product.getAttribute(NutchConstants.productId);
		if(retailer.equalsIgnoreCase("Gaastra DE") | retailer.equalsIgnoreCase("Gaastra FR"))
		{
			retailerProductId=retailerProductId.replaceAll("\\s+.*", "");
		}
		String name = product.getAttribute(NutchConstants.name);
		name = StringEscapeUtils.unescapeXml(name);
		name = StringEscapeUtils.unescapeHtml(name);
		String brand = product.getAttribute(NutchConstants.brand);
		brand = StringEscapeUtils.unescapeXml(brand);
		brand = StringEscapeUtils.unescapeHtml(brand);

		if(StringUtils.isNotBlank(brand) && ! brand.equalsIgnoreCase(name)  && !brand.equalsIgnoreCase(retailer))
		{
			/*
			 * if Brand is not present in Product Name, then Brandname should be appended to Product Name,
			 * and Retailer Name not equal to BrandName.  
			 */	
			name=brand+ " "+ name;  
		}
		
		String description = product.getAttribute(NutchConstants.description);
		description = StringEscapeUtils.unescapeXml(description);
		description = StringEscapeUtils.unescapeHtml(description);
		description = description.trim();
		String productUrl = product.getAttribute(NutchConstants.productUrl);
		String imageUrl = product.getAttribute(NutchConstants.imageURL);
		String category = product.getAttribute(NutchConstants.category);

		String[] categoryArray = { category };

		if (null != categoryDelimiter && null != category) {
			categoryArray = category.split(categoryDelimiter);
		}

		if (StringUtils.isNotBlank(retailer) && StringUtils.isNotBlank(retailerProductId)) {
			ProductHolder ph;
			if (xmlDocumentOutput != null) {
				ph = new ProductHolder(xmlDocumentOutput);
			}
			else {
				ph = new ProductHolder();
			}
			ph.setFeedRetailerName(retailer);
			ph.setRetailerProductId(retailerProductId);
			ph.setProductName(name);
			if (StringUtils.isNotBlank(brand)) {
				ph.setRetailerBrand(brand);
			}
			if(StringUtils.isNotBlank(urlSplitter) && productUrl.contains(urlSplitter) && productUrl.contains("=http"))
			{
				String feedUrl=getFeedURL(productUrl);
				feedUrl=feedUrl.replaceAll("\\]", "");
				feedUrl=feedUrl.replaceAll(".*?=(http.*)", "$1");
				ph.addProductUrl(productUrl, "feed", null);   //let NPE be thrown if productUrl is null
				ph.addProductUrl(feedUrl, "direct", null);
			}
			else
			{
				ph.addProductUrl(productUrl, null, null);   //let NPE be thrown if productUrl is null
			}

			ph.addProductImageUrl(imageUrl);
			ph.setDescription(description);

			for (String categoryName : categoryArray) {
				if (StringUtils.isNotBlank(categoryName)) {
					categoryName = StringEscapeUtils.unescapeXml(categoryName);
					categoryName = StringEscapeUtils.unescapeHtml(categoryName);
					categoryName=categoryName.trim();

					ph.addRetailerCategory(categoryName);
				}
			}

			ArrayList allSizes = new ArrayList();
			ArrayList allColors = new ArrayList();
			ArrayList sizeColorCombination = new ArrayList();
			double minRetailPrice = 0;
			double maxRetailPrice = 0;
			double minSalePrice = 0;
			double maxSalePrice = 0;
			double retailPriceDbl = 0;
			double salePriceDbl = 0;

			for (RetailerProduct productLine : (ArrayList<RetailerProduct>) multilinedProduct) {
				String retailPriceStr = productLine.getAttribute(NutchConstants.price);
if(retailPriceStr.equalsIgnoreCase("GBP"))
			{
	System.out.println(retailerProductId);
	System.exit(0);
			}
				if (StringUtils.isNotBlank(retailPriceStr)) {
					retailPriceDbl = priceStringFormatter.parse(retailPriceStr)
							.doubleValue();

					if (retailPriceDbl < minRetailPrice || minRetailPrice == 0) {
						minRetailPrice = retailPriceDbl;
					}
					if (retailPriceDbl > maxRetailPrice || maxRetailPrice == 0) {
						maxRetailPrice = retailPriceDbl;
					}

				}

				String salePriceStr = productLine.getAttribute(NutchConstants.salePrice);
				if (StringUtils.isNotBlank(salePriceStr)) {
					salePriceDbl = priceStringFormatter.parse(salePriceStr).doubleValue();
					if (salePriceDbl < minSalePrice || minSalePrice == 0) {
						minSalePrice = salePriceDbl;
					}
					if (salePriceDbl > maxSalePrice || maxSalePrice == 0) {
						maxSalePrice = salePriceDbl;
					}
				}
				else {
					if (retailPriceDbl < minSalePrice || minSalePrice == 0) {
						minSalePrice = retailPriceDbl;
					}
					if (retailPriceDbl > maxSalePrice || maxSalePrice == 0) {
						maxSalePrice = retailPriceDbl;
					}
				}

				String size = productLine.getAttribute(NutchConstants.size);

				String[] sizeArray = { size };
				if (null != sizeDelimiter && null != size) {
					sizeArray = size.split(sizeDelimiter);
				}
				else if (null == size) {
					sizeArray = new String[0];
				}
				// DCA - add all sizes to ProductBuilder
				for (String sizeName : sizeArray) {
					if (StringUtils.isNotBlank(sizeName) && !allSizes.contains(sizeName)) {
						sizeName=sizeName.trim();
						ph.addProductSize(sizeName);
						allSizes.add(sizeName);
					}
				}

				String color = productLine.getAttribute(NutchConstants.color);
				String[] colorArray = { color };
				if (null != colorDelimiter && null != color) {
					colorArray = color.split(colorDelimiter);
				}
				else if (null == color) {
					colorArray = new String[0];
				}
				// DCA - add all colors to ProductBuilder
				for (String colorName : colorArray) {
					if (StringUtils.isNotBlank(colorName) && !allColors.contains(colorName)) {
						Color c = new Color();
						ProductBuilder.setColorName(c, colorName);
						String colorImageUrl = productLine.getAttribute(NutchConstants.colorImageURL);
						if (StringUtils.isNotBlank(colorImageUrl)) {
							ProductBuilder.setColorProductImageUrl(c, colorImageUrl);
						}
						String colorSwatchUrl = productLine.getAttribute(NutchConstants.colorSwatchURL);
						if (StringUtils.isNotBlank(colorSwatchUrl)) {
							ProductBuilder.setSwatchImageUrl(c, colorSwatchUrl);
						}
						ph.addColor(c);
						allColors.add(colorName);
					}

					if (StringUtils.isNotBlank(colorName)) {
						String sku = productLine.getAttribute(NutchConstants.sku);
						if (StringUtils.isNotBlank(sku)) {
							for (String sizeName : sizeArray) {
								// DCA - if color/size combination haven't been added, add it to Product Builder
								String combo = colorName.concat(":").concat(sizeName);
								if (StringUtils.isNotBlank(sizeName)
										&& !sizeColorCombination.contains(combo)) {
									InStock inStock = new InStock();
									ph.setInStockProductSize(inStock, sizeName);
									ph.setInStockColorName(inStock, colorName);
									ph.setInStockSku(inStock, sku);
									ph.addInStock(inStock);
									sizeColorCombination.add(combo);
								}
							}
						}
					}
				}
			}
			if (minRetailPrice == 0) {
				log.warn(String.format("Skipped product {%s} due to blank price.", productUrl));
			}
			else {
				String currencyAndPriceRange = currencyFormatter.format(minRetailPrice);
				if (minRetailPrice != maxRetailPrice) {
					String maxRetailPriceStr = currencyFormatter.format(maxRetailPrice);
					currencyAndPriceRange = currencyAndPriceRange.concat(" - ").concat(
							maxRetailPriceStr);
				}
				ph.setCurrencyAndPriceRange(currencyAndPriceRange);


				String salePriceRange = currencyFormatter.format(minSalePrice);
				if (minSalePrice != maxSalePrice) {
					String maxSalePriceStr = currencyFormatter.format(maxSalePrice);
					salePriceRange = salePriceRange.concat(" - ").concat(
							maxSalePriceStr);
				}
				if (!currencyAndPriceRange.equals(salePriceRange)) {
					ph.setSalePriceRange(salePriceRange);
				}

				String dateString = sdf.format(new Date());
				ph.setExtractDate(dateString);

				if (xmlDocumentOutput == null) {
					driver.processProductService(ph.getProductBuilder());
				}
			}

		}
		else {
			log.warn(String
					.format(
							"Skipped product {%s} due to blank values {retailer:%s, retailerProductId:%s}",
							productUrl, retailer, retailerProductId));
		}
	}

	
	private static String getFeedURL(String uRL) {
		String FeedURL = null;
		Pattern p=Pattern.compile("%([a-fA-F0-9][a-fA-F0-9])");
		Matcher match=p.matcher(uRL);
		if(match.find())
		{
			FeedURL=URLDecoder.decode(uRL);
			Pattern p1=Pattern.compile("%([a-fA-F0-9][a-fA-F0-9])");
			Matcher match1=p.matcher(FeedURL);
			if(match1.find())
			{
				FeedURL=getFeedURL(FeedURL);
			}

		}
		else
		{
			FeedURL=uRL;
		}

		return FeedURL;
	}

	public static void usage()
	{
		System.err
		.println("usage: FeedParser -feed <path-to-feed> -key <attributeMappingKey> [-productDelimiter <string>] [-productTagName <string>] [-skipLines <num-lines-to-skip>] [-categoryDelimiter <string>] [-sizeDelimiter <string>] [-colorDelimiter <string>] [-sfu] [-dontRetireMissingProducts] [-instanceId <string>] [-priceStringFormat <languageCode>] [-currencyFormat <languageCode> <currencyCode>] [-outputXML <path-to-output-file>]");
	}

	public static void main(String[] args)
	{
		//grab timestamp
		SimpleDateFormat format = new SimpleDateFormat("MMddHHmmss");
		String timestamp = format.format(new Date());
		//BasicConfigurator.configure();  //log4j config

		try {
			String feed = null;
			String key = null;
			String productDelimiter = null;
			String productTagName = null;
			int skipLines = 0;
			boolean sfu = false;
			boolean dontRetireMissingProducts = false;
			String instanceId = null;
			Locale currencyLocale = null;

			for (int i = 0; i < args.length; i++) {
				String arg = args[i];
				if (arg.equals("-feed")) {
					i++;
					if (i >= args.length) {
						usage();
						System.exit(-1);
					}
					feed = args[i];
				}
				else if (arg.equals("-key")) {
					i++;
					if (i >= args.length) {
						usage();
						System.exit(-1);
					}
					key = args[i];
				}
				else if (arg.equals("-productDelimiter")) {
					i++;
					if (i >= args.length) {
						usage();
						System.exit(-1);
					}
					productDelimiter = args[i];
				}
				else if (arg.equals("-productTagName")) {
					i++;
					if (i >= args.length) {
						usage();
						System.exit(-1);
					}
					productTagName = args[i];
				}
				else if (arg.equals("-skipLines")) {
					i++;
					if (i >= args.length) {
						usage();
						System.exit(-1);
					}
					skipLines = Integer.parseInt(args[i]);
				}
				else if (arg.equals("-categoryDelimiter")) {
					i++;
					if (i >= args.length) {
						usage();
						System.exit(-1);
					}
					categoryDelimiter = args[i];
				}
				else if (arg.equals("-sizeDelimiter")) {
					i++;
					if (i >= args.length) {
						usage();
						System.exit(-1);
					}
					sizeDelimiter = args[i];
				}
				else if (arg.equals("-colorDelimiter")) {
					i++;
					if (i >= args.length) {
						usage();
						System.exit(-1);
					}
					colorDelimiter = args[i];
				}
				else if (arg.equals("-urlSplitter")) {
					i++;
					//					if (i >= args.length) {
					//						usage();
					//						System.exit(-1);
					//					}
					urlSplitter = args[i];
				}
				else if (arg.equals("-sfu")) {
					sfu = true;
				}
				else if (arg.equals("-dontRetireMissingProducts")) {
					dontRetireMissingProducts = true;
				}
				else if (arg.equals("-instanceId")) {
					i++;
					if (i >= args.length) {
						usage();
						System.exit(-1);
					}
					instanceId = args[i];
				}
				else if (arg.equals("-priceStringFormat")) {
					i++;
					if (i >= args.length) {
						usage();
						System.exit(-1);
					}
					String languageCode = args[i];

					Locale priceLocale = new Locale(languageCode);
					priceStringFormatter = NumberFormat.getInstance(priceLocale);
				}
				else if (arg.equals("-currencyFormat")) {
					i++;
					if (i >= args.length) {
						usage();
						System.exit(-1);
					}
					String languageCode = args[i];

					i++;
					if (i >= args.length) {
						usage();
						System.exit(-1);
					}
					String currencyCode = args[i];

					currencyLocale = new Locale(languageCode);
					currencyFormatter = NumberFormat.getCurrencyInstance(currencyLocale);
					Currency priceCurrency = Currency.getInstance(currencyCode);
					currencyFormatter.setCurrency(priceCurrency);
				}
				else if (arg.equals("-outputXML")) {
					i++;
					if (i >= args.length) {
						usage();
						System.exit(-1);
					}
					DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

					// root elements
					xmlDocumentOutput = docBuilder.newDocument();
					Element rootElement = xmlDocumentOutput.createElement(NutchConstants.products);
					xmlDocumentOutput.appendChild(rootElement);
					outputXMLLocation = args[i];
				}
			}

			if (feed == null) {
				System.err.println("-feed was not supplied and is required");
				System.exit(-1);
			}
			if (key == null) {
				System.err.println("-key was not supplied and is required");
				System.exit(-1);
			}
			if (productDelimiter == null && productTagName == null) {
				System.err
				.println("-productDelimiter and -productTagName was not supplied and one of them is required");
				System.exit(-1);
			}
			//            if (StringUtils.isBlank(instanceId)) {
			//                instanceId = System.getenv("HOST_ID");
			//            }
			//            if (StringUtils.isBlank(instanceId)) {
			//                System.err.println("if -instanceId is not supplied, ENV{'HOST_ID'} must be set");
			//                System.exit(-1);
			//            }
			//            if (instanceId.length() != 3) {
			//                System.err.println("-instanceId must be three characters. examples: i06, i12, n01");
			//                System.exit(-1);
			//            }

			if (null == priceStringFormatter && currencyLocale != null) {
				priceStringFormatter = NumberFormat.getInstance(currencyLocale);
			}

			//TODO: reexamine slow/fast
			String sOrF = "s";
			String uOrBlank = sfu ? "u" : "";
			String importerId = String.format("%s%s%s%s", instanceId, timestamp, sOrF, uOrBlank);

			if (xmlDocumentOutput == null) {
				driver = ProductImportDriver2.newInstance(importerId, null, sfu, sfu,
						dontRetireMissingProducts);
			}

			String filePath = String.format("/feedonly-import/%s/%s.foi", key, key);
			try {
				if (xmlDocumentOutput == null) {
					log.info("Starting up driver service");
					driver.startupService(filePath);
				}

				log.info(String.format("Begin processing %s", feed));

				if (productDelimiter != null) {
					parseTextFeed(feed, productDelimiter, key, skipLines);
				}
				else {
					parseXMLFeed(feed, productTagName, key);
				}

				log.info(String.format("Finished processing %s", feed));
				if (xmlDocumentOutput != null) {
					// write the content into xml file
					TransformerFactory transformerFactory = TransformerFactory.newInstance();
					Transformer transformer = transformerFactory.newTransformer();
					DOMSource source = new DOMSource(xmlDocumentOutput);
					StreamResult result = new StreamResult(new File(outputXMLLocation));

					// Output to console for testing
					// StreamResult result = new StreamResult(System.out);
					transformer.setOutputProperty(OutputKeys.METHOD, "xml");
					transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS,
							NutchConstants.description);

					transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
					transformer.setOutputProperty(OutputKeys.INDENT, "yes");
					transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

					transformer.transform(source, result);
					log.info(String.format("Product infos written out to %s", outputXMLLocation));
				}
				else {
					driver.postProcessService();
				}
			}
			finally {
				if (xmlDocumentOutput == null) {
					log.info("Shutting down driver service");
					driver.shutdownService();
				}
			}
		}
		catch (Exception e) {
			log.error("Uncaught exception: ", e);
			System.exit(-1);
		}
		System.exit(0);
	}

	public interface RetailerFeed
	{
		public RetailerProduct nextProduct() throws IOException;
	}
}
