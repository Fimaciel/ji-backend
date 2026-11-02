package br.edu.ifro.ji.security;

import br.edu.ifro.ji.exception.RegraNegocioException;
import br.edu.ifro.ji.service.SessaoService;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final SessaoService sessaoService;

    public JwtAuthenticationConverter(SessaoService sessaoService) {
        this.sessaoService = sessaoService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        try {
            UsuarioPrincipal principal = sessaoService.autenticar(jwt);
            return UsernamePasswordAuthenticationToken.authenticated(principal, jwt, List.of());
        } catch (RegraNegocioException exception) {
            throw new BadCredentialsException(exception.getMessage(), exception);
        }
    }
}
