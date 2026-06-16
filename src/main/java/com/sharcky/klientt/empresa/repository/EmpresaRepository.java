package com.sharcky.klientt.empresa.repository;

import com.sharcky.klientt.empresa.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    List<Empresa> findByCidadeContainingIgnoreCase(String cidade);

    List<Empresa> findByNomeContainingIgnoreCase(String nome);

    /** Identidade primária no upsert (dual-fonte): CNPJ (só dígitos). */
    Optional<Empresa> findFirstByCnpj(String cnpj);

    /** Fallback do upsert quando não há CNPJ (chave de cache: nome + cidade). */
    Optional<Empresa> findFirstByNomeIgnoreCaseAndCidadeIgnoreCase(String nome, String cidade);
}
