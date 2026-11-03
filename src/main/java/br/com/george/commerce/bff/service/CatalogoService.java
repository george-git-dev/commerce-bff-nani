package br.com.george.commerce.bff.service;

import br.com.george.commerce.bff.dto.CatalogoFiltrosResponse;
import br.com.george.commerce.bff.dto.CatalogoPaginaResponse;
import br.com.george.commerce.bff.dto.CatalogoProdutoResponse;
import org.springframework.util.MultiValueMap;

/** Vitrine da loja. Filtro, ordem e página são da commerce-api; aqui só repassa. */
public interface CatalogoService {

    /** Só os parâmetros conhecidos seguem para a API (lista fechada). */
    CatalogoPaginaResponse buscar(MultiValueMap<String, String> parametros);

    CatalogoProdutoResponse buscarPorSlug(String slug);

    CatalogoFiltrosResponse filtros();
}
