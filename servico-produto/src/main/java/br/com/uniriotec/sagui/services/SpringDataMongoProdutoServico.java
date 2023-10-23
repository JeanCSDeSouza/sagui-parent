package br.com.uniriotec.sagui.services;

import java.util.Locale;
import java.util.Optional;

import br.com.uniriotec.sagui.model.dto.ProdutoData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.MessageSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Service;

import br.com.uniriotec.sagui.model.Produto;
import br.com.uniriotec.sagui.model.dto.ProdutoForm;
import br.com.uniriotec.sagui.model.dto.ProdutoRepresentationAssembler;
import br.com.uniriotec.sagui.repository.ProdutoRepositorio;

@Service
@Slf4j
public class SpringDataMongoProdutoServico implements ProdutoServico {

	private final ProdutoRepositorio produtoRepositorio;
	private final ProdutoRepresentationAssembler produtoAssembler;
	private final MessageSource messageSource;
	@Autowired
	public SpringDataMongoProdutoServico(ProdutoRepositorio produtoRepositorio, ProdutoRepresentationAssembler produtoAssembler, MessageSource messageSource) {
		this.produtoRepositorio = produtoRepositorio;
		this.produtoAssembler = produtoAssembler;
		this.messageSource = messageSource;
	}

	/**
	 * Busca um produto pelo seu ID
	 * @param id Identificador de produto
	 * @return produto relacionado ao ID buscado
	 * @exception ProdutoNaoEncontradoException caso o produto não seja encontrado
	 */
	@Cacheable(value="produtoCache", key="#id")
	@Override
	public ProdutoData buscarPorId(String id) {
		log.info("Método foi chamado - get by id");
		Optional<Produto> produto = produtoRepositorio.findById(id);
		if( produto.isPresent() ) {
			return produtoAssembler.toModel( produto.get() );
		}else {
			throw new ProdutoNaoEncontradoException( messageSource.getMessage("api.erro.produto.nao.encontrado", null, Locale.getDefault())  );//
		}	
	}

	/**
	 * Busca todos os produtos no banco cuja propriedade ativo está marcada como true
	 * @return todos os produtos ativos
	 */
	@Override
	public CollectionModel<ProdutoData> buscarTodos() {
		return produtoAssembler.toCollectionModel(produtoRepositorio.findAll());
	}

	/**
	 * Retorna uma lista de produtos encapsulada em uma ProdutoRetornoPaginacao que inclui a
	 * pagina atual, o total de itens e o total de páginas.
	 * @param page número da página
	 * @param size quantidade de itens por página
	 * @return ProdutoRetornoPaginacao
	 */
	@Cacheable(value="produtoPaginadoCache")
	@Override
	public ProdutoRetornoPaginacao buscarTodosAtivosPaginavel(int page, int size){
		log.info("Método foi chamado - find by ativo");
		Pageable paging = PageRequest.of(page, size);
		
		Page<Produto> pageProduto;
		
		pageProduto = produtoRepositorio.findByAtivo(true, paging);
		
		return ProdutoRetornoPaginacao.builder()
				.produtos( produtoAssembler.toCollectionModel( pageProduto.getContent() ) )	
				.paginaAtual( pageProduto.getNumber() )
				.totalItems( pageProduto.getNumberOfElements() )
				.totalPaginas( pageProduto.getTotalPages() )
				.build();
	}

	/**
	 * Insere um produto no banco de dados.
	 * @param produto que será salvo no banco
	 * @return Retorna a representação do produto com sua ID
	 */
	@CacheEvict(value="produtoPaginadoCache", allEntries = true)
	@Override
	public ProdutoData salvar(ProdutoForm produto) {
		Produto persistir = Produto.builder()
				.nome( produto.getNome() )
				.descricao( produto.getDescricao() )
				.preco( produto.getPreco() )
				.tipo( produto.getTipo() )
				.imageUrl( produto.getImageUrl() )
				.ativo( produto.getAtivo() )
				.build();
			try {
				persistir.setId( produtoRepositorio.save(persistir).getId() );
				return produtoAssembler.toModel(persistir);
			}catch(DuplicateKeyException dke) {
				throw new BancoNaoModificadoException( messageSource.getMessage("api.erro.mongodb.chave.duplicada", null, Locale.getDefault()) );
			}		
	}

	/**
	 * Inativa um produto no banco de dados
	 * @param id do produto que será alterado
	 * @return retorna o produto atualizado
	 */
	@Override
	@Deprecated
	public ProdutoData inativar(String id) {
		Optional<Produto> produto = produtoRepositorio.findById(id);
		if( produto.isPresent() ) {
			produto.get().setAtivo(false); 
			return produtoAssembler.toModel( produtoRepositorio.save( produto.get() ) );
		}else {
			throw new ProdutoNaoEncontradoException( messageSource.getMessage("api.erro.produto.nao.encontrado", null, Locale.getDefault()) );//
		}
	}

	/**
	 * Altera o status de um produto pela sua ID ativando ou inativando sua visualiação.
	 * @param id identificador do produto que deve ser alterado
	 * @return Retorna a representação completa do produto que foi alterado.
	 */
	@Caching(evict = {
			@CacheEvict(value = "produtoCache", key = "#id"),
			@CacheEvict(value="produtoPaginadoCache", allEntries = true)
	})
	@Override
	public ProdutoData toogleStatus(String id) {
		Optional<Produto> produto = produtoRepositorio.findById(id);
		if( produto.isPresent() ) {
			produto.get().setAtivo( !produto.get().getAtivo() );
			return produtoAssembler.toModel( produtoRepositorio.save( produto.get() ) );
		}else {
			throw new ProdutoNaoEncontradoException( messageSource.getMessage("api.erro.produto.nao.encontrado", null, Locale.getDefault()) );//
		}
	}

	/**
	 * Altera um produto
	 * @param produtoForm representação do produto para ser alterado
	 * @return produto alterado
	 */

	@Caching(evict = {
			@CacheEvict(value = "produtoCache", key = "#produtoForm.id"),
			@CacheEvict(value="produtoPaginadoCache", allEntries = true)
	})
	@Override
	public ProdutoData alterar(ProdutoForm produtoForm) {
		Optional<Produto> produto = produtoRepositorio.findById( produtoForm.getId() );
		if(produto.isPresent()) {
			Produto persistido = produto.get();
			persistido.setNome(produtoForm.getNome() );
			persistido.setDescricao( produtoForm.getDescricao() );
			persistido.setPreco(produtoForm.getPreco());
			persistido.setTipo( produtoForm.getTipo());
			persistido.setImageUrl(produtoForm.getImageUrl() );
			persistido.setAtivo( produtoForm.getAtivo() );
			return produtoAssembler.toModel( produtoRepositorio.save(persistido) );
		}else {
			throw new ProdutoNaoEncontradoException( messageSource.getMessage("api.erro.produto.nao.encontrado", null, Locale.getDefault()) );
		}
	}
}
