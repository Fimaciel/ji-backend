package br.edu.ifro.ji.repository;

import br.edu.ifro.ji.model.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {
}
