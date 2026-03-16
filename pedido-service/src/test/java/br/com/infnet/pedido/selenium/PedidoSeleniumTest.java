package br.com.infnet.pedido.selenium;

import br.com.infnet.client.ProdutoServiceClient;
import br.com.infnet.pedido.repository.PedidoRepository;
import br.com.infnet.pedido.selenium.pages.PedidoDetailPage;
import br.com.infnet.pedido.selenium.pages.PedidoFormPage;
import br.com.infnet.pedido.selenium.pages.PedidoListPage;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes E2E com Selenium headless — executar com: mvn test -Dgroups=selenium
 *
 * ProdutoServiceClient é mockado para isolar chamadas ao produto-service.
 */
@Tag("selenium")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PedidoSeleniumTest {

    @LocalServerPort
    int port;

    @Autowired
    PedidoRepository repository;

    @MockBean
    ProdutoServiceClient produtoServiceClient;

    static WebDriver driver;
    static WebDriverWait wait;

    @BeforeAll
    static void setUpDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--headless=new", "--no-sandbox",
                "--disable-dev-shm-usage", "--window-size=1280,720");
        driver = new ChromeDriver(opts);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterAll
    static void tearDownDriver() {
        if (driver != null) driver.quit();
    }

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    String baseUrl() {
        return "http://localhost:" + port;
    }

    PedidoListPage abrirLista() {
        driver.get(baseUrl() + "/pedidos");
        return new PedidoListPage(driver);
    }

    PedidoListPage criarPedidoNaLista(String descricao, String valor) {
        PedidoListPage lista = abrirLista();
        PedidoFormPage form = lista.clicarNovoPedido();
        return form.preencherESalvar(descricao, valor);
    }

    @Test
    @Order(1)
    void listagem_carregaComSucesso() {
        abrirLista();
        assertThat(driver.getTitle()).isNotBlank();
        assertThat(driver.findElement(By.tagName("h1")).getText())
                .containsIgnoringCase("Pedidos");
    }

    @Test
    @Order(2)
    void linkNovoPedido_existe() {
        abrirLista();
        assertThat(driver.findElement(By.linkText("Novo Pedido")).isDisplayed()).isTrue();
    }

    @Test
    @Order(3)
    void formularioCriacao_carrega() {
        driver.get(baseUrl() + "/pedidos/novo");
        assertThat(driver.findElement(By.id("descricao")).isDisplayed()).isTrue();
        assertThat(driver.findElement(By.id("valor")).isDisplayed()).isTrue();
    }

    @Test
    @Order(4)
    void criarPedido_comDadosValidos() {
        PedidoListPage lista = criarPedidoNaLista("Pedido Selenium Teste", "75.50");
        assertThat(lista.contarPedidos()).isGreaterThan(0);
    }

    @Test
    @Order(5)
    void mensagemSucesso_aposCriar() {
        PedidoListPage lista = criarPedidoNaLista("Pedido Para Sucesso", "10.00");
        assertThat(lista.alertaSucessoVisivel()).isTrue();
    }

    @Test
    @Order(6)
    void valorInvalido_mostraErro() {
        driver.get(baseUrl() + "/pedidos/novo");
        PedidoFormPage form = new PedidoFormPage(driver);

        form = form.preencherESubmeterComErro("Test Erro", "nao-eh-numero");

        assertThat(form.erroEstaVisivel()).isTrue();
        assertThat(form.obterMensagemDeErro()).isNotBlank();
    }

    @Test
    @Order(7)
    void sqlInjection_naoVazaDadosInternos() {
        PedidoListPage lista = criarPedidoNaLista("'; DROP TABLE pedidos; --", "1.00");

        wait.until(ExpectedConditions.urlContains("/pedidos"));
        String pageSource = driver.getPageSource();
        assertThat(pageSource).doesNotContain("java.sql");
        assertThat(pageSource).doesNotContain("HibernateException");
    }

    @Test
    @Order(8)
    void editarPedido_funciona() {
        PedidoListPage lista = criarPedidoNaLista("Para Editar", "20.00");

        PedidoFormPage form = lista.clicarEditarNaLinha(0);
        assertThat(form.getValorDescricao()).isEqualTo("Para Editar");

        lista = form.preencherESalvar("Pedido Editado", "20.00");
        assertThat(driver.getPageSource()).contains("Pedido Editado");
    }

    @Test
    @Order(9)
    void avancarStatus_pendenteParaProcessando() {
        PedidoListPage lista = criarPedidoNaLista("Avançar Status Teste", "30.00");
        lista = lista.clicarBotaoStatus("PROCESSANDO");
        assertThat(driver.getPageSource()).contains("PROCESSANDO");
    }

    @Test
    @Order(10)
    void contestarPedidoConcluido_funciona() {
        PedidoListPage lista = criarPedidoNaLista("Para Contestar", "50.00");
        lista = lista.clicarBotaoStatus("PROCESSANDO");
        lista = lista.clicarBotaoStatus("CONCLUIDO");
        lista = lista.clicarContestaNaLinha("Produto recebido com defeito");
        assertThat(driver.getPageSource()).contains("CONTESTADO");
    }

    @Test
    @Order(11)
    void cancelarPedido_funciona() {
        PedidoListPage lista = criarPedidoNaLista("Para Cancelar", "15.00");
        lista = lista.clicarBotaoStatus("CANCELADO");
        assertThat(driver.getPageSource()).contains("CANCELADO");
    }

    @Test
    @Order(12)
    void removerPedido_funciona() {
        PedidoListPage lista = criarPedidoNaLista("Para Remover", "7.00");
        int totalAntes = lista.contarPedidos();
        lista = lista.clicarDeletarNaLinha(0);
        assertThat(lista.contarPedidos()).isLessThan(totalAntes);
    }

    @Test
    @Order(13)
    void deveExibirPaginaDeDetalhe() {
        criarPedidoNaLista("Pedido Para Detalhe", "25.00");

        WebElement linkDetalhe = driver.findElement(
                By.cssSelector("#tabelaPedidos tbody tr:first-child a[href*='/pedidos/']"));
        linkDetalhe.click();

        wait.until(ExpectedConditions.urlMatches(".*/pedidos/[0-9a-f\\-]+$"));
        assertThat(driver.getCurrentUrl()).matches(".*/pedidos/[0-9a-f\\-]+");
        assertThat(driver.getPageSource()).contains("Pedido Para Detalhe");
    }

    @Test
    @Order(14)
    void deveExibirErroAoCriarComDescricaoVazia() {
        driver.get(baseUrl() + "/pedidos/novo");
        PedidoFormPage form = new PedidoFormPage(driver);
        form = form.preencherESubmeterComErro("", "10.00");
        assertThat(form.erroEstaVisivel()).isTrue();
    }

    @Test
    @Order(15)
    void deveManterPedidoAoCancelarExclusao() {
        PedidoListPage lista = criarPedidoNaLista("Pedido Manter", "50.00");
        int antes = lista.contarPedidos();

        ((JavascriptExecutor) driver).executeScript("window.confirm = function(){ return false; }");
        WebElement btn = driver.findElements(
                By.cssSelector("form[action*='/deletar'] button[type='submit']")).get(0);
        btn.click();

        assertThat(lista.contarPedidos()).isEqualTo(antes);
    }

    @Test
    @Order(16)
    void deveExibirBadgeStatusCorreto() {
        criarPedidoNaLista("Badge Teste", "10.00");

        WebElement badge = driver.findElement(
                By.cssSelector("#tabelaPedidos tbody tr:first-child .sbadge"));
        assertThat(badge.getText()).containsIgnoringCase("PENDENTE");
        assertThat(badge.getAttribute("class")).contains("sb-PENDENTE");
    }

    @Test
    @Order(17)
    void deveExibirHistoricoDeStatusNaTeladeDetalhe() {
        PedidoListPage lista = criarPedidoNaLista("Pedido Historico", "40.00");
        WebElement linkDetalhe = driver.findElement(
                By.cssSelector("#tabelaPedidos tbody tr:first-child a[href*='/pedidos/']"));
        linkDetalhe.click();
        wait.until(ExpectedConditions.urlMatches(".*/pedidos/[0-9a-f\\-]+$"));
        PedidoDetailPage detalhe = new PedidoDetailPage(driver);
        assertThat(detalhe.getStatus()).containsIgnoringCase("PENDENTE");
    }

    @Test
    @Order(18)
    void deveAdicionarItemManualSemProdutoId() {
        PedidoListPage lista = criarPedidoNaLista("Pedido Com Item Manual", "50.00");

        WebElement linkDetalhe = driver.findElement(
                By.cssSelector("#tabelaPedidos tbody tr:first-child a[href*='/pedidos/']"));
        linkDetalhe.click();
        wait.until(ExpectedConditions.urlMatches(".*/pedidos/[0-9a-f\\-]+$"));

        PedidoDetailPage detalhe = new PedidoDetailPage(driver);
        assertThat(detalhe.formularioAdicionarItemVisivel()).isTrue();

        detalhe = detalhe.adicionarItem("Produto Manual", "25.00", 2);
        assertThat(detalhe.contarItens()).isGreaterThan(0);
    }
}
