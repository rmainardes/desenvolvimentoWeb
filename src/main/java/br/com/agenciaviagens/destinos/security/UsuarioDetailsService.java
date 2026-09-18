package br.com.agenciaviagens.destinos.security;

import br.com.agenciaviagens.destinos.model.Perfil;
import br.com.agenciaviagens.destinos.model.Usuario;
import br.com.agenciaviagens.destinos.repository.jpa.UsuarioRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carrega o usuario do banco para o Spring Security.
 *
 * Este e o ponto que atende ao requisito de "autenticacao de usuarios
 * cadastrados no banco de dados": nao ha usuario em memoria, em properties nem
 * gerado na inicializacao do Spring Security.
 *
 * A mensagem de erro e sempre a mesma, exista o usuario ou nao. Mensagens
 * distintas para "usuario inexistente" e "senha incorreta" transformam o login
 * em um oraculo que confirma quais contas existem -- alem de ser enumeracao de
 * usuarios, e exposicao desnecessaria de dado pessoal.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private static final String FALHA_GENERICA = "Credenciais invalidas";

    private final UsuarioRepository usuarios;

    public UsuarioDetailsService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarios.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(FALHA_GENERICA));

        List<SimpleGrantedAuthority> autoridades = usuario.getPerfis().stream()
                .map(Perfil::comoAuthority)
                .map(SimpleGrantedAuthority::new)
                .toList();

        return User.withUsername(usuario.getUsername())
                .password(usuario.getSenhaHash())
                .authorities(autoridades)
                .disabled(!usuario.isAtivo())
                .build();
    }
}
