package br.com.infnet.produto.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class ProdutoDetalhePage extends BasePage {

    @FindBy(css = "h1.h4")
    private WebElement tituloProduto;

    @FindBy(css = ".sku-badge")
    private WebElement skuBadge;

    @FindBy(id = "preview-preco")
    private WebElement previewPreco;

    @FindBy(id = "percentual")
    private WebElement campoPercentual;

    @FindBy(id = "togglePromo")
    private WebElement togglePromo;

    public ProdutoDetalhePage(WebDriver driver) {
        super(driver);
    }

    public String getNome() {
        aguardarElemento(By.cssSelector("h1.h4"));
        return tituloProduto.getText();
    }

    public String getSku() {
        aguardarElemento(By.cssSelector(".sku-badge"));
        return skuBadge.getText();
    }

    public String getEstoque() {
        aguardarElemento(By.cssSelector(".info-row"));
        List<WebElement> rows = driver.findElements(By.cssSelector(".info-row"));
        for (WebElement row : rows) {
            String label = row.findElements(By.cssSelector(".label-sm")).stream()
                    .map(WebElement::getText).findFirst().orElse("");
            if (label.equalsIgnoreCase("Estoque")) {
                return row.findElement(By.cssSelector(".fw-semibold")).getText();
            }
        }
        return "";
    }

    public String getDescricao() {
        aguardarElemento(By.cssSelector(".info-row"));
        List<WebElement> rows = driver.findElements(By.cssSelector(".info-row"));
        for (WebElement row : rows) {
            List<WebElement> labels = row.findElements(By.cssSelector(".label-sm"));
            if (!labels.isEmpty() && labels.get(0).getText().equalsIgnoreCase("Descrição")) {
                List<WebElement> texts = row.findElements(By.cssSelector("p.mb-0"));
                return texts.isEmpty() ? "" : texts.get(0).getText();
            }
        }
        return "";
    }

    public ProdutoDetalhePage ativarFormPromo() {
        aguardarClicavel(By.id("togglePromo"));
        clicarComJs(togglePromo);
        aguardarElemento(By.id("percentual"));
        return this;
    }

    public String preencherPromocaoEObterPreview(String percentual) {
        aguardarElemento(By.id("percentual"));
        ((JavascriptExecutor) driver).executeScript(
                "var input = document.getElementById('percentual');" +
                "input.value = arguments[0];" +
                "input.dispatchEvent(new Event('input'));",
                percentual);
        aguardarElemento(By.id("preview-preco"));
        return previewPreco.getText();
    }

    public ProdutoListPage salvarPromocao() {
        WebElement btnSalvar = driver.findElement(
                By.cssSelector("form[action*='/promocao'] button[type='submit']"));
        clicarComJs(btnSalvar);
        wait.until(ExpectedConditions.stalenessOf(btnSalvar));
        return new ProdutoListPage(driver);
    }

    public ProdutoListPage voltarParaLista() {
        WebElement link = driver.findElement(By.linkText("Voltar à lista"));
        clicarComJs(link);
        return new ProdutoListPage(driver);
    }
}
