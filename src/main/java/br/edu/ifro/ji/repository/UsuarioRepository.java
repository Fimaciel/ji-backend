package br.edu.ifro.ji.repository;

import br.edu.ifro.ji.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCaseAndDataExclusaoIsNull(String email);

    Optional<Usuario> findByIdAndDataExclusaoIsNull(Long id);

    boolean existsByEmailIgnoreCaseAndDataExclusaoIsNullAndIdNot(String email, Long id);

    boolean existsByCpf(String cpf);
}
