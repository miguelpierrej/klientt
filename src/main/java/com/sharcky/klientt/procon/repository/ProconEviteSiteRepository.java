package com.sharcky.klientt.procon.repository;

import com.sharcky.klientt.procon.model.ProconEviteSite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProconEviteSiteRepository extends JpaRepository<ProconEviteSite, Long> {

    Optional<ProconEviteSite> findByDominio(String dominio);
}
