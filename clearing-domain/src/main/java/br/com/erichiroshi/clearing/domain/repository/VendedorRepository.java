package br.com.erichiroshi.clearing.domain.repository;

import br.com.erichiroshi.clearing.domain.model.Vendedor;

import java.util.Optional;

public interface VendedorRepository {

    Optional<Vendedor> buscarPorId(String id);

    Vendedor salvar(Vendedor vendedor);
}
