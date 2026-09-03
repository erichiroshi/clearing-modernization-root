package br.com.erichiroshi.clearing.domain.repository;

import br.com.erichiroshi.clearing.domain.model.Comprador;

import java.util.Optional;

public interface CompradorRepository {

    Optional<Comprador> buscarPorId(String id);

    Comprador salvar(Comprador comprador);
}
