
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
import java.util.logging.Level;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class BrokenImageTest {

    private WebDriver driver;

    @BeforeClass
    public void setUp() {
        ChromeOptions options = new ChromeOptions();

        // Do not use headless mode: Chrome should open visibly.
        options.addArguments("--window-size=1920,1080");

        // Enable Chrome performance logs for network diagnostics.
        LoggingPreferences logs = new LoggingPreferences();
        logs.enable(LogType.PERFORMANCE, Level.ALL);
        options.setCapability("goog:loggingPrefs", logs);

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();

        driver.get(
            "https://the-internet.herokuapp.com/broken_images"
        );

        new WebDriverWait(driver, Duration.ofSeconds(20))
            .until(d -> "complete".equals(
                ((JavascriptExecutor) d)
                    .executeScript("return document.readyState")
            ));

        System.out.println("Website opened successfully.");
        System.out.println("Browser URL: " + driver.getCurrentUrl());
        System.out.println("Browser title: " + driver.getTitle());
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
            "No images found on the webpage"
        );
    }

    @Test(priority = 3)
    public void detectBrokenImages() {
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Wait for images to finish loading or fail.
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(d -> (Boolean) js.executeScript(
                    "return Array.from(document.images)"
                    + ".every(img => img.complete)"
                ));
        } catch (TimeoutException e) {
            System.out.println(
                "Warning: Some images did not finish loading."
            );
        }

        // Diagnostic information for the green banner.
        Object bannerStatus = js.executeScript(
            "var img = Array.from(document.images).find("
            + "i => i.src.includes('forkme_right_green_007200.png'));"
            + "return img ? {src: img.src, complete: img.complete, "
            + "width: img.naturalWidth} : 'Image not found';"
        );

        System.out.println("Banner image status: " + bannerStatus);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> images =
            (List<Map<String, Object>>) js.executeScript(
                "return Array.from(document.images).map(img => ({"
                + "src: img.currentSrc || img.src,"
                + "complete: img.complete,"
                + "width: img.naturalWidth"
                + "}));"
            );

        int broken = 0;
        int valid = 0;
        int unfinished = 0;

        System.out.println("----- IMAGE CHECK RESULTS -----");

        for (Map<String, Object> image : images) {
            String src = String.valueOf(image.get("src"));

            boolean complete =
                Boolean.TRUE.equals(image.get("complete"));

            int width =
                ((Number) image.get("width")).intValue();

            System.out.println(
                "CHECK: " + src
                + " | complete=" + complete
                + " | naturalWidth=" + width
            );

            if (!complete) {
                unfinished++;
                System.out.println("NOT FINISHED: " + src);
            } else if (width > 0) {
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
        System.out.println("Unfinished images: " + unfinished);

        // Print performance events related to the banner request.
        System.out.println("----- BANNER NETWORK LOGS -----");

        try {
            for (org.openqa.selenium.logging.LogEntry entry :
                    driver.manage().logs().get(LogType.PERFORMANCE)) {

                String message = entry.getMessage();

                if (message.contains("forkme_right_green_007200.png")) {
                    System.out.println(message);
                }
            }
        } catch (Exception e) {
            System.out.println(
                "Could not read performance logs: " + e.getMessage()
            );
        }

        System.out.println("-------------------------------");

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
