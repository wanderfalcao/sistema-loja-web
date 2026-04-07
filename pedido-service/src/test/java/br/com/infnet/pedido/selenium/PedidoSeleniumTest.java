package br.com.infnet.pedido.selenium;

import br.com.infnet.client.ProdutoInfo;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Testes E2E com Selenium headless — executar com:
 *   mvn test -Dgroups=selenium -DexcludedGroups= -Dspring.profiles.active=test
 *
 * ProdutoServiceClient é mockado para retornar um produto fixo,
 * garantindo que o formulário de criação exiba a tabela de seleção.
 */
@Tag("selenium")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PedidoSeleniumTest {

    @LocalServerPort int port;
    @Autowired PedidoRepository repository;
    @MockBean ProdutoServiceClient produtoServiceClient;

    static WebDriver driver;
    static WebDriverWait wait;

    static final ProdutoInfo PRODUTO_MOCK = new ProdutoInfo(
            UUID.randomUUID(), "Produto Teste Selenium", "SEL-001",
            new BigDecimal("50.00"), true, 10);

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
    void setUp() {
        repository.deleteAll();
        when(produtoServiceClient.listarAtivos()).thenReturn(List.of(PRODUTO_MOCK));
        when(produtoServiceClient.buscarProduto(PRODUTO_MOCK.getId())).thenReturn(PRODUTO_MOCK);
    }

    String baseUrl() { return "http://localhost:" + port; }

    PedidoListPage abrirLista() {
        driver.get(baseUrl() + "/pedidos");
        return new PedidoListPage(driver);
    }

    PedidoListPage criarPedidoNaLista(String observacao) {
        abrirLista().clicarNovoPedido().preencherESalvar(observacao);
        driver.get(baseUrl() + "/pedidos");
        return new PedidoListPage(driver);
    }

    @Test @Order(1)
    void listagem_carregaComSucesso() {
        abrirLista();
        assertThat(driver.getTitle()).isNotBlank();
        assertThat(driver.findElement(By.tagName("h1")).getText()).containsIgnoringCase("Pedidos");
    }

    @Test @Order(2)
    void linkNovoPedido_existe() {
        abrirLista();
        assertThat(driver.findElement(By.id("btn-novo-pedido")).isDisplayed()).isTrue();
    }

    @Test @Order(3)
    void formularioCriacao_exibeTabelaDeProdutos() {
        driver.get(baseUrl() + "/pedidos/novo");
        PedidoFormPage form = new PedidoFormPage(driver);
        assertThat(form.formularioNovoVisivel()).isTrue();
        assertThat(form.tabelaProdutosVisivel()).isTrue();
        assertThat(driver.findElement(By.id("observacao")).isDisplayed()).isTrue();
    }

    @Test @Order(4)
    void criarPedido_comProdutoSelecionado() {
        assertThat(criarPedidoNaLista("Pedido Selenium Teste").contarPedidos()).isGreaterThan(0);
    }

    @Test @Order(5)
    void mensagemSucesso_aposCriar() {
        driver.get(baseUrl() + "/pedidos/novo");
        PedidoDetailPage detalhe = new PedidoFormPage(driver).preencherESalvar("Pedido Sucesso");
        assertThat(detalhe.alertaSucessoVisivel()).isTrue();
    }

    @Test @Order(6)
    void semItensSelecionados_mostraErroNaLista() {
        driver.get(baseUrl() + "/pedidos/novo");
        PedidoListPage lista = new PedidoFormPage(driver).submeterSemItens();
        assertThat(lista.alertaErroVisivel()).isTrue();
        assertThat(lista.textoAlertaErro()).containsIgnoringCase("produto");
    }

    @Test @Order(7)
    void sqlInjection_naoVazaDadosInternos() {
        criarPedidoNaLista("'; DROP TABLE pedidos; --");
        String pageSource = driver.getPageSource();
        assertThat(pageSource).doesNotContain("java.sql");
        assertThat(pageSource).doesNotContain("HibernateException");
    }

    @Test @Order(8)
    void editarPedido_atualizaObservacao() {
        criarPedidoNaLista("Observacao Original");
        PedidoFormPage form = abrirLista().clicarEditarNaLinha(0);
        assertThat(form.getValorObservacao()).isEqualTo("Observacao Original");
        assertThat(form.preencherESalvarEdicao("Observacao Editada").alertaSucessoVisivel()).isTrue();
    }

    @Test @Order(9)
    void avancarStatus_pendenteParaProcessando() {
        PedidoListPage lista = criarPedidoNaLista("Avançar Status Teste").clicarBotaoStatus("PROCESSANDO");
        assertThat(driver.getPageSource()).contains("PROCESSANDO");
    }

    @Test @Order(10)
    void contestarPedidoConcluido_funciona() {
        PedidoListPage lista = criarPedidoNaLista("Para Contestar")
                .clicarBotaoStatus("PROCESSANDO")
                .clicarBotaoStatus("CONCLUIDO")
                .clicarContestaNaLinha("Produto recebido com defeito");
        assertThat(driver.getPageSource()).contains("CONTESTADO");
    }

    @Test @Order(11)
    void cancelarPedido_funciona() {
        assertThat(criarPedidoNaLista("Para Cancelar").clicarBotaoStatus("CANCELADO")
                .getBadgeStatusNaLinha(0)).containsIgnoringCase("CANCELADO");
    }

    @Test @Order(12)
    void removerPedido_decrementaContagem() {
        PedidoListPage lista = criarPedidoNaLista("Para Remover");
        int totalAntes = lista.contarPedidos();
        assertThat(lista.clicarDeletarNaLinha(0).contarPedidos()).isLessThan(totalAntes);
    }

    @Test @Order(13)
    void detalhe_exibeDescricaoGeradaAutomaticamente() {
        criarPedidoNaLista("Pedido Para Detalhe");
        driver.findElement(By.cssSelector(
                "#tabelaPedidos tbody tr:first-child a[href*='/pedidos/']:not([href*='editar'])")).click();
        wait.until(ExpectedConditions.urlMatches(".*/pedidos/[0-9a-f\\-]+$"));
        assertThat(driver.getPageSource()).containsIgnoringCase("Produto Teste Selenium");
    }

    @Test @Order(14)
    void deveManterPedidoAoCancelarExclusao() {
        PedidoListPage lista = criarPedidoNaLista("Pedido Manter");
        int antes = lista.contarPedidos();
        ((JavascriptExecutor) driver).executeScript("window.confirm = function(){ return false; }");
        driver.findElements(By.cssSelector("form[action*='/deletar'] button[type='submit']")).get(0).click();
        assertThat(lista.contarPedidos()).isEqualTo(antes);
    }

    @Test @Order(15)
    void deveExibirBadgeStatusCorreto() {
        criarPedidoNaLista("Badge Teste");
        WebElement badge = driver.findElement(By.cssSelector("#tabelaPedidos tbody tr:first-child .sbadge"));
        assertThat(badge.getText()).containsIgnoringCase("PENDENTE");
        assertThat(badge.getAttribute("class")).contains("sb-PENDENTE");
    }

    @Test @Order(16)
    void deveExibirHistoricoDeStatusNaTelaDeDetalhe() {
        criarPedidoNaLista("Pedido Historico");
        driver.findElement(By.cssSelector(
                "#tabelaPedidos tbody tr:first-child a[href*='/pedidos/']:not([href*='editar'])")).click();
        wait.until(ExpectedConditions.urlMatches(".*/pedidos/[0-9a-f\\-]+$"));
        assertThat(new PedidoDetailPage(driver).getStatus()).containsIgnoringCase("PENDENTE");
    }

    @Test @Order(17)
    void deveAdicionarItemManualNoDetalhe() {
        criarPedidoNaLista("Pedido Com Item Manual");
        driver.findElement(By.cssSelector(
                "#tabelaPedidos tbody tr:first-child a[href*='/pedidos/']:not([href*='editar'])")).click();
        wait.until(ExpectedConditions.urlMatches(".*/pedidos/[0-9a-f\\-]+$"));
        PedidoDetailPage detalhe = new PedidoDetailPage(driver);
        assertThat(detalhe.formularioAdicionarItemVisivel()).isTrue();
        assertThat(detalhe.adicionarItem("Produto Manual", "25.00", 2).contarItens()).isGreaterThan(0);
    }

    @Test @Order(18)
    void buscarPorDescricao_filtraResultados() {
        criarPedidoNaLista("Pedido Busca Alpha");
        criarPedidoNaLista("Pedido Busca Beta");
        driver.get(baseUrl() + "/pedidos?busca=Alpha");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("tabelaPedidos")));
        assertThat(driver.getPageSource()).containsIgnoringCase("Alpha");
    }
}
