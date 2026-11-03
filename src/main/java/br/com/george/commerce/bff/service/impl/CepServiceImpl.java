package br.com.george.commerce.bff.service.impl;

import lombok.extern.slf4j.Slf4j;
import br.com.george.commerce.bff.client.BrasilApiCepClient;
import br.com.george.commerce.bff.client.ViaCepClient;
import br.com.george.commerce.bff.dto.BrasilApiCepApiResponse;
import br.com.george.commerce.bff.dto.CepResponse;
import br.com.george.commerce.bff.dto.ViaCepApiResponse;
import br.com.george.commerce.bff.exception.CepIndisponivelException;
import br.com.george.commerce.bff.exception.CepInvalidoException;
import br.com.george.commerce.bff.exception.CepNaoEncontradoException;
import br.com.george.commerce.bff.service.CepService;
import feign.FeignException;
import org.springframework.stereotype.Service;

/**
 * ViaCEP primeiro; se ele falhar (fora do ar, lento, erro 5xx), BrasilAPI. CEP inexistente não
 * tenta o segundo (resposta certa, não falha).
 */
@Service
@Slf4j
public class CepServiceImpl implements CepService {

    private final ViaCepClient viaCepClient;
    private final BrasilApiCepClient brasilApiCepClient;

    public CepServiceImpl(ViaCepClient viaCepClient, BrasilApiCepClient brasilApiCepClient) {
        this.viaCepClient = viaCepClient;
        this.brasilApiCepClient = brasilApiCepClient;
    }

    @Override
    public CepResponse buscar(String cep) {
        String digits = cep.replaceAll("\\D", "");
        if (digits.length() != 8) {
            throw new CepInvalidoException();
        }
        try {
            return doViaCep(digits);
        } catch (FeignException viaCepFailed) {
            log.warn("ViaCEP falhou, tentando a BrasilAPI: {}", viaCepFailed.getMessage());
        }
        try {
            return doBrasilApi(digits);
        } catch (FeignException.NotFound notFound) {
            throw new CepNaoEncontradoException();
        } catch (FeignException brasilApiFailed) {
            log.warn("BrasilAPI também falhou: {}", brasilApiFailed.getMessage());
            throw new CepIndisponivelException();
        }
    }

    private CepResponse doViaCep(String cep) {
        ViaCepApiResponse viaCepApiResponse = viaCepClient.buscar(cep);
        if (viaCepApiResponse == null || viaCepApiResponse.erro() != null) {
            throw new CepNaoEncontradoException();
        }
        return CepResponse.builder()
                .cep(cep)
                .street(text(viaCepApiResponse.logradouro()))
                .district(text(viaCepApiResponse.bairro()))
                .city(text(viaCepApiResponse.localidade()))
                .state(text(viaCepApiResponse.uf()))
                .build();
    }

    private CepResponse doBrasilApi(String cep) {
        BrasilApiCepApiResponse found = brasilApiCepClient.buscar(cep);
        if (found == null) {
            throw new CepNaoEncontradoException();
        }
        return CepResponse.builder()
                .cep(cep)
                .street(text(found.street()))
                .district(text(found.neighborhood()))
                .city(text(found.city()))
                .state(text(found.state()))
                .build();
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }
}
