package br.com.infnet.produto.service;

import br.com.infnet.produto.domain.CategoriaProduto;
import br.com.infnet.produto.domain.Produto;
import br.com.infnet.produto.domain.Quantidade;
import br.com.infnet.produto.domain.Sku;
import br.com.infnet.produto.domain.TipoOperacaoEstoque;
import br.com.infnet.produto.domain.exception.ProdutoNaoEncontradoException;
import br.com.infnet.produto.dto.ProdutoRequest;
import br.com.infnet.produto.dto.ProdutoResponse;
import br.com.infnet.produto.mapper.ProdutoMapper;
import br.com.infnet.produto.repository.ProdutoRepository;
import br.com.infnet.shared.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository repository;

    @Mock
    private ProdutoMapper mapper;

    @InjectMocks
    private ProdutoService service;

    @ParameterizedTest
    @CsvSource({
        "Monitor 4K,        2500.00",
        "Teclado Mecanico,   350.00",
        "Mouse Gamer,        199.90",
        "Webcam HD,           89.90",
        "SSD 1TB,            450.00"
    })
    void deveCadastrarProdutosComSkuAutoGerado(String nome, BigDecimal preco) {
        Produto salvo = Produto.novo(nome.trim(), Sku.de("AUTO-XXXX"), preco);
        when(repository.save(any(Produto.class))).thenReturn(salvo);

        Produto resultado = service.cadastrar(nome.trim(), preco, null, null, CategoriaProduto.GERAL, null);

        assertThat(resultado.getNome()).isEqualTo(nome.trim());
        assertThat(resultado.getPreco().quantia()).isEqualByComparingTo(preco);
        verify(repository).save(any(Produto.class));
    }

    @Test
    void deveCadastrarComCategoria() {
        Produto salvo = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));
        salvo.definirCategoria(CategoriaProduto.MONITORES);
        when(repository.save(any(Produto.class))).thenReturn(salvo);

        Produto resultado = service.cadastrar("Monitor", new BigDecimal("1000.00"), null, null, CategoriaProduto.MONITORES, null);

        assertThat(resultado.getCategoria()).isEqualTo(CategoriaProduto.MONITORES);
    }

    @Test
    void deveCadastrarComDescricaoEEstoque() {
        Produto salvo = Produto.novo("Monitor 4K", Sku.de("MON-4K-XXXX"), new BigDecimal("2500.00"));
        salvo.definirDescricao("Ótimo monitor");
        salvo.definirEstoque(Quantidade.de(5));
        when(repository.save(any(Produto.class))).thenReturn(salvo);

        Produto resultado = service.cadastrar("Monitor 4K", new BigDecimal("2500.00"),
                "Ótimo monitor", 5, CategoriaProduto.MONITORES, null);

        assertThat(resultado.getDescricao()).isEqualTo("Ótimo monitor");
        assertThat(resultado.getEstoque().inteiro()).isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "-1.0", "-100.0", "-999.99"})
    void deveRejeitarPrecosNegativos(String precoStr) {
        BigDecimal preco = new BigDecimal(precoStr);
        assertThatThrownBy(() -> service.cadastrar("Produto X", preco, null, null, CategoriaProduto.GERAL, null))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Preco deve ser maior que zero");

        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @CsvSource({
        "Monitor Atualizado, 3000.00",
        "Notebook Gamer,     7500.00",
        "Headset Pro,         299.90"
    })
    void deveAtualizarProdutoExistente(String novoNome, BigDecimal novoPreco) {
        UUID id = UUID.randomUUID();
        Produto existente = Produto.novo("Nome Antigo", Sku.de("SKU-OLD"), new BigDecimal("100.0"));
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));

        Produto atualizado = service.atualizar(id, novoNome, novoPreco, null, null, null, null, null);

        assertThat(atualizado.getNome()).isEqualTo(novoNome);
        assertThat(atualizado.getPreco().quantia()).isEqualByComparingTo(novoPreco);
    }

    @Test
    void deveManterSkuExistenteNoUpdate() {
        UUID id = UUID.randomUUID();
        Produto existente = Produto.novo("Monitor", Sku.de("SKU-EXISTENTE"), new BigDecimal("100.0"));
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));

        Produto atualizado = service.atualizar(id, "Monitor Pro", new BigDecimal("200.0"), null, null, null, null, null);

        assertThat(atualizado.getSku().codigo()).isEqualTo("SKU-EXISTENTE");
    }

    @Test
    void deveAtualizarTodosOsCampos() {
        UUID id = UUID.randomUUID();
        Produto existente = Produto.novo("Monitor", Sku.de("SKU-OLD"), new BigDecimal("100.0"));
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));

        Produto atualizado = service.atualizar(id, "Monitor Pro", new BigDecimal("3000.00"),
                "Nova descrição", 10, false, CategoriaProduto.MONITORES, null);

        assertThat(atualizado.getNome()).isEqualTo("Monitor Pro");
        assertThat(atualizado.getDescricao()).isEqualTo("Nova descrição");
        assertThat(atualizado.getEstoque().inteiro()).isEqualTo(10);
        assertThat(atualizado.getAtivo()).isFalse();
        assertThat(atualizado.getCategoria()).isEqualTo(CategoriaProduto.MONITORES);
    }

    @Test
    void deveLancarExcecaoAoBuscarIdInexistente() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
                .isInstanceOf(ProdutoNaoEncontradoException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void deveRemoverProdutoExistente() {
        Produto produto = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("2500.0"));
        UUID id = produto.getId();
        when(repository.findById(id)).thenReturn(Optional.of(produto));

        service.remover(id);

        verify(repository).deleteById(id);
    }

    @Test
    void deveLancarExcecaoAoRemoverIdInexistente() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remover(id))
                .isInstanceOf(ProdutoNaoEncontradoException.class);

        verify(repository, never()).deleteById(any());
    }

    @Test
    void deveRetornarListaCompleta() {
        List<Produto> produtos = List.of(
                Produto.novo("Mouse",    Sku.de("MOU-001"), new BigDecimal("100.0")),
                Produto.novo("Teclado",  Sku.de("TEC-001"), new BigDecimal("200.0")),
                Produto.novo("Monitor",  Sku.de("MON-001"), new BigDecimal("1500.0"))
        );
        when(repository.findAll()).thenReturn(produtos);

        assertThat(service.listarTodos()).hasSize(3);
        verify(repository).findAll();
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaProdutos() {
        when(repository.findAll()).thenReturn(List.of());
        assertThat(service.listarTodos()).isEmpty();
    }

    @Test
    void deveCriarDTOComSucesso() {
        ProdutoRequest request = new ProdutoRequest(
                "Monitor 4K", "Monitor 4K UHD",
                new BigDecimal("2500.00"), 10, true, null, CategoriaProduto.MONITORES, null);
        Produto salvo = Produto.novo("Monitor 4K", Sku.de("MON-4K-XXXX"), new BigDecimal("2500.00"));
        ProdutoResponse response = ProdutoResponse.builder().nome("Monitor 4K").build();

        when(repository.existsByNomeIgnoreCase(request.getNome())).thenReturn(false);
        when(repository.existsBySku(any())).thenReturn(false);
        when(repository.save(any(Produto.class))).thenReturn(salvo);
        when(mapper.toResponse(any(Produto.class))).thenReturn(response);

        ProdutoResponse resultado = service.criarDTO(request);

        assertThat(resultado.getNome()).isEqualTo("Monitor 4K");
        verify(repository).save(any(Produto.class));
    }

    @Test
    void deveLancarExcecaoAoCriarDTOComNomeDuplicado() {
        ProdutoRequest request = new ProdutoRequest(
                "Monitor 4K", null, new BigDecimal("2500.00"), 10, true, null, CategoriaProduto.GERAL, null);
        when(repository.existsByNomeIgnoreCase(request.getNome())).thenReturn(true);

        assertThatThrownBy(() -> service.criarDTO(request))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("nome já existe");

        verify(repository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoCriarDTOComSkuDuplicado() {
        ProdutoRequest request = new ProdutoRequest(
                "Monitor 4K", null, new BigDecimal("2500.00"), 10, true, null, CategoriaProduto.GERAL, null);
        when(repository.existsByNomeIgnoreCase(request.getNome())).thenReturn(false);
        when(repository.existsBySku(any())).thenReturn(true);

        assertThatThrownBy(() -> service.criarDTO(request))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("SKU já cadastrado");

        verify(repository, never()).save(any());
    }

    @Test
    void deveBuscarPorSkuRetornarResponse() {
        String sku = "MON-4K-ABCD";
        Produto produto = Produto.novo("Monitor 4K", Sku.de(sku), new BigDecimal("2500.00"));
        ProdutoResponse response = ProdutoResponse.builder().nome("Monitor 4K").sku(sku).build();

        when(repository.findBySku(Sku.de(sku))).thenReturn(Optional.of(produto));
        when(mapper.toResponse(produto)).thenReturn(response);

        ProdutoResponse resultado = service.buscarPorSku(sku);

        assertThat(resultado.getSku()).isEqualTo(sku);
        assertThat(resultado.getNome()).isEqualTo("Monitor 4K");
    }

    @Test
    void deveBuscarPorSkuLancarExcecaoQuandoNaoEncontrado() {
        String sku = "SKU-INVALIDO";
        when(repository.findBySku(Sku.de(sku))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorSku(sku))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining(sku);
    }

    @Test
    void deveAjustarEstoqueEntrada() {
        Produto produto = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("2500.00"));
        produto.definirEstoque(Quantidade.de(10));
        UUID id = produto.getId();
        ProdutoResponse response = ProdutoResponse.builder().estoque(60).build();

        when(repository.findById(id)).thenReturn(Optional.of(produto));
        when(repository.save(any(Produto.class))).thenReturn(produto);
        when(mapper.toResponse(any(Produto.class))).thenReturn(response);

        ProdutoResponse resultado = service.ajustarEstoque(id, TipoOperacaoEstoque.ENTRADA, 50);

        assertThat(resultado.getEstoque()).isEqualTo(60);
    }

    @Test
    void deveLancarExcecaoAoAjustarEstoqueInsuficiente() {
        Produto produto = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("2500.00"));
        produto.definirEstoque(Quantidade.de(5));
        UUID id = produto.getId();

        when(repository.findById(id)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> service.ajustarEstoque(id, TipoOperacaoEstoque.SAIDA, 10))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Estoque insuficiente");
    }

    @Test
    void deveAjustarEstoqueSaidaComSucesso() {
        Produto produto = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("2500.00"));
        produto.definirEstoque(Quantidade.de(20));
        UUID id = produto.getId();
        ProdutoResponse response = ProdutoResponse.builder().estoque(10).build();

        when(repository.findById(id)).thenReturn(Optional.of(produto));
        when(repository.save(any(Produto.class))).thenReturn(produto);
        when(mapper.toResponse(any(Produto.class))).thenReturn(response);

        ProdutoResponse resultado = service.ajustarEstoque(id, TipoOperacaoEstoque.SAIDA, 10);

        assertThat(resultado.getEstoque()).isEqualTo(10);
    }

    @Test
    void deveLancarExcecaoQuantidadeInvalida() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.ajustarEstoque(id, TipoOperacaoEstoque.ENTRADA, 0))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Quantidade deve ser maior que zero");
    }

    @Test
    void deveRetornarBuscarDTO() {
        Produto produto = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("2500.00"));
        UUID id = produto.getId();
        ProdutoResponse response = ProdutoResponse.builder().id(id).nome("Monitor").build();

        when(repository.findById(id)).thenReturn(Optional.of(produto));
        when(mapper.toResponse(produto)).thenReturn(response);

        ProdutoResponse resultado = service.buscarDTO(id);

        assertThat(resultado.getId()).isEqualTo(id);
        assertThat(resultado.getNome()).isEqualTo("Monitor");
    }

    @Test
    void deveRetornarPaginaAtivos() {
        Produto produto = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("2500.00"));
        ProdutoResponse response = ProdutoResponse.builder().nome("Monitor").build();
        Pageable pageable = PageRequest.of(0, 20);

        when(repository.findAllByAtivo(eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(produto)));
        when(mapper.toResponse(any(Produto.class))).thenReturn(response);

        Page<ProdutoResponse> pagina = service.listar(pageable);

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).getNome()).isEqualTo("Monitor");
    }

    @Test
    void deveAtualizarDTOComSucesso() {
        UUID id = UUID.randomUUID();
        ProdutoRequest request = new ProdutoRequest(
                "Monitor Pro", null, new BigDecimal("3000.00"), 5, true, null, null, null);
        Produto produto = Produto.novo("Monitor", Sku.de("MON-XXXX"), new BigDecimal("2500.00"));
        ProdutoResponse response = ProdutoResponse.builder().nome("Monitor Pro").build();

        when(repository.existsByNomeIgnoreCaseAndIdNot(request.getNome(), id)).thenReturn(false);
        when(repository.findById(id)).thenReturn(Optional.of(produto));
        when(repository.save(any(Produto.class))).thenReturn(produto);
        when(mapper.toResponse(any(Produto.class))).thenReturn(response);

        ProdutoResponse resultado = service.atualizarDTO(id, request);

        assertThat(resultado.getNome()).isEqualTo("Monitor Pro");
    }

    @Test
    void deveLancarExcecaoAoAtualizarDTOComNomeDuplicado() {
        UUID id = UUID.randomUUID();
        Produto existente = Produto.novo("Monitor", Sku.de("MON-XXXX"), new BigDecimal("2500.00"));
        ProdutoRequest request = new ProdutoRequest(
                "Monitor Pro", null, new BigDecimal("3000.00"), 5, true, null, null, null);

        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.existsByNomeIgnoreCaseAndIdNot(request.getNome(), id)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizarDTO(id, request))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("nome já existe");

        verify(repository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoRemoverProdutoComEstoque() {
        Produto produto = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("2500.0"));
        produto.definirEstoque(Quantidade.de(5));
        UUID id = produto.getId();
        when(repository.findById(id)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> service.remover(id))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("estoque");

        verify(repository, never()).deleteById(any());
    }

    @Test
    void deveLancarExcecaoAoCriarDTOComProdutoAtivoSemEstoque() {
        ProdutoRequest request = new ProdutoRequest(
                "Monitor Ativo", null, new BigDecimal("2500.00"), 0, true, null, CategoriaProduto.GERAL, null);
        when(repository.existsByNomeIgnoreCase(request.getNome())).thenReturn(false);
        when(repository.existsBySku(any())).thenReturn(false);

        assertThatThrownBy(() -> service.criarDTO(request))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("estoque");

        verify(repository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoAtualizarDTOComProdutoAtivoSemEstoque() {
        UUID id = UUID.randomUUID();
        ProdutoRequest request = new ProdutoRequest(
                "Monitor Pro", null, new BigDecimal("3000.00"), 0, true, null, null, null);
        Produto produto = Produto.novo("Monitor", Sku.de("MON-XXXX"), new BigDecimal("2500.00"));
        produto.definirEstoque(Quantidade.de(0));

        when(repository.findById(id)).thenReturn(Optional.of(produto));
        when(repository.existsByNomeIgnoreCaseAndIdNot(request.getNome(), id)).thenReturn(false);

        assertThatThrownBy(() -> service.atualizarDTO(id, request))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("estoque");

        verify(repository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoSaidaParaProdutoInativo() {
        Produto produto = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("2500.00"));
        produto.desativar();
        produto.definirEstoque(Quantidade.de(10));
        UUID id = produto.getId();
        when(repository.findById(id)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> service.ajustarEstoque(id, TipoOperacaoEstoque.SAIDA, 5))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void deveDesativarProdutoQuandoEstoqueCaiAbaixoDoMinimo() {
        Produto produto = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("2500.00"));
        produto.definirEstoque(Quantidade.de(5));
        produto.definirEstoqueMinimo(Quantidade.de(5));
        UUID id = produto.getId();
        ProdutoResponse response = ProdutoResponse.builder().ativo(false).build();

        when(repository.findById(id)).thenReturn(Optional.of(produto));
        when(repository.save(any(Produto.class))).thenReturn(produto);
        when(mapper.toResponse(any(Produto.class))).thenReturn(response);

        service.ajustarEstoque(id, TipoOperacaoEstoque.SAIDA, 5);

        assertThat(produto.getAtivo()).isFalse();
    }

    @Test
    void deveReativarProdutoAoReceberEstoqueEntrada() {
        Produto produto = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("2500.00"));
        produto.desativar();
        produto.definirEstoque(Quantidade.de(0));
        produto.definirEstoqueMinimo(Quantidade.de(0));
        UUID id = produto.getId();
        ProdutoResponse response = ProdutoResponse.builder().ativo(true).build();

        when(repository.findById(id)).thenReturn(Optional.of(produto));
        when(repository.save(any(Produto.class))).thenReturn(produto);
        when(mapper.toResponse(any(Produto.class))).thenReturn(response);

        service.ajustarEstoque(id, TipoOperacaoEstoque.ENTRADA, 10);

        assertThat(produto.getAtivo()).isTrue();
    }

    @Test
    void deveAtivarPromocaoComSucesso() {
        Produto produto = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));
        UUID id = produto.getId();
        ProdutoResponse response = ProdutoResponse.builder().precoComDesconto(new BigDecimal("800.00")).build();

        when(repository.findById(id)).thenReturn(Optional.of(produto));
        when(repository.save(any(Produto.class))).thenReturn(produto);
        when(mapper.toResponse(any(Produto.class))).thenReturn(response);

        ProdutoResponse resultado = service.ativarPromocao(id, new BigDecimal("20"), null, null);

        assertThat(resultado.getPrecoComDesconto()).isEqualByComparingTo("800.00");
        verify(repository).save(any(Produto.class));
    }

    @Test
    void deveEncerrarPromocaoComSucesso() {
        Produto produto = Produto.novo("Monitor", Sku.de("MON-001"), new BigDecimal("1000.00"));
        produto.ativarPromocao(new BigDecimal("20"), null, null);
        UUID id = produto.getId();
        ProdutoResponse response = ProdutoResponse.builder().build();

        when(repository.findById(id)).thenReturn(Optional.of(produto));
        when(repository.save(any(Produto.class))).thenReturn(produto);
        when(mapper.toResponse(any(Produto.class))).thenReturn(response);

        ProdutoResponse resultado = service.encerrarPromocao(id);

        assertThat(resultado.getPrecoComDesconto()).isNull();
    }
}
