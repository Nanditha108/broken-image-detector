
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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class BrokenImageTest {

    private WebDriver driver;

    @BeforeClass
    public void setUp() throws Exception {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();

        // Open the HTML file from src/test/resources.
        java.net.URL page = getClass()
                .getClassLoader()
                .getResource("test-page.html");

        Assert.assertNotNull(
                page,
                "test-page.html was not found in src/test/resources"
        );

        driver.get(page.toExternalForm());

        new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(d -> "complete".equals(
                        ((JavascriptExecutor) d)
                                .executeScript("return document.readyState")
                ));

        System.out.println("Local test page opened successfully.");
        System.out.println("Browser URL: " + driver.getCurrentUrl());
        System.out.println("Browser title: " + driver.getTitle());
    }

    @Test(priority = 1)
    public void verifyPageTitle() {
        Assert.assertEquals(
                driver.getTitle(),
                "Broken Image Detector Demo",
                "Page title does not match"
        );

        System.out.println("Page title verified.");
    }

    @Test(priority = 2)
    public void verifyImagesExist() {
        Long count = (Long) ((JavascriptExecutor) driver)
                .executeScript("return document.images.length");

        System.out.println("Total images found: " + count);

        Assert.assertEquals(
                count.longValue(),
                4L,
                "Expected exactly four images on the test page"
        );
    }

    @Test(priority = 3)
    public void detectBrokenImages() {
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Wait until all four images have either loaded or failed.
        new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(d -> (Boolean) ((JavascriptExecutor) d)
                        .executeScript(
                                "return Array.from(document.images)"
                                + ".every(img => img.complete)"
                        ));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> images =
                (List<Map<String, Object>>) js.executeScript(
                        "return Array.from(document.images).map(img => ({"
                        + "src: img.currentSrc || img.src,"
                        + "alt: img.alt,"
                        + "complete: img.complete,"
                        + "width: img.naturalWidth"
                        + "}));"
                );

        int broken = 0;
        int valid = 0;
        int unfinished = 0;

        System.out.println();
        System.out.println("===== IMAGE CHECK RESULTS =====");

        for (Map<String, Object> image : images) {
            String src = String.valueOf(image.get("src"));
            String alt = String.valueOf(image.get("alt"));

            boolean complete =
                    Boolean.TRUE.equals(image.get("complete"));

            int width =
                    ((Number) image.get("width")).intValue();

            System.out.println(
                    "CHECK: " + alt
                    + " | URL: " + src
                    + " | complete=" + complete
                    + " | naturalWidth=" + width
            );

            if (!complete) {
                unfinished++;
                System.out.println("NOT FINISHED: " + alt);
            } else if (width > 0) {
                valid++;
                System.out.println("VALID: " + alt);
            } else {
                broken++;
                System.out.println("BROKEN: " + alt);
            }
        }

        System.out.println("-------------------------------");
        System.out.println("Total images: " + images.size());
        System.out.println("Broken images: " + broken);
        System.out.println("Valid images: " + valid);
        System.out.println("Unfinished images: " + unfinished);
        System.out.println("===============================");

        // Fail the test if the controlled page does not give
        // the expected two valid and two broken images.
        Assert.assertEquals(
                unfinished, 0,
                "Some images did not finish loading"
        );
        Assert.assertEquals(
                images.size(), 4,
                "Expected four images"
        );
        Assert.assertEquals(
                valid, 2,
                "Expected two valid images"
        );
        Assert.assertEquals(
                broken, 2,
                "Expected two broken images"
        );
    }

    @Test(priority = 4)
    public void captureScreenshot() throws IOException {
        Path screenshotDir = Paths.get("target", "screenshots");
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
                "Screenshot saved: " + destination.toAbsolutePath()
        );
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }

        System.out.println("Browser closed successfully.");
    }
}
