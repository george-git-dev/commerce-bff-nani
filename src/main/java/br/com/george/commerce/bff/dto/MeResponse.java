package br.com.george.commerce.bff.dto;

import lombok.Builder;
import java.util.List;

/**
 * Quem está logado. Papéis e permissões iguais aos do front
 * (commerce-web, core/config/permissions.ts). Nunca inclui token.
 */
@Builder
public record MeResponse(String id, String name, String email, List<String> roles, List<String> permissions) {
}
