package br.com.agenciaviagens.destinos.security;

import br.com.agenciaviagens.destinos.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Padroniza as respostas de 401 e 403.
 *
 * Falhas de autenticacao e autorizacao acontecem na cadeia de filtros, antes
 * de qualquer controller, entao o @RestControllerAdvice nunca as enxerga. Sem
 * este componente o cliente receberia um corpo vazio nesses dois casos, num
 * contrato que devolve JSON estruturado em todos os outros -- inconsistencia
 * que quebra quem integra.
 *
 * Sobre o JsonMapper: o Spring Boot 4 usa Jackson 3, que mudou de pacote
 * (tools.jackson, no lugar de com.fasterxml.jackson) e introduziu JsonMapper
 * como especializacao de ObjectMapper para JSON. O bean autoconfigurado pelo
 * Boot e do tipo JsonMapper, e e ele que deve ser injetado.
 *
 * As mensagens sao deliberadamente genericas: nao dizem qual perfil seria
 * necessario nem se o usuario existe.
 */
@Component
public class TratadorDeErrosDeSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErrosDeSeguranca.class);

    private final JsonMapper jsonMapper;

    public TratadorDeErrosDeSeguranca(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /** 401: nao autenticado, ou credenciais invalidas. */
    @Override
    public void commence(HttpServletRequest requisicao,
                         HttpServletResponse resposta,
                         AuthenticationException excecao) throws IOException {
        log.warn("Acesso nao autenticado em {} {}", requisicao.getMethod(), requisicao.getRequestURI());
        // Sinaliza ao cliente qual esquema usar. O realm entre aspas mantem a
        // resposta compativel com clientes HTTP; navegadores podem abrir a
        // janela de login, o que nao e problema para uma API de integracao.
        resposta.setHeader("WWW-Authenticate", "Basic realm=\"destinos-api\", charset=\"UTF-8\"");
        escrever(requisicao, resposta, HttpStatus.UNAUTHORIZED,
                "Autenticacao obrigatoria para acessar este recurso");
    }

    /** 403: autenticado, mas sem o perfil exigido. */
    @Override
    public void handle(HttpServletRequest requisicao,
                       HttpServletResponse resposta,
                       AccessDeniedException excecao) throws IOException {
        log.warn("Acesso negado em {} {}", requisicao.getMethod(), requisicao.getRequestURI());
        escrever(requisicao, resposta, HttpStatus.FORBIDDEN,
                "Seu perfil de acesso nao permite esta operacao");
    }

    private void escrever(HttpServletRequest requisicao,
                          HttpServletResponse resposta,
                          HttpStatus status,
                          String mensagem) throws IOException {
        ErroResponse corpo = ErroResponse.de(status.value(), status.getReasonPhrase(),
                mensagem, requisicao.getRequestURI());

        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        resposta.getWriter().write(jsonMapper.writeValueAsString(corpo));
    }
}
