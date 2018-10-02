package com.example.demo.arcgis;


import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import java.util.concurrent.TimeUnit;

public class ArcgisPerformanceTest {
    

    public static void main(String[] args) throws Exception {

        ArcgisPerformanceTest test = new ArcgisPerformanceTest();
        String driverType="chrome";

//        //1.本地测试，方便初期验证测试方法是否正确.
//        for(int i=0;i<5;i++) {
//            new Thread(()-> test.localTask(driverType)).start();
//            test.waitFor(100);
//        }
//        test.waitFor(1000*60*60*24);

        //2.selenium-grid 远程测试,用于大规模的压力测试,模拟真实用户访问.
        int thread_number=180;  //
        for(int i=1;i<=thread_number;i++){
            new Thread(()-> test.remoteTask(driverType)).start();
            test.waitFor(100);
        }
    }


    public void remoteTask(String driverType){

        String seleniumGridHubURL="http://11.23.3.114:4444/wd/hub";
        try {
            WebDriver webDriver = getRemoteDriver(driverType, seleniumGridHubURL);
            String entryURL = "http://11.23.3.114:8777/index.html";
            arcgisTest(webDriver, entryURL);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void localTask(String driverType){
        try {
            WebDriver webDriver = getLocalDriver(driverType);//getDriver();
            String entryURL = "http://11.23.3.114:8777/index.html";
            arcgisTest(webDriver, entryURL);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    /**
     * 远程版
     * @throws Exception
     */
    public void remoteDriverTest() throws Exception {
        DesiredCapabilities firefoxDesiredcap = DesiredCapabilities.firefox();
        DesiredCapabilities chromeDesiredcap = DesiredCapabilities.chrome();
        //DesiredCapabilities ieDesiredcap = DesiredCapabilities.internetExplorer();

        //DesiredCapabilities desiredCapabilities = firefoxDesiredcap;
        DesiredCapabilities desiredCapabilities = chromeDesiredcap;
        //String seleniumGridHubURL="http://192.168.102.6:4444/wd/hub";
        String seleniumGridHubURL="http://11.23.3.114:4444/wd/hub";
        WebDriver webDriver = new RemoteWebDriver(new URL(seleniumGridHubURL), desiredCapabilities);
        webDriverTest(webDriver);
    }

    /**
     * 远程版
     * @throws Exception
     */
    public WebDriver getRemoteDriver(String driverType,String seleniumGridHubURL) throws Exception {

        DesiredCapabilities desiredCapabilities = null;
        if("firefox".equals(driverType.toLowerCase())){
            desiredCapabilities = DesiredCapabilities.firefox();
        }
        else if("chrome".equals(driverType.toLowerCase())){
            desiredCapabilities = DesiredCapabilities.chrome();
        }
        else if("ie".equals(driverType.toLowerCase())){
            desiredCapabilities = DesiredCapabilities.internetExplorer();
        }
        else {
            throw new UnsupportedOperationException("尚未支持的类型....");
        }
        System.out.println("driverType="+driverType+" desiredCapabilities="+desiredCapabilities);

        WebDriver webDriver = new RemoteWebDriver(new URL(seleniumGridHubURL), desiredCapabilities);
        return webDriver;
    }

    public void localWebDriverTest(){
        WebDriver driver = getLocalDriver("chrome");//getDriver();
        webDriverTest(driver);
    }

    //http://11.23.3.114:8777/
    public void arcgisTest(WebDriver webDriver, String entryURL) {

        try {
            try {
                webDriver.manage().window().maximize();
            }catch(Exception e){
                e.printStackTrace();
            }
            webDriver.manage().timeouts().implicitlyWait(8, TimeUnit.SECONDS);

            //driver.manage().timeouts().pageLoadTimeout(20,TimeUnit.SECONDS);
            //driver.manage().timeouts().setScriptTimeout(20,TimeUnit.SECONDS);
            webDriver.get(entryURL);
            Thread.sleep(4000);
            System.out.println(entryURL+" 加载成功.");
            long step = 100000+System.nanoTime()%100000;

            randomMove(webDriver,step);

            webDriver.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        if (webDriver != null) {
            System.out.println("driver!=null");
        } else {
            System.out.println("driver==null");
        }
    }

    private void randomMove(WebDriver webDriver,long step){

        WebElement plusButton = null;
        try {
            plusButton = webDriver.findElement(By.xpath("//div[contains(@class,'esriSimpleSliderIncrementButton')]"));
        }catch(Exception e){
            e.printStackTrace();
        }
        //WebElement minusButton = webDriver.findElement(By.xpath("//div[contains(@class,'esriSimpleSliderDecrementButton')]"));

        for(int i=0;i<step;i++) {
            //119.30239300, latitude=26.06971100 福州茶亭公园

            //119.30033306347656,26.06078460839844
            double fuzhouCenterLongitude=119.30239300;
            double fuzhouCenterLatitude=26.06971100;

            double longitude=(fuzhouCenterLongitude-0.5)+0.5*Math.random();
            double latitude=(fuzhouCenterLatitude-0.2)+0.5*Math.random();
            int zoomLevel=1+(int)(Math.random()*8);
            String randomMoveJavascript = "m_randomMove("+longitude+","+latitude+","+zoomLevel+");";

            waitFor(1000);
            if (webDriver instanceof JavascriptExecutor) {
                try {
                    ((JavascriptExecutor) webDriver).executeScript(randomMoveJavascript);
                }catch(Exception e){
                    e.printStackTrace();
                }
            } else {
                throw new IllegalStateException("This driver does not support JavaScript!");
            }

            int maxZoomLevel=12;
            int k=1+(int)(Math.random()*maxZoomLevel);
            if(plusButton!=null) {
                for (int t = 0; t < k; t++) {
                    plusButton.click();
                    waitFor(100);
                }
            }
            long timeWaitForInMilliseconds=3000+(long)(Math.random()*2000);
            System.out.println("将等待"+timeWaitForInMilliseconds+"毫秒. longitude="+longitude+"  latitude="+latitude);
            waitFor(timeWaitForInMilliseconds);
        }
    }
    
    public void webDriverTest(WebDriver driver) {

        try {
            try {
                driver.manage().window().maximize();
            }catch(Exception e){
                e.printStackTrace();
            }
            driver.manage().timeouts().implicitlyWait(8, TimeUnit.SECONDS);

            //driver.manage().timeouts().pageLoadTimeout(20,TimeUnit.SECONDS);
            //driver.manage().timeouts().setScriptTimeout(20,TimeUnit.SECONDS);
            driver.get("http://ris.szpl.gov.cn/bol/index.aspx");

            do {
                WebElement nextPageLink = driver.findElement(By.partialLinkText("[下一页]"));
                WebElement lastPageLink = driver.findElement(By.partialLinkText("[末页]"));
                if (nextPageLink != null) {
                    System.out.println(nextPageLink.getText());
                    System.out.println(driver.getPageSource());
                    nextPageLink.click();
                    Thread.sleep(3000);
                } else {
                    System.out.println("找不到下一页.");
                    break;
                }

                // lastPageLink.
            }
            while (true);
            //driver.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        if (driver != null) {
            System.out.println("driver!=null");
        } else {
            System.out.println("driver==null");
        }
    }
    
    public void waitFor(long timeInMilliseconds) {
        try {
            Thread.sleep(timeInMilliseconds);
        } catch (InterruptedException e) {
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
        return driver;
    }
}

