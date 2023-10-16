package br.com.uniriotec.sagui.model.dto;

import br.com.uniriotec.sagui.model.Produto;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Interface para realizar mapeamento de Produto para atualizacao. Assim, nao sera preciso
 * enviar todos os dados do produto a ser alterado. Apenas os que serao alterados.
 */
@Mapper(componentModel = "spring")
public interface ProdutoMapper {

    /**
     * Mapeia os campos nao nulos de um objeto para um objeto alvo. Ou seja, atualiza o objeto alvo
     * apenas com os campos do primeiro objeto que nao sao nulos.
     *
     * @param produtoForm
     * @param produto
     * @return Produto
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public Produto updateProduto(ProdutoForm produtoForm, @MappingTarget Produto produto);
}
