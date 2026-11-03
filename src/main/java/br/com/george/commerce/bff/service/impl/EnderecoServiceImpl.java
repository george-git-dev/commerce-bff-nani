package br.com.george.commerce.bff.service.impl;

import br.com.george.commerce.bff.client.MinhaContaClient;
import br.com.george.commerce.bff.dto.EnderecoRequest;
import br.com.george.commerce.bff.dto.EnderecoResponse;
import br.com.george.commerce.bff.mapper.EnderecoMapper;
import br.com.george.commerce.bff.service.EnderecoService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EnderecoServiceImpl implements EnderecoService {

    private final MinhaContaClient minhaConta;
    private final EnderecoMapper mapper;

    public EnderecoServiceImpl(MinhaContaClient minhaConta, EnderecoMapper mapper) {
        this.minhaConta = minhaConta;
        this.mapper = mapper;
    }

    @Override
    public List<EnderecoResponse> listar() {
        return minhaConta.listarEnderecos().stream().map(mapper::toResponse).toList();
    }

    @Override
    public EnderecoResponse cadastrar(EnderecoRequest request) {
        return mapper.toResponse(minhaConta.cadastrarEndereco(mapper.toApi(request)));
    }

    @Override
    public EnderecoResponse atualizar(Long id, EnderecoRequest request) {
        return mapper.toResponse(minhaConta.atualizarEndereco(id, mapper.toApi(request)));
    }

    @Override
    public void remover(Long id) {
        minhaConta.removerEndereco(id);
    }
}
