package br.com.george.commerce.bff.dto;

import java.util.List;

/** Pirâmide olfativa: topo, coração e fundo. */
public record CatalogoNotasResponse(List<String> top, List<String> heart, List<String> base) {
}
