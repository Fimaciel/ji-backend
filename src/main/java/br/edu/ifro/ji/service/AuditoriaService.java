package br.edu.ifro.ji.service;

import br.edu.ifro.ji.model.Contexto;
import br.edu.ifro.ji.model.LogAuditoria;
import br.edu.ifro.ji.model.Usuario;
import br.edu.ifro.ji.repository.LogAuditoriaRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class AuditoriaService {

    private final LogAuditoriaRepository repository;
    private final Clock clock;

    public AuditoriaService(LogAuditoriaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void registrar(Usuario usuario, Contexto contexto, String acao,
                          String entidade, Long registroId, String detalhes) {
        repository.save(new LogAuditoria(
                usuario,
                contexto,
                acao,
                entidade,
                registroId,
                Instant.now(clock),
                detalhes
        ));
    }
}
