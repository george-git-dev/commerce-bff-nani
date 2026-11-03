package br.com.george.commerce.bff.service.impl;

import br.com.george.commerce.bff.client.CatalogoClient;
import br.com.george.commerce.bff.dto.CatalogoFiltrosResponse;
import br.com.george.commerce.bff.dto.CatalogoPaginaResponse;
import br.com.george.commerce.bff.dto.CatalogoProdutoResponse;
import br.com.george.commerce.bff.service.CatalogoService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

@Service
public class CatalogoServiceImpl implements CatalogoService {

    /** O que a vitrine da API entende. Qualquer outro parâmetro é ignorado (não vai para a API). */
    static final Set<String> PARAMETROS = Set.of("category", "family", "occasion", "gender", "brand", "minPrice",
            "maxPrice", "q", "inStock", "kind", "deal", "launch", "bestSeller", "badge", "sort", "page", "size");

    private final CatalogoClient catalogo;

    public CatalogoServiceImpl(CatalogoClient catalogo) {
        this.catalogo = catalogo;
    }

    @Override
    public CatalogoPaginaResponse buscar(MultiValueMap<String, String> parametros) {
        Map<String, String> filtros = new LinkedHashMap<>();
        parametros.forEach((nome, valores) -> {
            if (PARAMETROS.contains(nome) && valores != null) {
                List<String> preenchidos = valores.stream().filter(valor -> valor != null && !valor.isBlank()).toList();
                if (!preenchidos.isEmpty()) {
                    filtros.put(nome, String.join(",", preenchidos));
                }
            }
        });
        return catalogo.buscar(filtros);
    }

    @Override
    public CatalogoProdutoResponse buscarPorSlug(String slug) {
        return catalogo.buscarPorSlug(slug);
    }

    @Override
    public CatalogoFiltrosResponse filtros() {
        return catalogo.filtros();
    }
}
