package br.com.infnet.pedido.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

/**
 * Page Object do formulário de pedido.
 *
 * Modo criação (/pedidos/novo): exibe tabela de produtos com inputs[name="quantidades"],
 * campo #observacao e botão #btn-criar-pedido. Não há campos #descricao ou #valor.
 *
 * Modo edição (/pedidos/{id}/editar): exibe apenas #observacao e botão submit.
 * Após submit, redireciona para /pedidos (lista).
 */
public class PedidoFormPage extends BasePage {

    public PedidoFormPage(WebDriver driver) {
        super(driver);
    }

    /** Modo criação: qty=1 no primeiro produto, preenche observacao, submete → detalhe. */
    public PedidoDetalhePage preencherESalvar(String observacao) {
        aguardarElemento(By.id("formNovoPedido"));
        List<WebElement> qtyInputs = driver.findElements(By.name("quantidades"));
        if (!qtyInputs.isEmpty()) {
            WebElement first = qtyInputs.get(0);
            ((JavascriptExecutor) driver).executeScript("arguments[0].value='1'", first);
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].dispatchEvent(new Event('input'))", first);
        }
        preencherObservacao(observacao);
        WebElement btn = driver.findElement(By.cssSelector("button[type='submit']"));
        clicarComJs(btn);
        wait.until(ExpectedConditions.stalenessOf(btn));
        return new PedidoDetalhePage(driver);
    }

    /** Modo criação: submete sem selecionar nenhum produto → redireciona para lista com erro. */
    public PedidoListPage submeterSemItens() {
        aguardarElemento(By.id("formNovoPedido"));
        driver.findElements(By.name("quantidades")).forEach(inp ->
                ((JavascriptExecutor) driver).executeScript("arguments[0].value='0'", inp));
        ((JavascriptExecutor) driver).executeScript("document.getElementById('formNovoPedido').submit();");
        return new PedidoListPage(driver);
    }

    /** Modo edição: preenche observacao e submete → aguarda redirect para lista. */
    public PedidoListPage preencherESalvarEdicao(String observacao) {
        aguardarElemento(By.id("observacao"));
        preencherObservacao(observacao);
        WebElement btn = driver.findElement(By.cssSelector("button[type='submit']"));
        clicarComJs(btn);
        wait.until(ExpectedConditions.stalenessOf(btn));
        return new PedidoListPage(driver);
    }

    public String getValorObservacao() {
        aguardarElemento(By.id("observacao"));
        return driver.findElement(By.id("observacao")).getAttribute("value");
    }

    public PedidoListPage cancelar() {
        clicarComJs(driver.findElement(By.cssSelector("a[href='/pedidos']")));
        return new PedidoListPage(driver);
    }

    public boolean formularioNovoVisivel() {
        return !driver.findElements(By.id("formNovoPedido")).isEmpty();
    }

    public boolean tabelaProdutosVisivel() {
        return !driver.findElements(By.name("quantidades")).isEmpty();
    }

    public boolean erroEstaVisivel() {
        List<WebElement> alertas = driver.findElements(By.cssSelector(".alert-danger"));
        return !alertas.isEmpty() && alertas.get(0).isDisplayed();
    }

    public String obterMensagemDeErro() {
        aguardarElemento(By.cssSelector(".alert-danger"));
        return driver.findElement(By.cssSelector(".alert-danger")).getText();
    }

    private void preencherObservacao(String observacao) {
        List<WebElement> campos = driver.findElements(By.id("observacao"));
        if (!campos.isEmpty() && observacao != null && !observacao.isBlank()) {
            campos.get(0).clear();
            campos.get(0).sendKeys(observacao);
        }
    }
}
