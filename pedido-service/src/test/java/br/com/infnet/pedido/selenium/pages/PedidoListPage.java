package br.com.infnet.pedido.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class PedidoListPage extends BasePage {

    private static final By LINHAS = By.cssSelector("#tabelaPedidos tbody tr");

    @FindBy(linkText = "Novo Pedido")
    private WebElement btnNovoPedido;

    @FindBy(css = "#tabelaPedidos tbody tr")
    private List<WebElement> linhas;

    @FindBy(css = ".alert-success")
    private WebElement alertSucesso;

    public PedidoListPage(WebDriver driver) {
        super(driver);
    }

    public PedidoFormPage clicarNovoPedido() {
        aguardarClicavel(By.linkText("Novo Pedido"));
        clicarComJs(btnNovoPedido);
        return new PedidoFormPage(driver);
    }

    public int contarPedidos() {
        List<WebElement> rows = driver.findElements(LINHAS);
        if (rows.isEmpty()) return 0;
        aguardarElemento(LINHAS);
        return (int) linhas.stream()
                .filter(l -> !l.findElements(By.cssSelector("form[action*='/deletar'] button")).isEmpty())
                .count();
    }

    public PedidoDetailPage clicarDetalheNaLinha(int indice) {
        aguardarElemento(LINHAS);
        WebElement link = linhas.get(indice)
                .findElement(By.cssSelector("a[title='Ver detalhes'], a[href*='/pedidos/']:not([href*='editar'])"));
        clicarComJs(link);
        return new PedidoDetailPage(driver);
    }

    public PedidoFormPage clicarEditarNaLinha(int indice) {
        aguardarElemento(LINHAS);
        WebElement btnEditar = linhas.get(indice).findElement(By.cssSelector("a[title='Editar']"));
        clicarComJs(btnEditar);
        return new PedidoFormPage(driver);
    }

    /** Clica no botão de status usando o seletor global (não restrito à linha). */
    public PedidoListPage clicarBotaoStatus(String dataStatusAction) {
        By selector = By.cssSelector("button[data-status-action='" + dataStatusAction + "']");
        aguardarElemento(selector);
        List<WebElement> btns = driver.findElements(selector);
        WebElement btn = btns.get(0);
        clicarComJs(btn);
        wait.until(ExpectedConditions.stalenessOf(btn));
        return new PedidoListPage(driver);
    }

    public PedidoListPage clicarContestaNaLinha(String motivo) {
        By contestarSelector = By.cssSelector("button[data-status-action='CONTESTAR']");
        aguardarElemento(contestarSelector);
        clicarComJs(driver.findElements(contestarSelector).get(0));
        aguardarElemento(By.id("motivoContestacao"));
        driver.findElement(By.id("motivoContestacao")).sendKeys(motivo);
        WebElement form = driver.findElement(By.id("formContestar"));
        form.submit();
        wait.until(ExpectedConditions.stalenessOf(form));
        return new PedidoListPage(driver);
    }

    public PedidoListPage clicarDeletarNaLinha(int indice) {
        aguardarElemento(LINHAS);
        WebElement btn = linhas.get(indice)
                .findElement(By.cssSelector("form[action*='/deletar'] button[type='submit']"));
        ((JavascriptExecutor) driver).executeScript("window.confirm = function(){ return true; }");
        clicarComJs(btn);
        wait.until(ExpectedConditions.stalenessOf(btn));
        return new PedidoListPage(driver);
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

    public String getBadgeStatusNaLinha(int indice) {
        aguardarElemento(LINHAS);
        return linhas.get(indice).findElement(By.cssSelector(".sbadge, .badge")).getText();
    }
}
