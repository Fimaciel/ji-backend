package br.edu.ifro.ji.service;

import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.model.Contexto;
import br.edu.ifro.ji.model.ContextoParticipante;
import br.edu.ifro.ji.model.PapelParticipante;
import br.edu.ifro.ji.model.Usuario;
import br.edu.ifro.ji.repository.ContextoParticipanteRepository;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContextoAtivoService {

    private final ContextoParticipanteRepository participanteRepository;

    public ContextoAtivoService(ContextoParticipanteRepository participanteRepository) {
        this.participanteRepository = participanteRepository;
    }

    @Transactional(readOnly = true)
    public AcessoContexto obterParaLeitura(UsuarioPrincipal principal) {
        if (principal.contextoAtivoId() == null) {
            throw new RegraNegocioException(
                    HttpStatus.CONFLICT,
                    "contexto-ativo-ausente",
                    "Selecione um contexto antes de continuar."
            );
        }

        ContextoParticipante participante = participanteRepository
                .findByContextoIdAndUsuarioIdAndDataSaidaIsNull(
                        principal.contextoAtivoId(), principal.usuarioId())
                .orElseThrow(() -> new RegraNegocioException(
                        HttpStatus.FORBIDDEN,
                        "contexto-sem-acesso",
                        "Você não possui acesso ao contexto ativo."
                ));
        return new AcessoContexto(
                participante.getContexto(),
                participante.getUsuario(),
                participante.getPapel()
        );
    }

    @Transactional(readOnly = true)
    public AcessoContexto obterParaEscrita(UsuarioPrincipal principal) {
        AcessoContexto acesso = obterParaLeitura(principal);
        if (acesso.papel() == PapelParticipante.DEPENDENTE) {
            throw new RegraNegocioException(
                    HttpStatus.FORBIDDEN,
                    "contexto-somente-leitura",
                    "Seu papel permite apenas consultar os dados deste contexto."
            );
        }
        return acesso;
    }

    public record AcessoContexto(
            Contexto contexto,
            Usuario usuario,
            PapelParticipante papel
    ) {
    }
}
