package br.com.infnet.pedido.selenium;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Extensão JUnit 5 que captura screenshot automaticamente quando um teste falha.
 * Os arquivos são salvos em {@code target/screenshots/{TestClass}/{methodName}.png}.
 *
 * <p>Registrar o driver via {@link #registerDriver(WebDriver)} no {@code @BeforeAll}
 * do teste, logo após a criação do ChromeDriver.</p>
 */
public class SeleniumExtension implements TestWatcher {

    private static WebDriver driver;

    static void registerDriver(WebDriver webDriver) {
        driver = webDriver;
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        if (!(driver instanceof TakesScreenshot)) return;
        try {
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            String className  = context.getRequiredTestClass().getSimpleName();
            String methodName = context.getRequiredTestMethod().getName();
            Path dir = Paths.get("target", "screenshots", className);
            Files.createDirectories(dir);
            Files.write(dir.resolve(methodName + ".png"), screenshot);
        } catch (IOException e) {
            System.err.println("[SeleniumExtension] Não foi possível salvar screenshot: " + e.getMessage());
        }
    }
}
