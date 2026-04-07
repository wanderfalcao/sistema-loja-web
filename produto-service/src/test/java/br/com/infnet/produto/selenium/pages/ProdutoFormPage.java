package br.com.infnet.produto.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

public class ProdutoFormPage extends BasePage {

    @FindBy(id = "nome")
    private WebElement campoNome;

    @FindBy(id = "preco")
    private WebElement campoPreco;

    @FindBy(id = "descricao")
    private WebElement campoDescricao;

    @FindBy(id = "estoque")
    private WebElement campoEstoque;

    @FindBy(id = "btn-salvar")
    private WebElement btnSalvar;

    @FindBy(id = "btn-cancelar")
    private WebElement btnCancelar;

    public ProdutoFormPage(WebDriver driver) {
        super(driver);
    }

    /** Seleciona a primeira categoria não vazia disponível no select. */
    private void selecionarPrimeiraCategoria() {
        List<WebElement> selects = driver.findElements(By.id("categoria"));
        if (!selects.isEmpty()) {
            Select select = new Select(selects.get(0));
            if (select.getOptions().size() > 1) {
                select.selectByIndex(1);
            }
        }
    }

    /** Preenche nome, preço e categoria e submete aguardando redirect para a lista. */
    public ProdutoListPage preencherESalvar(String nome, String preco) {
        aguardarElemento(By.id("nome"));
        campoNome.clear();
        campoNome.sendKeys(nome);
        campoPreco.clear();
        campoPreco.sendKeys(preco);
        selecionarPrimeiraCategoria();
        clicarComJs(btnSalvar);
        wait.until(ExpectedConditions.stalenessOf(btnSalvar));
        return new ProdutoListPage(driver);
    }

    /** Preenche todos os campos (incluindo descrição e estoque) e salva aguardando redirect. */
    public ProdutoListPage preencherCompleto(String nome, String preco, String descricao, String estoque) {
        aguardarElemento(By.id("nome"));
        campoNome.clear();
        campoNome.sendKeys(nome);
        campoPreco.clear();
        campoPreco.sendKeys(preco);
        if (descricao != null && campoDescricao != null) {
            campoDescricao.clear();
            campoDescricao.sendKeys(descricao);
        }
        if (estoque != null && campoEstoque != null) {
            campoEstoque.clear();
            campoEstoque.sendKeys(estoque);
        }
        selecionarPrimeiraCategoria();
        clicarComJs(btnSalvar);
        wait.until(ExpectedConditions.stalenessOf(btnSalvar));
        return new ProdutoListPage(driver);
    }

    /**
     * Submete com dados inválidos usando JS para contornar a validação HTML5,
     * chegando à validação do servidor. Seleciona categoria para que a validação
     * recaia sobre nome/preço, não sobre categoria.
     */
    public ProdutoFormPage preencherESubmeterComErro(String nome, String preco) {
        aguardarElemento(By.id("nome"));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('nome').value = arguments[0];" +
                "document.getElementById('preco').value = arguments[1];",
                nome, preco);
        // seleciona a primeira categoria via JS para não disparar validação de categoria
        ((JavascriptExecutor) driver).executeScript(
                "var s = document.getElementById('categoria');" +
                "if (s && s.options.length > 1) s.selectedIndex = 1;");
        ((JavascriptExecutor) driver).executeScript("document.querySelector('form').submit();");
        aguardarElemento(By.id("mensagem-erro"));
        return new ProdutoFormPage(driver);
    }

    public boolean erroEstaVisivel() {
        List<WebElement> elementos = driver.findElements(By.id("mensagem-erro"));
        return !elementos.isEmpty() && elementos.get(0).isDisplayed();
    }

    public String obterMensagemDeErro() {
        aguardarElemento(By.id("mensagem-erro"));
        return driver.findElement(By.id("mensagem-erro")).getText();
    }

    public ProdutoListPage cancelar() {
        clicarComJs(btnCancelar);
        return new ProdutoListPage(driver);
    }

    public String getValorNome() {
        aguardarElemento(By.id("nome"));
        return campoNome.getAttribute("value");
    }

    public String getValorPreco() {
        aguardarElemento(By.id("preco"));
        return campoPreco.getAttribute("value");
    }
}
