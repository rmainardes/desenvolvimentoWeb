package br.com.agenciaviagens.destinos.config;

import br.com.agenciaviagens.destinos.dto.AvaliacaoRequest;
import br.com.agenciaviagens.destinos.dto.DestinoRequest;
import br.com.agenciaviagens.destinos.model.Destino;
import br.com.agenciaviagens.destinos.service.DestinoService;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Carga de dados ficticios para facilitar os testes manuais da API.
 * Desative com app.dados-iniciais=false.
 */
@Configuration
public class DadosIniciaisConfig {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciaisConfig.class);

    @Bean
    public ApplicationRunner carregarDados(DestinoService destinoService,
                                           @Value("${app.dados-iniciais:true}") boolean habilitado) {
        return args -> {
            if (!habilitado) {
                log.info("Carga de dados iniciais desativada");
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

            log.info("Carga de dados iniciais concluida");
        };
    }
}
