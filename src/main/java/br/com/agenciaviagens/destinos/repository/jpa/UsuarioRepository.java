package br.com.agenciaviagens.destinos.repository.jpa;

import br.com.agenciaviagens.destinos.model.Usuario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de usuarios.
 *
 * Diferente de destinos, aqui nao existe uma porta no pacote repository: nao
 * ha regra de negocio a proteger de uma troca de tecnologia -- usuario e
 * infraestrutura de autenticacao, consumida apenas pelo UserDetailsService e
 * pela carga inicial. Criar uma interface extra seria cerimonia sem beneficio.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);
}
