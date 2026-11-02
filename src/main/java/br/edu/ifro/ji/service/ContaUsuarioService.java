package br.edu.ifro.ji.service;

import br.edu.ifro.ji.dto.AtualizacaoConfiguracaoRequest;
import br.edu.ifro.ji.dto.AtualizacaoPerfilRequest;
import br.edu.ifro.ji.dto.ConfiguracaoUsuarioResponse;
import br.edu.ifro.ji.dto.ContaUsuarioResponse;
import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.model.ConfiguracaoUsuario;
import br.edu.ifro.ji.model.Tema;
import br.edu.ifro.ji.model.Usuario;
import br.edu.ifro.ji.repository.ConfiguracaoUsuarioRepository;
import br.edu.ifro.ji.repository.SessaoRepository;
import br.edu.ifro.ji.repository.TokenRedefinicaoSenhaRepository;
import br.edu.ifro.ji.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

@Service
public class ContaUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ConfiguracaoUsuarioRepository configuracaoRepository;
    private final SessaoRepository sessaoRepository;
    private final TokenRedefinicaoSenhaRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public ContaUsuarioService(UsuarioRepository usuarioRepository,
                               ConfiguracaoUsuarioRepository configuracaoRepository,
                               SessaoRepository sessaoRepository,
                               TokenRedefinicaoSenhaRepository tokenRepository,
                               PasswordEncoder passwordEncoder,
                               AuditoriaService auditoriaService,
                               Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.configuracaoRepository = configuracaoRepository;
        this.sessaoRepository = sessaoRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ContaUsuarioResponse obter(Long usuarioId) {
        Usuario usuario = buscarUsuarioAtivo(usuarioId);
        return ContaUsuarioResponse.de(usuario, buscarConfiguracao(usuarioId));
    }

    @Transactional
    public ContaUsuarioResponse atualizarPerfil(Long usuarioId, AtualizacaoPerfilRequest request) {
        Usuario usuario = buscarUsuarioAtivo(usuarioId);
        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenha())) {
            throw new RegraNegocioException(
                    HttpStatus.BAD_REQUEST,
                    "senha-atual-invalida",
                    "A senha atual informada não confere."
            );
        }

        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCaseAndDataExclusaoIsNullAndIdNot(
                email, usuarioId)) {
            throw new RegraNegocioException(
                    HttpStatus.CONFLICT,
                    "email-em-uso",
                    "O e-mail informado já está em uso."
            );
        }

        validarNovaSenha(request);
        String senhaCodificada = request.novaSenha() == null
                ? null
                : passwordEncoder.encode(request.novaSenha());
        Instant agora = Instant.now(clock);
        usuario.atualizarPerfil(
                request.nome(),
                request.sobrenome(),
                request.dataNascimento(),
                email,
                senhaCodificada,
                agora
        );
        auditoriaService.registrar(
                usuario,
                null,
                "ATUALIZACAO_PERFIL",
                "USUARIO",
                usuario.getId(),
                request.novaSenha() == null
                        ? "Dados do perfil atualizados."
                        : "Dados do perfil e senha atualizados."
        );
        return ContaUsuarioResponse.de(usuario, buscarConfiguracao(usuarioId));
    }

    @Transactional
    public ConfiguracaoUsuarioResponse atualizarConfiguracao(
            Long usuarioId, AtualizacaoConfiguracaoRequest request) {
        Usuario usuario = buscarUsuarioAtivo(usuarioId);
        ConfiguracaoUsuario configuracao = buscarConfiguracao(usuarioId);
        configuracao.atualizar(request.salarioMensal(), request.notificacoesAtivas());
        usuario.registrarAtualizacao(Instant.now(clock));
        auditoriaService.registrar(
                usuario,
                null,
                "ATUALIZACAO_CONFIGURACAO",
                "CONFIGURACAO_USUARIO",
                configuracao.getId(),
                "Preferências financeiras e de notificação atualizadas."
        );
        return ConfiguracaoUsuarioResponse.de(configuracao);
    }

    @Transactional
    public ConfiguracaoUsuarioResponse atualizarTema(Long usuarioId, Tema tema) {
        Usuario usuario = buscarUsuarioAtivo(usuarioId);
        ConfiguracaoUsuario configuracao = buscarConfiguracao(usuarioId);
        configuracao.alterarTema(tema);
        usuario.registrarAtualizacao(Instant.now(clock));
        auditoriaService.registrar(
                usuario,
                null,
                "ALTERACAO_TEMA",
                "CONFIGURACAO_USUARIO",
                configuracao.getId(),
                "Tema alterado para " + tema.name() + "."
        );
        return ConfiguracaoUsuarioResponse.de(configuracao);
    }

    @Transactional
    public ConfiguracaoUsuarioResponse restaurarConfiguracao(Long usuarioId) {
        Usuario usuario = buscarUsuarioAtivo(usuarioId);
        ConfiguracaoUsuario configuracao = buscarConfiguracao(usuarioId);
        configuracao.restaurarPadrao();
        usuario.registrarAtualizacao(Instant.now(clock));
        auditoriaService.registrar(
                usuario,
                null,
                "RESTAURACAO_CONFIGURACAO",
                "CONFIGURACAO_USUARIO",
                configuracao.getId(),
                "Configurações restauradas para os valores padrão."
        );
        return ConfiguracaoUsuarioResponse.de(configuracao);
    }

    @Transactional
    public void encerrarConta(Long usuarioId) {
        Usuario usuario = buscarUsuarioAtivo(usuarioId);
        Instant agora = Instant.now(clock);
        auditoriaService.registrar(
                usuario,
                null,
                "ENCERRAMENTO_CONTA",
                "USUARIO",
                usuario.getId(),
                "Conta encerrada logicamente; dados preservados."
        );
        usuario.encerrar(agora);
        sessaoRepository.revogarTodasDoUsuario(usuarioId, agora);
        tokenRepository.invalidarTokensPendentes(usuarioId);
    }

    private Usuario buscarUsuarioAtivo(Long usuarioId) {
        return usuarioRepository.findByIdAndDataExclusaoIsNull(usuarioId)
                .orElseThrow(() -> new RegraNegocioException(
                        HttpStatus.NOT_FOUND,
                        "usuario-nao-encontrado",
                        "A conta ativa não foi encontrada."
                ));
    }

    private ConfiguracaoUsuario buscarConfiguracao(Long usuarioId) {
        return configuracaoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RegraNegocioException(
                        HttpStatus.CONFLICT,
                        "configuracao-nao-encontrada",
                        "A configuração da conta não foi encontrada."
                ));
    }

    private void validarNovaSenha(AtualizacaoPerfilRequest request) {
        if (request.novaSenha() == null && request.confirmacaoNovaSenha() == null) {
            return;
        }
        if (request.novaSenha() == null
                || !request.novaSenha().equals(request.confirmacaoNovaSenha())) {
            throw new RegraNegocioException(
                    HttpStatus.BAD_REQUEST,
                    "senhas-divergentes",
                    "A nova senha e a confirmação devem ser iguais."
            );
        }
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
