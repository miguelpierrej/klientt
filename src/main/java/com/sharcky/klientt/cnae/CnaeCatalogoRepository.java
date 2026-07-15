package com.sharcky.klientt.cnae;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CnaeCatalogoRepository extends JpaRepository<CnaeCatalogo, String> {

    /** Sugestões de atividade por descrição (autocomplete do "O que procura?"). */
    List<CnaeCatalogo> findTop8ByDescricaoContainingIgnoreCaseOrderByDescricaoAsc(String descricao);
}
