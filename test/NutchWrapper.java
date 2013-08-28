package com.shopstyle.nutchx;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
import org.apache.nutch.crawl.Crawl;
import org.apache.nutch.util.NutchConfiguration;

import com.shopstyle.importer.ProductImportDriver2;
import com.shopstyle.importer.ProductImportDriverService;
import com.shopstyle.util.FileUtil;

public final class NutchWrapper
{
    private static final Logger log = Logger.getLogger(NutchWrapper.class);

    //use a handoff object, so that our singleton instance can be populated with arg data received by the main method
    private static ProductImportDriverService DriverHandoff;

    public enum SingletonHolder {
        Instance;
        private final ProductImportDriverService driver;
        private final ExecutorService executorService;
        SingletonHolder() {
            driver = DriverHandoff;
            DriverHandoff = null;
            executorService = Executors.newSingleThreadExecutor();
        }
        public ProductImportDriverService getDriver() {
            return driver;
        }
        public ExecutorService getExecutorService() {
            return executorService;
        }
    }

    private static void notEnoughArgs(String arg)
    {
        System.err.println(String.format("Could not read value of %s: not enough arguments", arg));
    }

    /*
     * For usage, see scripts/invokeNutch
     */
    public static void main(String[] args)
    {
        //grab timestamp
        SimpleDateFormat format = new SimpleDateFormat("MMddHHmmss");
        String timestamp = format.format(new Date());
        //BasicConfigurator.configure();  //log4j config

        try {

            String nutchHome = System.getenv("NUTCH_HOME");
            if (nutchHome == null) {
                log.error("Could not run NutchWrapper: NUTCH_HOME not set");
                System.exit(-1);
            }

            String feedName = null;
            String instanceId = null;
            String retailerHandle = null;
            String[] stripText = null;
            String stripParamsAsString = null;
            String[] stripParams = null;
            boolean productUrlIsNotDirectUrl = false;
            int topN = Integer.MIN_VALUE;
            String urlsDirPath = null;
            String crawlDirPath = nutchHome + "/crawl"; //default value
            int depth = 1;                              //default value
            int threads = 1;                              //default value
            String additionalNutchArgs = "";
            boolean sfu = false;
            boolean dontRetireMissingProducts = false;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("-feedName")) {
                    i++;
                    if (i >= args.length) {
                        notEnoughArgs(arg);
                        System.exit(-1);
                    }
                    feedName = args[i];
                }
                else if (arg.equals("-instanceId")) {
                    i++;
                    if (i >= args.length) {
                        notEnoughArgs(arg);
                        System.exit(-1);
                    }
                    instanceId = args[i];
                }
                else if (arg.equals("-retailer")) {
                    i++;
                    if (i >= args.length) {
                        notEnoughArgs(arg);
                        System.exit(-1);
                    }
                    retailerHandle = args[i];
                }
                else if (arg.equals("-stripText")) {
                    i++;
                    if (i >= args.length) {
                        notEnoughArgs(arg);
                        System.exit(-1);
                    }
                    String stripTextAsString = args[i];
                    stripText = stripTextAsString.split("\\s*\\|\\s*");
                }
                else if (arg.equals("-stripParams")) {
                    i++;
                    if (i >= args.length) {
                        notEnoughArgs(arg);
                        System.exit(-1);
                    }
                    stripParamsAsString = args[i];
                    stripParams = stripParamsAsString.split("\\s*\\|\\s*");
                    if (stripParams.length == 0) {
                        System.err.println("invalid -stripParams: " + stripParamsAsString);
                        System.exit(-1);
                    }
                }
                else if (arg.equals("-productUrlIsNotDirectUrl")) {
                    if (i >= args.length) {
                        notEnoughArgs(arg);
                        System.exit(-1);
                    }
                    productUrlIsNotDirectUrl = true;
                }

                else if (arg.equals("-topN")) {
                    i++;
                    if (i >= args.length) {
                        notEnoughArgs(arg);
                        System.exit(-1);
                    }
                    String topNString = args[i];
                    try {
                        topN = Integer.parseInt(topNString);
                    }
                    catch (NumberFormatException nfe) {
                        System.err.println("invalid -topN: " + topNString);
                        System.exit(-1);
                    }
                }
                else if (arg.equals("-readUrlsFromDir")) {
                    i++;
                    if (i >= args.length) {
                        notEnoughArgs(arg);
                        System.exit(-1);
                    }
                    urlsDirPath = args[i];
                }
                else if (arg.equals("-crawlDir")) {
                    i++;
                    if (i >= args.length) {
                        notEnoughArgs(arg);
                        System.exit(-1);
                    }
                    crawlDirPath = args[i];
                }
                else if (arg.equals("-depth")) {
                    i++;
                    if (i >= args.length) {
                        notEnoughArgs(arg);
                        System.exit(-1);
                    }
                    String depthString = args[i];
                    try {
                        depth = Integer.parseInt(depthString);
                    }
                    catch (NumberFormatException nfe) {
                        System.err.println("invalid -depth: " + depthString);
                        System.exit(-1);
                    }
                }
                else if (arg.equals("-threads")) {
                    i++;
                    if (i >= args.length) {
                        notEnoughArgs(arg);
                        System.exit(-1);
                    }
                    String threadsString = args[i];
                    try {
                        threads = Integer.parseInt(threadsString);
                    }
                    catch (NumberFormatException nfe) {
                        System.err.println("invalid -depth: " + threadsString);
                        System.exit(-1);
                    }
                }
                else if (arg.equals("-additionalNutchArgs")) {
                    i++;
                    if (i >= args.length) {
                        notEnoughArgs(arg);
                        System.exit(-1);
                    }
                    additionalNutchArgs = args[i];
                }
                else if (arg.equals("-sfu")) {
                    sfu = true;
                }
                else if (arg.equals("-dontRetireMissingProducts")) {
                    dontRetireMissingProducts = true;
                }
                else {
                    System.err.println("Unknown arg: " + arg);
                    System.exit(-1);
                }
            }

