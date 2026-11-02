package br.com.george.commerce.bff.service.impl;

import br.com.george.commerce.bff.client.MinhaContaClient;
import br.com.george.commerce.bff.dto.AtualizarPerfilApiRequest;
import br.com.george.commerce.bff.dto.AtualizarPerfilRequest;
import br.com.george.commerce.bff.dto.UsuarioApiResponse;
import br.com.george.commerce.bff.dto.MeResponse;
import br.com.george.commerce.bff.dto.PerfilResponse;
import br.com.george.commerce.bff.mapper.MeMapper;
import br.com.george.commerce.bff.service.PerfilService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class PerfilServiceImpl implements PerfilService {

    private final MinhaContaClient minhaConta;
    private final MeMapper meMapper;

    public PerfilServiceImpl(MinhaContaClient minhaConta, MeMapper meMapper) {
        this.minhaConta = minhaConta;
        this.meMapper = meMapper;
    }

    /** Nome e e-mail: o Keycloak é a fonte (é lá que o cliente edita). CPF e celular: a loja. */
    @Override
    public PerfilResponse buscar(OidcUser user) {
        return juntar(user, minhaConta.buscarPerfil());
    }

    @Override
    public PerfilResponse atualizar(OidcUser user, AtualizarPerfilRequest request) {
        UsuarioApiResponse loja = minhaConta.atualizarPerfil(new AtualizarPerfilApiRequest(request.phone(), request.cpf()));
        return juntar(user, loja);
    }

    private PerfilResponse juntar(OidcUser user, UsuarioApiResponse loja) {
        MeResponse identidade = meMapper.toResponse(user);
        return new PerfilResponse(identidade.name(), identidade.email(), loja.cpf(), loja.phone());
    }
}
