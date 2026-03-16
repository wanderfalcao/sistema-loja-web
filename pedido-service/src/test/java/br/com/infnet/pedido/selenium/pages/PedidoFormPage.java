package br.com.infnet.pedido.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

public class PedidoFormPage extends BasePage {

    @FindBy(id = "descricao")
    private WebElement campoDescricao;

    @FindBy(id = "valor")
    private WebElement campoValor;

    @FindBy(css = "button[type='submit']")
    private WebElement btnSalvar;

    public PedidoFormPage(WebDriver driver) {
        super(driver);
    }

    /** Preenche o formulário e submete esperando redirecionamento para a lista. */
    public PedidoListPage preencherESalvar(String descricao, String valor) {
        aguardarElemento(By.id("descricao"));
        campoDescricao.clear();
        campoDescricao.sendKeys(descricao);
        campoValor.clear();
        campoValor.sendKeys(valor);
        clicarComJs(btnSalvar);
        return new PedidoListPage(driver);
    }

    /**
     * Submete com dados inválidos usando JS para contornar a validação HTML5,
     * chegando à validação do servidor.
     */
    public PedidoFormPage preencherESubmeterComErro(String descricao, String valor) {
        aguardarElemento(By.id("descricao"));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('descricao').value = arguments[0];" +
                "document.getElementById('valor').value = arguments[1];",
                descricao, valor);
        ((JavascriptExecutor) driver).executeScript("document.querySelector('form').submit();");
        aguardarElemento(By.cssSelector(".alert-danger"));
        return new PedidoFormPage(driver);
    }

    public boolean erroEstaVisivel() {
        List<WebElement> alertas = driver.findElements(By.cssSelector(".alert-danger"));
        return !alertas.isEmpty() && alertas.get(0).isDisplayed();
    }

    public String obterMensagemDeErro() {
        aguardarElemento(By.cssSelector(".alert-danger"));
        return driver.findElement(By.cssSelector(".alert-danger")).getText();
    }

    public PedidoListPage cancelar() {
        WebElement btnCancelar = driver.findElement(By.cssSelector("a[href='/pedidos']"));
        clicarComJs(btnCancelar);
        return new PedidoListPage(driver);
    }

    public String getValorDescricao() {
        aguardarElemento(By.id("descricao"));
        return campoDescricao.getAttribute("value");
    }
}
