package br.com.uniriotec.sagui.services;

import java.util.Locale;
import java.util.Optional;

import br.com.uniriotec.sagui.model.dto.ProdutoData;
import br.com.uniriotec.sagui.model.dto.ProdutoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

	private ProdutoRepositorio produtoRepositorio;
	private ProdutoRepresentationAssembler produtoAssembler;
	private MessageSource messageSource;
	@Autowired
	public SpringDataMongoProdutoServico(ProdutoRepositorio produtoRepositorio, ProdutoRepresentationAssembler produtoAssembler, MessageSource messageSource) {
		this.produtoRepositorio = produtoRepositorio;
		this.produtoAssembler = produtoAssembler;
		this.messageSource = messageSource;
	}
	@Autowired
	private ProdutoMapper produtoMapper;

	/**
	 * Busca um produto pelo seu ID
	 * @param id Identificador de produto
	 * @return produto relacionado ao ID buscado
	 * @exception ProdutoNaoEncontradoException caso o produto não seja encontrado
	 */
//	@Cacheable(value="produtoCache", key="#id")
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
//	@Cacheable(value="produtoPaginadoCache")
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
	 * @param produto
	 * @return
	 */
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
	 * Alterna o status do Produto entre a ativo ou inativo
	 *
	 * @param id
	 * @return Produto com o status atualizado
	 */
	@Override
	public ProdutoData toogleProdutoStatus(String id) {
		Optional<Produto> produto = produtoRepositorio.findById(id);
		if( produto.isPresent() ) {
			produto.get().setAtivo(!produto.get().getAtivo());
			return produtoAssembler.toModel( produtoRepositorio.save( produto.get() ) );
		}else {
			throw new ProdutoNaoEncontradoException( messageSource.getMessage("api.erro.produto.nao.encontrado", null, Locale.getDefault()) );//
		}
	}


	/**
	 * Altera um produto
	 * @param produtoForm
	 * @return produto alterado
	 */
	@CacheEvict(value = "produtoCache", key = "#produtoForm.id")
	@Override
	public ProdutoData alterar(ProdutoForm produtoForm) {
		Optional<Produto> produto = produtoRepositorio.findById( produtoForm.getId() );
		if(produto.isPresent()) {
			Produto produtoAtualizado = produtoMapper.updateProduto(produtoForm, produto.get());
			return produtoAssembler.toModel( produtoRepositorio.save(produtoAtualizado) );
		}else {
			throw new ProdutoNaoEncontradoException( messageSource.getMessage("api.erro.produto.nao.encontrado", null, Locale.getDefault()) );
		}
	}
}
