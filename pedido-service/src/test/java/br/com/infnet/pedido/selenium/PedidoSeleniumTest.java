package br.com.infnet.pedido.selenium;

import br.com.infnet.client.ProdutoInfo;
import br.com.infnet.client.ProdutoServiceClient;
import br.com.infnet.pedido.repository.PedidoRepository;
import br.com.infnet.pedido.repository.StatusHistoricoRepository;
import br.com.infnet.pedido.selenium.pages.PedidoDetalhePage;
import br.com.infnet.pedido.selenium.pages.PedidoFormPage;
import br.com.infnet.pedido.selenium.pages.PedidoListPage;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Testes E2E com Selenium — headless por padrão, com opção de modo visual.
 *
 * Executar:
 *   mvn test -Dgroups=selenium -DexcludedGroups= -Dspring.profiles.active=test
 * Para modo com browser visível (debug local):
 *   mvn test -Dgroups=selenium -Dselenium.headless=false
 *
 * ProdutoServiceClient é mockado para retornar um produto fixo,
 * garantindo que o formulário de criação exiba os cards de produto.
 */
@Tag("selenium")
@ExtendWith(SeleniumExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PedidoSeleniumTest {

    /** URL do serviço deployado — quando presente, o WebDriver aponta para o Cloud Run. */
    private static final String REMOTE_URL =
            System.getProperty("selenium.base.url.pedidos");
    private static final boolean REMOTE_MODE =
            REMOTE_URL != null && !REMOTE_URL.isBlank();

    @LocalServerPort int port;
    @Autowired PedidoRepository repository;
    @Autowired StatusHistoricoRepository historicoRepository;
    @MockBean ProdutoServiceClient produtoServiceClient;

    static WebDriver driver;
    static WebDriverWait wait;

    static final ProdutoInfo PRODUTO_MOCK = new ProdutoInfo(
            UUID.randomUUID(), "Produto Teste Selenium", "SEL-001",
            new BigDecimal("50.00"), true, 10, null);

    /** Rastreia IDs dos pedidos criados em cada teste para cleanup em remote mode. */
    private final List<String> createdIds = new ArrayList<>();

    @BeforeAll
    static void setUpDriver() {
        WebDriverManager.chromedriver().setup();
        boolean headless = !"false".equalsIgnoreCase(System.getProperty("selenium.headless", "true"));
        ChromeOptions opts = new ChromeOptions();
        if (headless) opts.addArguments("--headless=new");
        opts.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--window-size=1280,720");
        driver = new ChromeDriver(opts);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        SeleniumExtension.registerDriver(driver);
    }

    @AfterAll
    static void tearDownDriver() {
        if (driver != null) driver.quit();
    }

    @BeforeEach
    void setUp() {
        createdIds.clear();
        if (!REMOTE_MODE) {
            historicoRepository.deleteAll();
            repository.deleteAll();
        }
        when(produtoServiceClient.listarAtivos()).thenReturn(List.of(PRODUTO_MOCK));
        when(produtoServiceClient.buscarProduto(PRODUTO_MOCK.getId())).thenReturn(PRODUTO_MOCK);
    }

    @AfterEach
    void limparRemote() {
        if (!REMOTE_MODE || createdIds.isEmpty()) return;
        driver.get(baseUrl() + "/pedidos?size=200");
        PedidoListPage lista = new PedidoListPage(driver);
        for (String id : new ArrayList<>(createdIds)) {
            try {
                lista.clicarDeletarPorId(id);
            } catch (Exception ignored) { /* já deletado pelo próprio teste */ }
        }
    }

    String baseUrl() {
        return REMOTE_MODE ? REMOTE_URL : "http://localhost:" + port;
    }

    PedidoListPage abrirLista() {
        driver.get(baseUrl() + "/pedidos?size=200");
        return new PedidoListPage(driver);
    }

    PedidoListPage criarPedidoNaLista(String observacao) {
        abrirLista().clicarNovoPedido().preencherESalvar(observacao);
        // driver está em /pedidos/{uuid} — capturar ID antes de navegar
        String url = driver.getCurrentUrl();
        String id = url.substring(url.lastIndexOf('/') + 1);
        createdIds.add(id);
        driver.get(baseUrl() + "/pedidos?size=200");
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
        PedidoDetalhePage detalhe = new PedidoFormPage(driver).preencherESalvar("Pedido Sucesso");
        // capturar ID para cleanup
        String url = driver.getCurrentUrl();
        createdIds.add(url.substring(url.lastIndexOf('/') + 1));
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
        criarPedidoNaLista("Avançar Status Teste").clicarBotaoStatus("PROCESSANDO");
        assertThat(driver.getPageSource()).contains("PROCESSANDO");
    }

    @Test @Order(10)
    void contestarPedidoConcluido_funciona() {
        criarPedidoNaLista("Para Contestar")
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
        String idRemovido = createdIds.isEmpty() ? null : createdIds.get(createdIds.size() - 1);
        lista = lista.clicarDeletarNaLinha(0);
        // já deletado pelo teste — remover do tracking para evitar tentativa dupla
        if (idRemovido != null) createdIds.remove(idRemovido);
        assertThat(lista.contarPedidos()).isLessThan(totalAntes);
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
        assertThat(new PedidoDetalhePage(driver).getStatus()).containsIgnoringCase("PENDENTE");
    }

    @Test @Order(17)
    void deveAdicionarItemManualNoDetalhe() {
        criarPedidoNaLista("Pedido Com Item Manual");
        driver.findElement(By.cssSelector(
                "#tabelaPedidos tbody tr:first-child a[href*='/pedidos/']:not([href*='editar'])")).click();
        wait.until(ExpectedConditions.urlMatches(".*/pedidos/[0-9a-f\\-]+$"));
        PedidoDetalhePage detalhe = new PedidoDetalhePage(driver);
        assertThat(detalhe.formularioAdicionarItemVisivel()).isTrue();
        assertThat(detalhe.adicionarItem("Produto Manual", "25.00", 2).contarItens()).isGreaterThan(0);
    }

    @Test @Order(18)
    void buscarPorDescricao_filtraResultados() {
        criarPedidoNaLista("Pedido Busca Alpha");
        criarPedidoNaLista("Pedido Busca Beta");
        driver.get(baseUrl() + "/pedidos?size=200&busca=Alpha");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("tabelaPedidos")));
        assertThat(driver.getPageSource()).containsIgnoringCase("Alpha");
    }

    @Test @Order(19)
    void devePreservarObservacaoAoContestar() {
        String observacaoOriginal = "Observação que deve ser preservada";
        String motivoContestacao  = "Item faltando na entrega";

        PedidoListPage lista = criarPedidoNaLista(observacaoOriginal)
                .clicarBotaoStatus("PROCESSANDO")
                .clicarBotaoStatus("CONCLUIDO")
                .clicarContestaNaLinha(motivoContestacao);

        PedidoDetalhePage detalhe = lista.clicarDetalheNaLinha(0);

        assertThat(detalhe.getObservacao()).contains(observacaoOriginal);
        assertThat(detalhe.getMotivoContestacao()).isNotBlank();
    }

    @Test @Order(20)
    void deveExibirMotivoContestacaoNoDetalhe() {
        String motivo = "Produto diferente do anunciado";

        PedidoListPage lista = criarPedidoNaLista("Pedido Para Verificar Motivo")
                .clicarBotaoStatus("PROCESSANDO")
                .clicarBotaoStatus("CONCLUIDO")
                .clicarContestaNaLinha(motivo);

        PedidoDetalhePage detalhe = lista.clicarDetalheNaLinha(0);

        assertThat(detalhe.getMotivoContestacao()).isEqualTo(motivo);
        assertThat(detalhe.getStatus()).containsIgnoringCase("CONTESTADO");
    }
}