            if (StringUtils.isBlank(feedName)) {
                System.err.println("-feedName was not supplied and is required");
                System.exit(-1);
            }

            if (StringUtils.isBlank(instanceId)) {
                instanceId = System.getenv("HOST_ID");
            }
            if (StringUtils.isBlank(instanceId)) {
                System.err.println("if -instanceId is not supplied, ENV{'HOST_ID'} must be set");
                System.exit(-1);
            }
            if (instanceId.length() != 3) {
                System.err.println("-instanceId must be three characters. examples: i06, i12, n01");
                System.exit(-1);
            }

            if (retailerHandle == null) {
                if (stripText != null || stripParams != null) {
                    System.err.println("-stripText and -stripParams may only be supplied if -retailer is supplied");
                    System.exit(-1);
                }
                if (topN == Integer.MIN_VALUE) {
                    System.err.println("-topN is required, unless -retailer is supplied");
                    System.exit(-1);
                }
                //use default urlsDirPath
                if (urlsDirPath == null) {
                    urlsDirPath = nutchHome + "/urls";
                }
            }
            else if (urlsDirPath != null) {
                //when retailer is supplied, we will use a tmpUrlsDir, so -readUrlsFromDir must not be supplied
                System.err.println("if -retailer is supplied, -readUrlsFromDir must not be supplied");
                System.exit(-1);
            }

            File tmpUrlsDir = null;        //temporary urls directory we'll delete at the end
            if (retailerHandle != null) {
                urlsDirPath = String.format("%s/urls-tmp/%s", nutchHome,
                    retailerHandle.replace(" ", "_"));
                tmpUrlsDir = new File(urlsDirPath);
                log.info(String
                    .format(
                        "Invoking FetchURLs with args {stripText:%s, stripParams:%s, retailerHandle:%s, tmpUrlsDir:%s,productUrlIsNotDirectUrl:%s}",
                        stripText, stripParamsAsString, retailerHandle, urlsDirPath,
                        productUrlIsNotDirectUrl));
                int urlsWritten = FetchURLs.queryAndWriteProductUrls(stripText, stripParams,
                    retailerHandle, tmpUrlsDir, productUrlIsNotDirectUrl, topN);
                if (urlsWritten == 0) {
                    log.error("FetchURLs wrote zero URLs...exiting");
                    System.exit(-1);
                }
                if (topN == Integer.MIN_VALUE) {
                    topN = urlsWritten;
                }
            }

            //TODO: reexamine slow/fast
            String sOrF = "s";
            String uOrBlank = sfu ? "u" : "";
            String importerId = String.format("%s%s%s%s", instanceId, timestamp, sOrF, uOrBlank);

            DriverHandoff = ProductImportDriver2.newInstance(importerId, null, sfu, sfu,
                dontRetireMissingProducts);
            //trigger construction of the singleton instance
            ProductImportDriverService driver = SingletonHolder.Instance.getDriver();

            String nutchArgs = String.format("%s|-dir|%s|-depth|%d|-threads|%d|-topN|%d|%s",
                urlsDirPath,
 crawlDirPath, depth, threads, topN, additionalNutchArgs);
            String[] nutchArgsAsArray = nutchArgs.split("\\s*\\|\\s*");
            String filePath = String.format("/nutch-import/%s/%s.nut", feedName, feedName);

            try {
                driver.startupService(filePath);

                log.info("Invoking nutch with args " + nutchArgs);
                Configuration conf = NutchConfiguration.create();
                int res = ToolRunner.run(conf, new Crawl(), nutchArgsAsArray);
                log.info("Nutch result code: " + res);

                ExecutorService executorService = SingletonHolder.Instance.getExecutorService();
                executorService.shutdown();
                executorService.awaitTermination(10, TimeUnit.HOURS);
                log.info("Nutch is finished working");
                driver.postProcessService();

                //if all has gone well so far, and tmpUrlsDir exists, delete it
                if (tmpUrlsDir != null && tmpUrlsDir.exists()) {
                    FileUtil.recursiveDelete(tmpUrlsDir);
                }
            }
            finally {
                log.info("Shutting down driver service");
                driver.shutdownService();
            }
        }
        catch (Exception e) {
            log.error("Uncaught exception: ", e);
            System.exit(-1);
        }
        System.exit(0);
    }
}
