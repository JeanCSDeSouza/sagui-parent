package br.com.uniriotec.sagui.services;

import br.com.uniriotec.sagui.model.dto.ProdutoData;
import org.springframework.hateoas.CollectionModel;

import br.com.uniriotec.sagui.model.dto.ProdutoForm;

public interface ProdutoServico {

	ProdutoData buscarPorId(String id);

	CollectionModel<ProdutoData> buscarTodos();

	ProdutoRetornoPaginacao buscarTodosAtivosPaginavel(int page, int size);

	ProdutoData salvar(ProdutoForm produto);

	ProdutoData inativar(String id);

	ProdutoData alterar(ProdutoForm produto);

}