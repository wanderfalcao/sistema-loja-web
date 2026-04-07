package br.com.infnet.produto.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class ProdutoListPage extends BasePage {

    private static final By LINHAS = By.cssSelector("#tabela-produtos tbody tr");

    @FindBy(id = "btn-novo-produto")
    private WebElement btnNovoProduto;

    @FindBy(css = "#tabela-produtos tbody tr")
    private List<WebElement> linhas;

    @FindBy(css = ".alert-success")
    private WebElement alertSucesso;

    public ProdutoListPage(WebDriver driver) {
        super(driver);
    }

    public ProdutoFormPage clicarNovoProduto() {
        aguardarClicavel(By.id("btn-novo-produto"));
        clicarComJs(btnNovoProduto);
        return new ProdutoFormPage(driver);
    }

    public int contarProdutos() {
        // A tabela só existe quando há produtos; se não aparecer em 10s, banco está vazio
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("tabela-produtos")));
        } catch (org.openqa.selenium.TimeoutException e) {
            return 0;
        }
        return (int) linhas.stream()
                .filter(l -> !l.findElements(By.cssSelector(".btn-excluir")).isEmpty())
                .count();
    }

    public ProdutoFormPage clicarEditarNaLinha(int indice) {
        aguardarElemento(LINHAS);
        WebElement btnEditar = linhas.get(indice).findElement(By.cssSelector(".btn-editar"));
        clicarComJs(btnEditar);
        return new ProdutoFormPage(driver);
    }

    public ProdutoListPage clicarExcluirNaLinha(int indice) {
        aguardarElemento(LINHAS);
        WebElement btnExcluir = linhas.get(indice).findElement(By.cssSelector(".btn-excluir"));
        clicarComJs(btnExcluir);
        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();
        wait.until(ExpectedConditions.stalenessOf(btnExcluir));
        return new ProdutoListPage(driver);
    }

    /** Clica em Excluir e cancela o alerta — produto permanece na lista. */
    public ProdutoListPage cancelarExcluirNaLinha(int indice) {
        aguardarElemento(LINHAS);
        WebElement btnExcluir = linhas.get(indice).findElement(By.cssSelector(".btn-excluir"));
        clicarComJs(btnExcluir);
        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().dismiss();
        return new ProdutoListPage(driver);
    }

    public ProdutoDetalhePage clicarDetalheNaLinha(int indice) {
        aguardarElemento(LINHAS);
        WebElement link = linhas.get(indice).findElement(By.cssSelector("a[title='Detalhe']"));
        clicarComJs(link);
        return new ProdutoDetalhePage(driver);
    }

    public ProdutoDetalhePage clicarDetalhePorNome(String nome) {
        aguardarElemento(LINHAS);
        WebElement link = linhas.stream()
                .filter(l -> nomeIgual(l, nome))
                .findFirst()
                .orElseThrow()
                .findElement(By.cssSelector("a[title='Detalhe']"));
        clicarComJs(link);
        return new ProdutoDetalhePage(driver);
    }

    public ProdutoFormPage clicarEditarPorNome(String nome) {
        aguardarElemento(LINHAS);
        WebElement btn = linhas.stream()
                .filter(l -> nomeIgual(l, nome))
                .findFirst()
                .orElseThrow()
                .findElement(By.cssSelector(".btn-editar"));
        clicarComJs(btn);
        return new ProdutoFormPage(driver);
    }

    public ProdutoListPage clicarExcluirPorNome(String nome) {
        aguardarElemento(LINHAS);
        WebElement btn = linhas.stream()
                .filter(l -> nomeIgual(l, nome))
                .findFirst()
                .orElseThrow()
                .findElement(By.cssSelector(".btn-excluir"));
        clicarComJs(btn);
        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();
        wait.until(ExpectedConditions.stalenessOf(btn));
        return new ProdutoListPage(driver);
    }

    /** Verifica se a célula de nome da linha contém EXATAMENTE o nome informado. */
    private boolean nomeIgual(WebElement linha, String nome) {
        List<WebElement> spans = linha.findElements(By.cssSelector("span.fw-semibold"));
        return !spans.isEmpty() && spans.get(0).getText().equals(nome);
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

    public String textoAlertaSucesso() {
        aguardarElemento(By.cssSelector(".alert-success"));
        return alertSucesso.getText();
    }

    public String getSkuNaLinha(int indice) {
        aguardarElemento(LINHAS);
        return linhas.get(indice).findElement(By.cssSelector(".sku-badge, td:nth-child(2)")).getText();
    }
}
