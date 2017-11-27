import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.junit.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;


public class CanvasTest {
	static private String SERVER_URL = "http://localhost:8080/java-client-demo";
	static private WebDriver driver;
	static WebDriverWait webDriverWait;
	static private File SCREENSHOT_FOLDER = new File("./target/screenshot/"); //screenshot output folder

	@BeforeClass
	static public void init(){
		System.setProperty("webdriver.chrome.driver", "/Applications/chromedriver");
		driver = new ChromeDriver(); //keikai only responds to chrome driver, HtmlUnitDriver doesn't work
		driver.manage().window().setSize(new Dimension(1200, 1000));
		//wait for a short delay for each action, e.g. editor appears after a double-clicking
		driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);

		webDriverWait = new WebDriverWait(driver, 5);
		prepareOutputFolder();
	}

	private static void prepareOutputFolder() {
		if (SCREENSHOT_FOLDER.exists()){
			if (!SCREENSHOT_FOLDER.isDirectory()){
				SCREENSHOT_FOLDER.delete();
				SCREENSHOT_FOLDER.mkdir();
			}
		}
	}

	@AfterClass
	static public void end(){
		driver.quit();
	}
	
	/**
	 * test very basic function. Make sure keikai is working. 
	 */
	@Test
	public void smokeTest() throws IOException {
		driver.get(SERVER_URL+"/zk/index.zul");
		KeikaiTester.waitKeikaiLoaded(webDriverWait);
		WebElement keikai = KeikaiTester.getKeikaiMain(driver);
		Actions action = new Actions(driver);
		action.moveToElement(keikai,40, 30).doubleClick().build().perform();
		WebElement editor = KeikaiTester.getEditor(driver);
		input(editor,"test");
		//take a screenshot
//		File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
//		FileUtils.copyFile(scrFile, new File("./screenshot.png"));
		pauseInSecond(2);
	}

	@Test
	public void loadFile() throws IOException{
		driver.get(SERVER_URL+"/zk/index.zul");
		KeikaiTester.waitKeikaiLoaded(webDriverWait);
		driver.findElement(By.className("z-icon-file")).click(); //File
		driver.findElement(By.className("z-icon-list")).click(); //load
		String fileName = "template.xlsx";
		driver.findElement(By.name(fileName)).click();
		pauseInSecond(1); //wait for importing
		
		String id = KeikaiTester.getKeikaiMain(driver).getAttribute("id");
		BufferedImage bufferedImage = KeikaiTester.generateCanvasImage(driver,id);
		
		BufferedImage expectedImage = ImageIO.read(new File("./answer/template.xlsx.png"));
		if (!KeikaiTester.compareImages(expectedImage, bufferedImage)){
			ImageIO.write(bufferedImage, "png", new File(SCREENSHOT_FOLDER, fileName+".png"));
			Assert.assertTrue("", false);
		}
	}

	private void pauseInSecond(int seconds){
		try {
			TimeUnit.SECONDS.sleep(seconds);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void input(WebElement editor, String text){
		editor.sendKeys(text);
		editor.sendKeys(Keys.ENTER);
	}

}
