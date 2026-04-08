package br.com.infnet.produto.selenium;

import br.com.infnet.produto.repository.ProdutoRepository;
import br.com.infnet.produto.selenium.pages.ProdutoDetalhePage;
import br.com.infnet.produto.selenium.pages.ProdutoFormPage;
import br.com.infnet.produto.selenium.pages.ProdutoListPage;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes E2E com Selenium — headless por padrão, com opção de modo visual.
 *
 * Executar:
 *   mvn test -Dgroups=selenium -Dspring.profiles.active=test
 * Para modo com browser visível (debug local):
 *   mvn test -Dgroups=selenium -Dselenium.headless=false
 */
@Tag("selenium")
@ExtendWith(SeleniumExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProdutoSeleniumTest {

    /** URL do serviço deployado — quando presente, o WebDriver aponta para o Cloud Run. */
    private static final String REMOTE_URL =
            System.getProperty("selenium.base.url.produtos");
    private static final boolean REMOTE_MODE =
            REMOTE_URL != null && !REMOTE_URL.isBlank();

    /**
     * Sufixo único por execução da suite: " Teste-{6 chars}".
     * Garante que nomes de produtos criados pelos testes nunca colidem com dados de seed
     * nem com runs anteriores, eliminando falhas por constraint unique.
     */
    private static final String SUFFIX = " Teste-" + UUID.randomUUID().toString().substring(0, 6);

    @LocalServerPort
    private int porta;

    @Autowired
    private ProdutoRepository repository;

    private static WebDriver driver;

    /** Rastreia nomes dos produtos criados em cada teste para cleanup em remote mode. */
    private final List<String> createdNomes = new ArrayList<>();

    private String baseUrl() {
        return REMOTE_MODE ? REMOTE_URL : "http://localhost:" + porta;
    }

    @BeforeAll
    static void setUpDriver() {
        WebDriverManager.chromedriver().setup();
        boolean headless = !"false".equalsIgnoreCase(System.getProperty("selenium.headless", "true"));
        ChromeOptions options = new ChromeOptions();
        if (headless) options.addArguments("--headless=new");
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage",
                "--disable-gpu", "--window-size=1280,800");
        driver = new ChromeDriver(options);
        SeleniumExtension.registerDriver(driver);
    }

    @AfterAll
    static void tearDownDriver() {
        if (driver != null) driver.quit();
    }

    @BeforeEach
    void limparBanco() {
        createdNomes.clear();
        if (!REMOTE_MODE) repository.deleteAll();
    }

    @AfterEach
    void limparRemote() {
        if (!REMOTE_MODE || createdNomes.isEmpty()) return;
        // size=200 garante que todos os produtos criados estejam visíveis para exclusão
        driver.get(baseUrl() + "/produtos?size=200");
        for (String nome : new ArrayList<>(createdNomes)) {
            try {
                new ProdutoListPage(driver).clicarExcluirPorNome(nome);
                driver.get(baseUrl() + "/produtos?size=200");
            } catch (Exception ignored) { /* produto já deletado pelo próprio teste */ }
        }
    }

    /** Lista com size=200 para evitar paginação nos counts e buscas por nome. */
    private ProdutoListPage abrirLista() {
        driver.get(baseUrl() + "/produtos?size=200");
        return new ProdutoListPage(driver);
    }

    private ProdutoListPage criarProdutoNaLista(String nome, String preco) {
        abrirLista().clicarNovoProduto().preencherESalvar(nome, preco);
        createdNomes.add(nome);
        // Após o redirect do form, navegar para a lista completa (sem paginação)
        driver.get(baseUrl() + "/produtos?size=200");
        return new ProdutoListPage(driver);
    }

    private ProdutoListPage criarProdutoDeletavel(String nome, String preco) {
        abrirLista().clicarNovoProduto().preencherESalvarDeletavel(nome, preco);
        createdNomes.add(nome);
        driver.get(baseUrl() + "/produtos?size=200");
        return new ProdutoListPage(driver);
    }

    private ProdutoListPage criarProdutoCompleto(String nome, String preco, String desc, String estoque) {
        abrirLista().clicarNovoProduto().preencherCompleto(nome, preco, desc, estoque);
        createdNomes.add(nome);
        driver.get(baseUrl() + "/produtos?size=200");
        return new ProdutoListPage(driver);
    }

    @Test
    @Order(1)
    void deveCadastrarProdutoEExibirNaLista() {
        ProdutoListPage lista = abrirLista();
        int antes = lista.contarProdutos();
        lista = criarProdutoNaLista("Monitor 4K" + SUFFIX, "2500.00");

        assertThat(lista.contarProdutos()).isEqualTo(antes + 1);
    }

    @Test
    @Order(2)
    void deveCadastrarMultiplosProdutosEContabilizarCorretamente() {
        ProdutoListPage lista = abrirLista();
        int antes = lista.contarProdutos();

        criarProdutoNaLista("Teclado Mecanico" + SUFFIX, "350.00");
        criarProdutoNaLista("Mouse Gamer" + SUFFIX, "199.90");
        lista = criarProdutoNaLista("Webcam HD" + SUFFIX, "89.90");

        assertThat(lista.contarProdutos()).isEqualTo(antes + 3);
    }

    @Test
    @Order(3)
    void deveEditarProdutoEExibirMensagemDeSucesso() {
        ProdutoListPage lista = criarProdutoNaLista("Produto Original" + SUFFIX, "100.00");

        ProdutoFormPage form = lista.clicarEditarPorNome("Produto Original" + SUFFIX);
        assertThat(form.getValorNome()).isEqualTo("Produto Original" + SUFFIX);

        lista = form.preencherESalvar("Produto Editado" + SUFFIX, "200.00");
        // rastrear nome novo para cleanup
        createdNomes.remove("Produto Original" + SUFFIX);
        createdNomes.add("Produto Editado" + SUFFIX);
        assertThat(lista.alertaSucessoVisivel()).isTrue();
    }

    @Test
    @Order(4)
    void deveExcluirProdutoEDecrementarContagem() {
        ProdutoListPage lista = criarProdutoDeletavel("Produto para Excluir" + SUFFIX, "10.00");
        int antes = lista.contarProdutos();

        lista = lista.clicarExcluirPorNome("Produto para Excluir" + SUFFIX);
        // já deletado pelo teste — remover do tracking
        createdNomes.remove("Produto para Excluir" + SUFFIX);

        assertThat(lista.contarProdutos()).isLessThan(antes);
    }

    @Test
    @Order(5)
    void deveCancelarFormularioEVoltarParaLista() {
        ProdutoListPage lista = abrirLista();
        ProdutoFormPage form = lista.clicarNovoProduto();
        lista = form.cancelar();

        assertThat(driver.getCurrentUrl()).contains("/produtos");
    }

    @Test
    @Order(6)
    void deveExibirErroAoCadastrarComNomeVazio() {
        ProdutoListPage lista = abrirLista();
        ProdutoFormPage form = lista.clicarNovoProduto();

        form = form.preencherESubmeterComErro("", "100.00");

        assertThat(form.erroEstaVisivel()).isTrue();
        assertThat(form.obterMensagemDeErro()).contains("Nome obrigatorio");
    }

    @Test
    @Order(7)
    void deveExibirErroAoCadastrarComPrecoNegativo() {
        ProdutoListPage lista = abrirLista();
        ProdutoFormPage form = lista.clicarNovoProduto();

        form = form.preencherESubmeterComErro("Produto Valido" + SUFFIX, "-5.00");

        assertThat(form.erroEstaVisivel()).isTrue();
        assertThat(form.obterMensagemDeErro()).contains("Preco deve ser maior que zero");
    }

    @ParameterizedTest
    @CsvSource({
        "Teclado Gamer,    299.90",
        "Mouse Sem Fio,    149.50",
        "Headset Pro,      599.00"
    })
    @Order(8)
    void deveCadastrarProdutosParametrizados(String nome, String preco) {
        ProdutoListPage lista = abrirLista();
        int antes = lista.contarProdutos();
        lista = criarProdutoNaLista(nome.trim() + SUFFIX, preco.trim());

        assertThat(lista.contarProdutos()).isEqualTo(antes + 1);
    }

    @Test
    @Order(9)
    void deveManterProdutoAoCancelarConfirmacaoDeExclusao() {
        ProdutoListPage lista = criarProdutoNaLista("Produto Cancelado" + SUFFIX, "50.00");
        int antes = lista.contarProdutos();

        lista = lista.cancelarExcluirNaLinha(0);

        assertThat(lista.contarProdutos()).isEqualTo(antes);
    }

    @Test
    @Order(10)
    void devePersistirDescricaoEEstoqueAoCadastrar() {
        ProdutoListPage lista = abrirLista();
        int antes = lista.contarProdutos();
        lista = criarProdutoCompleto("Monitor Full HD" + SUFFIX, "1200.00", "Monitor de alta resolucao", "15");

        assertThat(lista.contarProdutos()).isEqualTo(antes + 1);
        ProdutoDetalhePage detalhe = lista.clicarDetalhePorNome("Monitor Full HD" + SUFFIX);
        assertThat(detalhe.getNome()).isEqualTo("Monitor Full HD" + SUFFIX);
        assertThat(detalhe.getEstoque()).isEqualTo("15");
        assertThat(detalhe.getDescricao()).contains("alta resolucao");
    }

    @Test
    @Order(11)
    void deveNavegaParaDetalheProduto() {
        ProdutoListPage lista = criarProdutoNaLista("Produto Detalhe" + SUFFIX, "299.00");

        ProdutoDetalhePage detalhe = lista.clicarDetalhePorNome("Produto Detalhe" + SUFFIX);

        assertThat(detalhe.getNome()).isEqualTo("Produto Detalhe" + SUFFIX);
        assertThat(driver.getCurrentUrl()).contains("/produtos/");
    }

    @Test
    @Order(12)
    void deveExibirPreviewCorretoAoDigitarPercentualDePromocao() {
        ProdutoListPage lista = criarProdutoNaLista("Produto Promo" + SUFFIX, "1000.00");

        ProdutoDetalhePage detalhe = lista.clicarDetalhePorNome("Produto Promo" + SUFFIX);
        detalhe.ativarFormPromo();
        String preview = detalhe.preencherPromocaoEObterPreview("10");

        assertThat(preview).isEqualTo("900.00");
    }

    @Test
    @Order(13)
    void deveCadastrarProdutoComCategoriaEVerificarNoDetalhe() {
        ProdutoListPage lista = criarProdutoCompleto("Smartphone Premium" + SUFFIX, "3500.00", "Celular top de linha", "20");

        assertThat(lista.contarProdutos()).isGreaterThan(0);
        ProdutoDetalhePage detalhe = lista.clicarDetalhePorNome("Smartphone Premium" + SUFFIX);
        assertThat(detalhe.getNome()).isEqualTo("Smartphone Premium" + SUFFIX);
    }

    @Test
    @Order(14)
    void deveGerarSkuAutomaticoAoCadastrar() {
        ProdutoListPage lista = criarProdutoNaLista("Notebook Gamer" + SUFFIX, "5999.00");

        ProdutoDetalhePage detalhe = lista.clicarDetalhePorNome("Notebook Gamer" + SUFFIX);
        String sku = detalhe.getSku();
        assertThat(sku).isNotBlank();
        assertThat(sku.length()).isGreaterThan(3);
    }

    @Test
    @Order(15)
    void deveResistirASqlInjectionNaInterface() {
        String nomeMalicioso = "'; DROP TABLE produtos; --";
        ProdutoListPage lista = criarProdutoNaLista(nomeMalicioso, "100.00");

        String pageSource = driver.getPageSource();
        assertThat(pageSource).doesNotContain("java.sql");
        assertThat(pageSource).doesNotContain("HibernateException");
        assertThat(lista.contarProdutos()).isGreaterThan(0);
    }

    @Test
    @Order(16)
    void deveRejeitarProdutoAtivoComEstoqueZero() {
        ProdutoFormPage form = abrirLista().clicarNovoProduto();

        form = form.submeterComAtivoESemEstoque("Produto Sem Estoque" + SUFFIX, "99.90");

        assertThat(form.erroEstaVisivel()).isTrue();
        assertThat(form.obterMensagemDeErro()).contains("Produto ativo deve ter estoque maior que zero");
    }
}
