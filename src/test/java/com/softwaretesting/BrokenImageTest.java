
package com.softwaretesting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class BrokenImageTest {

    private WebDriver driver;

    @BeforeClass
    public void setUp() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();

        driver.get(
            "https://the-internet.herokuapp.com/broken_images"
        );

        // Wait for the webpage to load.
        new WebDriverWait(driver, Duration.ofSeconds(20))
            .until(d -> "complete".equals(
                ((JavascriptExecutor) d)
                    .executeScript("return document.readyState")
            ));
    }

    @Test(priority = 1)
    public void verifyPageTitle() {
        Assert.assertEquals(
            driver.getTitle(),
            "The Internet",
            "Page title does not match"
        );

        System.out.println("Page title verified.");
    }

    @Test(priority = 2)
    public void verifyImagesExist() {
        Long count = (Long) ((JavascriptExecutor) driver)
            .executeScript("return document.images.length");

        System.out.println("Total images found: " + count);

        Assert.assertTrue(
            count > 0,
            "No images were found on the page"
        );
    }

    @Test(priority = 3)
    public void detectBrokenImages() {

        // Wait for images to finish loading or fail.
        // If the wait times out, still inspect their current status.
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(d -> (Boolean) ((JavascriptExecutor) d)
                    .executeScript(
                        "return Array.from(document.images)"
                        + ".every(img => img.complete)"
                    ));
        } catch (TimeoutException e) {
            System.out.println(
                "Warning: Some images did not settle in time."
            );
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> images =
            (List<Map<String, Object>>)
            ((JavascriptExecutor) driver).executeScript(
                "return Array.from(document.images).map(img => ({"
                + "src: img.currentSrc || img.src,"
                + "complete: img.complete,"
                + "width: img.naturalWidth"
                + "}));"
            );

        int broken = 0;
        int valid = 0;

        System.out.println("----- IMAGE CHECK RESULTS -----");

        for (Map<String, Object> image : images) {
            String src = String.valueOf(image.get("src"));

            boolean complete =
                Boolean.TRUE.equals(image.get("complete"));

            int width =
                ((Number) image.get("width")).intValue();

            if (complete && width > 0) {
                valid++;
                System.out.println("VALID: " + src);
            } else {
                broken++;
                System.out.println("BROKEN: " + src);
            }
        }

        System.out.println("-------------------------------");
        System.out.println("Total images: " + images.size());
        System.out.println("Broken images: " + broken);
        System.out.println("Valid images: " + valid);

        // Do not fail just because the demo website
        // intentionally contains broken images.
        Assert.assertTrue(
            !images.isEmpty(),
            "No images were available to inspect"
        );
    }

    @Test(priority = 4)
    public void captureScreenshot() throws IOException {
        Path screenshotDir =
            Paths.get("target", "screenshots");

        Files.createDirectories(screenshotDir);

        File screenshot = ((TakesScreenshot) driver)
            .getScreenshotAs(OutputType.FILE);

        Path destination =
            screenshotDir.resolve("broken-images-page.png");

        Files.copy(
            screenshot.toPath(),
            destination,
            StandardCopyOption.REPLACE_EXISTING
        );

        System.out.println(
            "Screenshot saved: "
            + destination.toAbsolutePath()
        );
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
