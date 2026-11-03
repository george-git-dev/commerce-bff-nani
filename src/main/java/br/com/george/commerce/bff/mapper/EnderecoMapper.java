package br.com.george.commerce.bff.mapper;

import br.com.george.commerce.bff.dto.EnderecoApiRequest;
import br.com.george.commerce.bff.dto.EnderecoApiResponse;
import br.com.george.commerce.bff.dto.EnderecoRequest;
import br.com.george.commerce.bff.dto.EnderecoResponse;
import org.springframework.stereotype.Component;

/** Traduz os nomes do front ({@code cep}, {@code district}, {@code isDefault}) e os da commerce-api. */
@Component
public class EnderecoMapper {

    public EnderecoApiRequest toApi(EnderecoRequest request) {
        return EnderecoApiRequest.builder()
                .street(request.street())
                .number(request.number())
                .complement(request.complement())
                .neighborhood(request.district())
                .city(request.city())
                .state(request.state())
                .zipCode(request.cep())
                .primaryAddress(Boolean.TRUE.equals(request.isDefault()))
                .build();
    }

    public EnderecoResponse toResponse(EnderecoApiResponse address) {
        return EnderecoResponse.builder()
                .id(address.id())
                .cep(address.zipCode())
                .street(address.street())
                .number(address.number())
                .complement(address.complement() == null ? "" : address.complement())
                .district(address.neighborhood())
                .city(address.city())
                .state(address.state())
                .isDefault(Boolean.TRUE.equals(address.primaryAddress()))
                .build();
    }
}
