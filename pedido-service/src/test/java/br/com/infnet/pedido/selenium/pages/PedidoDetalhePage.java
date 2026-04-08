package br.com.infnet.pedido.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class PedidoDetalhePage extends BasePage {

    @FindBy(css = ".sbadge")
    private WebElement badgeStatus;

    @FindBy(css = "h1.page-title")
    private WebElement titulo;

    public PedidoDetalhePage(WebDriver driver) {
        super(driver);
    }

    public String getStatus() {
        aguardarElemento(By.cssSelector(".sbadge"));
        return badgeStatus.getText();
    }

    public String getClasseBadgeStatus() {
        aguardarElemento(By.cssSelector(".sbadge"));
        return badgeStatus.getAttribute("class");
    }

    public String getDescricao() {
        aguardarElemento(By.cssSelector("h1.page-title"));
        return titulo.getText();
    }

    public boolean historicoVisivel() {
        return !driver.findElements(By.cssSelector("table.table-sm tbody tr")).isEmpty();
    }

    public PedidoListPage clicarVoltar() {
        WebElement link = driver.findElement(By.cssSelector("a[href='/pedidos']"));
        clicarComJs(link);
        return new PedidoListPage(driver);
    }

    public PedidoListPage clicarAvancarStatus(String statusAlvo) {
        By selector = By.cssSelector("input[name='novoStatus'][value='" + statusAlvo + "']");
        aguardarElemento(selector);
        WebElement input = driver.findElement(selector);
        WebElement form = input.findElement(By.xpath("./.."));
        WebElement btn = form.findElement(By.cssSelector("button[type='submit']"));
        clicarComJs(btn);
        wait.until(ExpectedConditions.stalenessOf(btn));
        return new PedidoListPage(driver);
    }

    public boolean formularioAdicionarItemVisivel() {
        return !driver.findElements(By.cssSelector("form[action$='/itens']")).isEmpty();
    }

    public PedidoDetalhePage adicionarItem(String nomeProduto, String precoUnitario, int quantidade) {
        aguardarElemento(By.name("nomeProduto"));
        driver.findElement(By.name("nomeProduto")).sendKeys(nomeProduto);
        driver.findElement(By.name("precoUnitario")).clear();
        driver.findElement(By.name("precoUnitario")).sendKeys(precoUnitario);
        driver.findElement(By.name("quantidade")).clear();
        driver.findElement(By.name("quantidade")).sendKeys(String.valueOf(quantidade));
        WebElement btnAdicionar = driver.findElement(
                By.cssSelector("form[action$='/itens'] button[type='submit']"));
        clicarComJs(btnAdicionar);
        wait.until(ExpectedConditions.stalenessOf(btnAdicionar));
        return new PedidoDetalhePage(driver);
    }

    public int contarItens() {
        return driver.findElements(By.cssSelector("table tbody tr")).size();
    }

    public String getMotivoContestacao() {
        List<WebElement> elements = driver.findElements(By.cssSelector(".contestacao-box .cmotivo"));
        return elements.isEmpty() ? "" : elements.get(0).getText();
    }

    public String getObservacao() {
        List<WebElement> elements = driver.findElements(By.cssSelector(".info-value"));
        return elements.isEmpty() ? "" : elements.get(0).getText();
    }

    public boolean alertaSucessoVisivel() {
        List<WebElement> alertas = driver.findElements(By.cssSelector(".alert-success"));
        if (alertas.isEmpty()) return false;
        try {
            wait.until(ExpectedConditions.visibilityOf(alertas.get(0)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
