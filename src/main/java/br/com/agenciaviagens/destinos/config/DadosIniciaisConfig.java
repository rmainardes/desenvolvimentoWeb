package br.com.agenciaviagens.destinos.config;

import br.com.agenciaviagens.destinos.dto.AvaliacaoRequest;
import br.com.agenciaviagens.destinos.dto.DestinoRequest;
import br.com.agenciaviagens.destinos.model.Destino;
import br.com.agenciaviagens.destinos.model.Perfil;
import br.com.agenciaviagens.destinos.model.Usuario;
import br.com.agenciaviagens.destinos.repository.jpa.UsuarioRepository;
import br.com.agenciaviagens.destinos.service.DestinoService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Carga inicial: usuarios de acesso e destinos de exemplo.
 *
 * Diferenca importante em relacao a versao em memoria: o banco sobrevive ao
 * restart. A carga anterior recriava os mesmos destinos a cada inicializacao,
 * o que com persistencia real faria a aplicacao subir uma vez e quebrar na
 * segunda com violacao de unicidade. Agora as duas cargas sao idempotentes --
 * so inserem o que ainda nao existe.
 *
 * As senhas nunca aparecem no codigo nem no application.yml: vem de variavel
 * de ambiente, com um valor padrao obvio que serve apenas para o ambiente de
 * desenvolvimento e que a aplicacao denuncia no log quando e usado.
 */
@Configuration
public class DadosIniciaisConfig {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciaisConfig.class);

    private static final String SENHA_PADRAO_DESENVOLVIMENTO = "trocar-esta-senha";

    @Value("${app.usuarios-iniciais.admin-username:admin}")
    private String adminUsername;

    @Value("${app.usuarios-iniciais.admin-senha:" + SENHA_PADRAO_DESENVOLVIMENTO + "}")
    private String adminSenha;

    @Value("${app.usuarios-iniciais.user-username:viajante}")
    private String userUsername;

    @Value("${app.usuarios-iniciais.user-senha:" + SENHA_PADRAO_DESENVOLVIMENTO + "}")
    private String userSenha;

    /**
     * Cria os usuarios de acesso caso ainda nao existam.
     *
     * Roda antes da carga de destinos apenas por clareza de log; nao ha
     * dependencia entre as duas.
     */
    @Bean
    @Order(1)
    public ApplicationRunner criarUsuariosIniciais(UsuarioRepository usuarios, PasswordEncoder encoder) {
        return args -> {
            criarSeAusente(usuarios, encoder, adminUsername, adminSenha, Set.of(Perfil.ADMIN, Perfil.USER));
            criarSeAusente(usuarios, encoder, userUsername, userSenha, Set.of(Perfil.USER));

            if (SENHA_PADRAO_DESENVOLVIMENTO.equals(adminSenha) || SENHA_PADRAO_DESENVOLVIMENTO.equals(userSenha)) {
                log.warn("ATENCAO: ha usuario usando a senha padrao de desenvolvimento. "
                        + "Defina APP_ADMIN_SENHA e APP_USER_SENHA antes de expor esta API.");
            }
        };
    }

    private void criarSeAusente(UsuarioRepository usuarios,
                                PasswordEncoder encoder,
                                String username,
                                String senha,
                                Set<Perfil> perfis) {
        if (usuarios.existsByUsername(username)) {
            log.info("Usuario '{}' ja existe, mantido como esta", username);
            return;
        }
        usuarios.save(new Usuario(username, encoder.encode(senha), perfis));
        log.info("Usuario '{}' criado com perfis {}", username, perfis);
    }

    /**
     * Popula o catalogo com destinos de exemplo na primeira execucao.
     * Desative com app.dados-iniciais=false.
     */
    @Bean
    @Order(2)
    public ApplicationRunner carregarDestinos(DestinoService destinoService,
                                              @Value("${app.dados-iniciais:true}") boolean habilitado) {
        return args -> {
            if (!habilitado) {
                log.info("Carga de destinos de exemplo desativada");
                return;
            }
            if (!destinoService.semDestinosCadastrados()) {
                log.info("Ja existem destinos cadastrados, carga de exemplo ignorada");
                return;
            }

            Destino gramado = destinoService.criar(new DestinoRequest(
                    "Gramado",
                    "Cidade serrana com arquitetura europeia, chocolate artesanal e clima frio.",
                    new DestinoRequest.LocalizacaoRequest("Gramado", "RS", "Brasil"),
                    List.of("City tour", "Mini Mundo", "Rota do vinho"),
                    120,
                    new BigDecimal("2350.00")));

            destinoService.criar(new DestinoRequest(
                    "Fernando de Noronha",
                    "Arquipelago com praias preservadas e mergulho em aguas cristalinas.",
                    new DestinoRequest.LocalizacaoRequest("Fernando de Noronha", "PE", "Brasil"),
                    List.of("Mergulho", "Passeio de barco", "Trilha do Atalaia"),
                    45,
                    new BigDecimal("5890.00")));

            destinoService.criar(new DestinoRequest(
                    "Bariloche",
                    "Destino de montanha na Patagonia argentina, com lagos e estacoes de esqui.",
                    new DestinoRequest.LocalizacaoRequest("San Carlos de Bariloche", "Rio Negro", "Argentina"),
                    List.of("Esqui", "Cerro Catedral", "Circuito Chico"),
                    80,
                    new BigDecimal("4120.00")));

            destinoService.registrarAvaliacao(gramado.getId(),
                    new AvaliacaoRequest("Ana", 5, "Cidade encantadora no inverno."));
            destinoService.registrarAvaliacao(gramado.getId(),
                    new AvaliacaoRequest("Bruno", 4, "Otima estrutura, mas cheia na alta temporada."));

            log.info("Carga de destinos de exemplo concluida");
        };
    }
}
