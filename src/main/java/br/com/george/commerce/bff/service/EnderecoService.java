package br.com.george.commerce.bff.service;

import br.com.george.commerce.bff.dto.EnderecoRequest;
import br.com.george.commerce.bff.dto.EnderecoResponse;
import java.util.List;

/** Minha conta → Endereços. Regras (principal, limite) são da commerce-api; aqui só traduz. */
public interface EnderecoService {

    /** Principal primeiro (a API já devolve ordenado). */
    List<EnderecoResponse> listar();

    EnderecoResponse cadastrar(EnderecoRequest request);

    EnderecoResponse atualizar(Long id, EnderecoRequest request);

    void remover(Long id);
}
