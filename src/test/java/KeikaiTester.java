import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

/**
 * a helper class for browser testing
 */
public class KeikaiTester {


	static WebElement getEditor(WebDriver driver) {
		return driver.findElement(By.cssSelector(".keditor"));
	}

	/**
	 * a canvas whose ID is [container_id]_inner_main
	 * @param driver
	 * @return
	 */
	static WebElement getKeikaiMain(WebDriver driver) {
		return driver.findElement(By.xpath("//canvas[1]"));
	}

	static void waitKeikaiLoaded(WebDriverWait webDriverWait) {
		webDriverWait.until(new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver driver) {
				return driver.findElement(By.cssSelector(".keikai-inner")) != null;
			}
		});
	}

	public static boolean compareImages(BufferedImage imgA, BufferedImage imgB) {
		// The images must be the same size.
		if (imgA.getWidth() == imgB.getWidth() && imgA.getHeight() == imgB.getHeight()) {
			int width = imgA.getWidth();
			int height = imgA.getHeight();

			// Loop over every pixel.
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					// Compare the pixels for equality.
					if (imgA.getRGB(x, y) != imgB.getRGB(x, y)) {
						return false;
					}
				}
			}
		} else {
			return false;
		}
		return true;
	}

	public static BufferedImage generateCanvasImage(WebDriver driver, String id) throws IOException {
		String dataUrl = ((JavascriptExecutor)driver).executeScript("return jq('#"+id+"')[0].toDataURL()").toString();
		byte[] imagedata = DatatypeConverter.parseBase64Binary(dataUrl.substring(dataUrl.indexOf(",") + 1));
		return ImageIO.read(new ByteArrayInputStream(imagedata));
	}
}
