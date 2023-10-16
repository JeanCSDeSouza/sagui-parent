package br.com.uniriotec.sagui.controller;

import br.com.uniriotec.sagui.model.dto.ProdutoData;
import br.com.uniriotec.sagui.services.ProdutoServico;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.uniriotec.sagui.model.dto.ProdutoForm;
import br.com.uniriotec.sagui.services.ProdutoRetornoPaginacao;

@RestController
@RequestMapping("/produtos")
public class ProdutoControlador {

	private ProdutoServico produtoServico;


	@Autowired
	public ProdutoControlador(ProdutoServico produtoServico) {
		super();
		this.produtoServico = produtoServico;
	}

	/**
	 * Retorna todos os produtos, ativos e inativos.
	 * @return CollectionModel<ProdutoData> com todos os produtos diponíveis.
	 */
	@GetMapping("/admin")
	@ResponseStatus(HttpStatus.OK)
	public CollectionModel<ProdutoData> produtos() {
		return produtoServico.buscarTodos();
	}

	/**
	 * Retorna os produtos ativos de forma paginada. Retorna todos os itens ativos caso não seja especificado a página e o tamanho.
	 * @param page inteiro que determina a pagina que é pra ser retornada
	 * @param size define quantidade de itens por página e a quantidade de páginas.
	 * @return
	 */
	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public ProdutoRetornoPaginacao produtosPaginados(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size){
		return produtoServico.buscarTodosAtivosPaginavel(page, size);
	}
	/**
	 * Busca um produto que tenha o identificador igual ao id passado. Caso não encontre um produto com a id que foi passada levanta uma exceção do tipo "ProdutoNaoEncontrado"
	 * @param id identificador de produto
	 * @return
	 */
	@GetMapping("/{id}")
	@ResponseStatus(HttpStatus.OK)
	public ProdutoData produto(@PathVariable(value="id") String id){
		return produtoServico.buscarPorId(id);
	}
	/**
	 * Salva um produto no banco. Caso a validação dos campos de produto deem errado retorna uma exceção "BancoDeDadosNaoModificado"
	 * @param produto
	 * @return
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProdutoData inserirProduto(@RequestBody @Validated ProdutoForm produto){
		return produtoServico.salvar(produto);
	}
	/**
	 * Altera um produto que tenha o identificador passado. Caso não encontre o produto levanta um exceção do tipo "ProdutoNaoEncontrado".
	 * Ou se o form tiver erros de validação levanta uma exceção do tipo "MethodArgumentNotValidException"
	 * @param produto
	 * @return
	 */
	@PutMapping
	@ResponseStatus(HttpStatus.OK)
	public ProdutoData alterarProduto(@RequestBody @Validated ProdutoForm produto){
		return produtoServico.alterar(produto);
	}

	/**
	 * Alterna o status de um produto que tenha o identificador passado.
	 * Caso não encontre o produto levanta uma exceção do tipo "ProdutoNaoEncontrado".
	 * @param id
	 * @return
	 */
	@PatchMapping("/toggle-status-produto/{id}")
	@ResponseStatus(HttpStatus.OK)
	public ProdutoData toogleStatusProduto(@PathVariable(value = "id") String id) {
		return produtoServico.toogleProdutoStatus(id);
	}


}
