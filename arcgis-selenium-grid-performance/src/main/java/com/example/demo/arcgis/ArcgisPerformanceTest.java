package com.example.demo.arcgis;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class ArcgisPerformanceTest {

    private Capabilities chromeCapabilities = DesiredCapabilities.chrome();
    private Capabilities firefoxCapabilities = DesiredCapabilities.firefox();

    private Map<String, Integer> explorerInstanceCountMap = new HashMap<>();
    private boolean shouldTakeSnapShot = false;
    private String seleniumHubServerURL = "http://11.23.3.114:4444/wd/hub";
    private String entryURL = "http://11.23.3.117/map.html";
    private File imageDir = new File("/tmp/images");
    private String logFilename;
    private Set<String> webClientTypeSet = new HashSet<>();
    private AtomicLong totalTestCount;
    private AtomicLong remainTestCount;
    private AtomicLong maxWaitTimeInMilliseconds;
    private int testNumberPerExplorerLifeTime=10; //每个webdriver 测试多个网页后自动关闭.

    private long startTestTime;

    private Properties properties;

    private BlockingQueue<String> logQueue;

    private StringBuffer logBuf;



    public ArcgisPerformanceTest(String... args) {

        properties = new Properties();
        try {
            String arcgisConfigFilePath = "arcgis.conf";
            if (args.length > 0) {
                arcgisConfigFilePath = args[0];
            }

            String arcgisConfFileFullPath = FileUtil.getAbsolutePath(arcgisConfigFilePath);
            System.out.println(arcgisConfFileFullPath);
            FileInputStream fileInputStream = new FileInputStream(arcgisConfFileFullPath);
            properties.load(fileInputStream);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String... args) throws Exception {

        ArcgisPerformanceTest test = new ArcgisPerformanceTest(args);
        test.run();
    }


    public void init() {

        logBuf = new StringBuffer();
        logQueue =new LinkedBlockingQueue<>();

        if(properties.getProperty("log_file_name")!=null){
            logFilename = FileUtil.getAbsolutePath(properties.getProperty("log_file_name"));
        }

        if (properties.getProperty("max_wait_time_in_milliseconds") != null) {
            maxWaitTimeInMilliseconds = new AtomicLong(Long.parseLong(properties.getProperty("max_wait_time_in_milliseconds")));
        }
        if (properties.getProperty("selenium_hub_server_url") != null) {
            seleniumHubServerURL = properties.getProperty("selenium_hub_server_url");
        }

        if (properties.getProperty("entry_url") != null) {
            entryURL = properties.getProperty("entry_url");
        }

        if (properties.get("total_test_count") != null) {
            totalTestCount  = new AtomicLong(Long.parseLong(properties.getProperty("total_test_count")));
            remainTestCount = new AtomicLong(totalTestCount.longValue());
        }

        if (properties.getProperty("image_dir") != null) {
            imageDir = new File(properties.getProperty("image_dir"));
        }

        if (properties.getProperty("web_client_type") != null) {
            String webCientType = properties.getProperty("web_client_type");
            webClientTypeSet.addAll(Arrays.asList(webCientType.split(",")));
        }

        if (properties.getProperty("take_snapshot") != null) {
            shouldTakeSnapShot = Boolean.parseBoolean(properties.getProperty("take_snapshot"));
        }

        int chromeInstanceNumber = 0;
        int firefoxInstanceNumber = 0;

        if (properties.getProperty("chrome_instance_number") != null) {
            chromeInstanceNumber = Integer.parseInt(properties.getProperty("chrome_instance_number"));
        }

        if (properties.getProperty("firefox_instance_number") != null) {
            firefoxInstanceNumber = Integer.parseInt(properties.getProperty("firefox_instance_number"));
        }
        explorerInstanceCountMap.put("chrome", chromeInstanceNumber);
        explorerInstanceCountMap.put("firefox", firefoxInstanceNumber);

        this.testNumberPerExplorerLifeTime= Integer.parseInt(properties.getProperty("test_number_per_explorer_life_time"));

        logInfo(properties.toString());

    }

    private long getCurrentTestSpeedPerSecond(){
        long timeElpasedInSeconds=(System.currentTimeMillis()-this.startTestTime)/1000;
        if(timeElpasedInSeconds<=0){
            timeElpasedInSeconds=1;
        }
        long finishedTestCount = totalTestCount.get()-remainTestCount.get();

        Double speed=new Double(finishedTestCount/timeElpasedInSeconds);
        return speed.longValue();
    }

    /**
     * selenium_hub_server_url=http://11.23.3.114:4444/wd/hub
     * entry_url=http://11.23.3.117/map.html
     * image_dir=/tmp/images
     */

    public void run() {
        init();
//        for(int i=0;i<1;i++) {
//            new Thread(()->localTask("chrome",entryURL,imageDir)).start();
//            waitFor(1000);
//        }

        new Thread(()-> saveLogToDisk()).start();


        if (webClientTypeSet.contains("chrome")) {
            int chromeInstanceNumber = explorerInstanceCountMap.get("chrome");
            for (int i = 0; i < chromeInstanceNumber; i++) {
                new Thread(remoteTask(seleniumHubServerURL, chromeCapabilities, entryURL, imageDir)).start();
                waitFor(10);
            }
        }

        if (webClientTypeSet.contains("firefox")) {
            int firefoxInstanceNumber = explorerInstanceCountMap.get("firefox");
            for (int i = 0; i < firefoxInstanceNumber; i++) {
                new Thread(remoteTask(seleniumHubServerURL, firefoxCapabilities, entryURL, imageDir)).start();
                waitFor(10);
            }
        }

        this.startTestTime=System.currentTimeMillis();

        long waitStepInMilliseconds = 1000 * 10;
        while (maxWaitTimeInMilliseconds.get() > 0 && remainTestCount.get()>0) {
            waitFor(waitStepInMilliseconds);
            maxWaitTimeInMilliseconds.set(maxWaitTimeInMilliseconds.get()-waitStepInMilliseconds);
            long leftTimeInSeconds = maxWaitTimeInMilliseconds.get() / 1000;
            logInfo("离测试终止时间还剩下:" + leftTimeInSeconds + "秒,剩余" + remainTestCount.get() + "次测试待执行.\n");
        }

    }

    private void saveLogToDisk(){
        try {
            FileOutputStream fout = new FileOutputStream(logFilename);
            while(remainTestCount.get()>0) {
                String log = logQueue.take();
                fout.write(log.getBytes());
            }
            fout.flush();
            fout.close();
            System.out.println("日志已经保存完毕,日志队列退出.");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void logInfo(String info){
        try {
            //logQueue.put(info);
            //logQueue.add(info);
            //System.out.println(info);
            logBuf.append(info);

            if(logBuf.length()>1024*10){
                System.out.println(logBuf.toString());
                logBuf.delete(0,logBuf.length()-1);
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    Runnable remoteTask(final String hubServerURL, final Capabilities capabilities, String entryURL, File imageDir) {

        Runnable runnable = () -> {
                while(remainTestCount.get()>0 && maxWaitTimeInMilliseconds.get() > 0) {
                    long step = testNumberPerExplorerLifeTime;
                    if(remainTestCount.get()<step){
                        step = remainTestCount.get();
                    }

                    URL url = null;
                    try{
                        url = new URL(hubServerURL);
                    }catch(MalformedURLException e){
                        e.printStackTrace();
                    }
                    RemoteWebDriver webDriver = new RemoteWebDriver(url, capabilities);
                    webDriver.get(entryURL);
                    logInfo(webDriver.getTitle()+" 本次浏览器将执行"+step+"次测试.\n"); //System.out.println(webDriver.getPageSource());
                    arcgisTest(webDriver, entryURL, imageDir, step);
                    if(webDriver!=null) {
                        webDriver.quit();
                    }
                    logInfo(" 本次浏览器已经执行"+step+"次测试完毕,浏览器退出."+" 已完成测试"+(totalTestCount.get()-remainTestCount.get())+"次. 测试总共还剩余"+ remainTestCount.get()+"次测试待完成. 当前测试速度"+getCurrentTestSpeedPerSecond()+"次/秒.\n");
                }
        };
        return runnable;
    }

    //http://11.23.3.114:8777/
    public void arcgisTest(WebDriver webDriver, String entryURL, File imageDir,long step) {

        try {
            webDriver.manage().timeouts().implicitlyWait(8, TimeUnit.SECONDS);
            webDriver.get(entryURL);
            logInfo(entryURL + " 加载成功. title=" + webDriver.getTitle()+". 剩下待测试" + remainTestCount.get() + "次.\n");

            try {
                ((JavascriptExecutor) webDriver).executeScript("window.resizeTo(1200, 1500);");
            } catch (Exception e) {
                e.printStackTrace();
            }

            randomMove(webDriver, step, imageDir);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        if (webDriver != null) {
            webDriver.close();
        } else {
            System.err.println("driver==null");
        }
    }

    private void randomMove(WebDriver webDriver, long step, File image_folder) {

        //等待某个元素的出现,最多等待10秒
        WebDriverWait webDriverWait = new WebDriverWait(webDriver, 10);
        WebElement plusButton = null;
        try {
            plusButton = webDriverWait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[contains(@class,'esriSimpleSliderIncrementButton')]")));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //WebElement minusButton = webDriver.findElement(By.xpath("//div[contains(@class,'esriSimpleSliderDecrementButton')]"));

        for (int i = 0; i < step; i++) {
            //119.30239300, latitude=26.06971100 福州茶亭公园

            //119.30033306347656,26.06078460839844
            double fuzhouCenterLongitude = 119.30239300;
            double fuzhouCenterLatitude = 26.06971100;

            double longitude = (fuzhouCenterLongitude - 0.5) + 0.5 * Math.random();
            double latitude = (fuzhouCenterLatitude - 0.2) + 0.5 * Math.random();
            int zoomLevel = 1 + (int) (Math.random() * 8);
            String randomMoveJavascript = "m_randomMove(" + longitude + "," + latitude + "," + zoomLevel + ");";

            //waitFor(1000);
            if (webDriver instanceof JavascriptExecutor) {
                try {
                    ((JavascriptExecutor) webDriver).executeScript(randomMoveJavascript);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                throw new IllegalStateException("This driver does not support JavaScript!");
            }

            int maxZoomLevel = 12;
            int k = 1 + (int) (Math.random() * maxZoomLevel);
            if (plusButton != null) {
                for (int t = 0; t < k; t++) {
                    plusButton.click();
                    waitFor(100);
                }
            }
            remainTestCount.decrementAndGet();

            logInfo("正在打开地图浏览 longitude=" + longitude + "  latitude=" + latitude + " 本浏览器剩余待测试次数=" + (step-i)+"次,后将关闭浏览器.\n");

            if (shouldTakeSnapShot) {
                try {
                    String filename = image_folder.getCanonicalPath() + "/longitude_" + longitude + "_latitude_" + latitude + "_time_" + System.currentTimeMillis() + ".png";
                    takeSnapShot(webDriver, filename);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }


    public void takeSnapShot(WebDriver webdriver, String fileWithPath) throws Exception {

        logInfo("准备给浏览器拍照...\n");
        //Convert web driver object to TakeScreenshot

        TakesScreenshot scrShot = ((TakesScreenshot) webdriver);

        //Call getScreenshotAs method to create image file

        File srcFile = scrShot.getScreenshotAs(OutputType.FILE);

        logInfo("浏览器拍照成功.srcFile=" + srcFile+"\n");

        //Move image file to new destination

        File destFile = new File(fileWithPath);

        //Copy file at destination

        FileUtils.copyFile(srcFile, destFile);

        logInfo("浏览器拍照文件:" + destFile + "保存成功...\n");

    }


    public void waitFor(long timeInMilliseconds) {
        try {
            Thread.sleep(timeInMilliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void localTask(String driverType, String entryURL, File imageDir,long testCount) {
        try {
            WebDriver webDriver = getLocalDriver(driverType);//getDriver();
            arcgisTest(webDriver, entryURL, imageDir,testCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public WebDriver getLocalDriver(String name) {
        if ("firefox".equals(name.toLowerCase())) {
            return getLocalFireFoxDriver();
        }

        if ("chrome".equals(name.toLowerCase())) {
            return getLocalChromeDriver();
        }

        return null;
    }

    public WebDriver getLocalFireFoxDriver() {
        // declaration and instantiation of objects/variables
        System.setProperty("webdriver.firefox.marionette", "/Users/happy/server/selenium/firefox/mac/0.21.0/geckodriver");
        WebDriver driver = new FirefoxDriver();
        return driver;
    }

    public WebDriver getLocalChromeDriver() {
        System.setProperty("webdriver.chrome.driver", "/Users/happy/server/selenium/chrome/mac/2.40/chromedriver");
        WebDriver driver = new ChromeDriver();
        //((ChromeDriver) driver).setLogLevel(Level.ALL);
        return driver;
    }

}