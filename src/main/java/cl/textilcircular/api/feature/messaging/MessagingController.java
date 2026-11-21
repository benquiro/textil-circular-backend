package cl.textilcircular.api.feature.messaging;

import cl.textilcircular.api.core.security.Caller;
import cl.textilcircular.api.feature.messaging.MessagingDtos.ConversationView;
import cl.textilcircular.api.feature.messaging.MessagingDtos.MessageView;
import cl.textilcircular.api.feature.messaging.MessagingDtos.OpenConversationRequest;
import cl.textilcircular.api.feature.messaging.MessagingDtos.ReportRequest;
import cl.textilcircular.api.feature.messaging.MessagingDtos.SendMessageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** El chat entre un cliente y un emprendimiento: bandeja, historial, envío, estado de lectura, bloqueo y reportes. */
@RestController
@RequiredArgsConstructor
class MessagingController {

    private final MessagingService messaging;

    @Operation(summary = "Ver mi bandeja de mensajes", tags = "Messaging",
            description = "Conversaciones de quien llama, con la del mensaje más reciente primero.")
    @ApiResponse(responseCode = "200", description = "Conversaciones")
    @GetMapping("/conversations")
    List<ConversationView> list(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return messaging.list(Caller.of(jwt));
    }

    @Operation(summary = "Abrir una conversación", tags = "Messaging",
            description = "Devuelve la conversación existente o la crea. Solo clientes ACTIVE con un emprendimiento público y ajeno.")
    @ApiResponse(responseCode = "200", description = "La conversación")
    @ApiResponse(responseCode = "403", description = "`FORBIDDEN`: quien llama no es un cliente ACTIVE o es el propio emprendimiento")
    @ApiResponse(responseCode = "404", description = "`NOT_FOUND`: el emprendimiento no existe o no es público")
    @PostMapping("/conversations")
    ConversationView open(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                          @RequestBody OpenConversationRequest request) {
        return messaging.open(Caller.of(jwt), request);
    }

    @Operation(summary = "Listar mensajes", tags = "Messaging",
            description = "Del más antiguo al más reciente. `afterId` devuelve solo los posteriores a ese mensaje.")
    @ApiResponse(responseCode = "200", description = "Mensajes")
    @ApiResponse(responseCode = "404", description = "`NOT_FOUND`: la conversación no existe para quien llama")
    @GetMapping("/conversations/{conversationId}/messages")
    List<MessageView> messages(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                               @Parameter(description = "Id de la conversación") @PathVariable long conversationId,
                               @Parameter(description = "Id del último mensaje que la app ya tiene") @RequestParam(required = false) Long afterId) {
        return messaging.messages(Caller.of(jwt), conversationId, afterId);
    }

    @Operation(summary = "Enviar un mensaje", tags = "Messaging",
            description = "Idempotente por `clientUuid`: un reintento responde 200 con el mensaje ya guardado.")
    @ApiResponse(responseCode = "200", description = "Ya estaba guardado (reintento)")
    @ApiResponse(responseCode = "201", description = "Guardado")
    @ApiResponse(responseCode = "403", description = "`FORBIDDEN` o `CONVERSATION_BLOCKED`")
    @ApiResponse(responseCode = "409", description = "`MESSAGE_ID_CONFLICT`: el uuid ya lo usó otro remitente")
    @PostMapping("/conversations/{conversationId}/messages")
    ResponseEntity<MessageView> send(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                     @Parameter(description = "Id de la conversación") @PathVariable long conversationId,
                                     @RequestBody SendMessageRequest request) {
        MessagingService.SendResult result = messaging.send(Caller.of(jwt), conversationId, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.message());
    }

    @Operation(summary = "Marcar una conversación como leída", tags = "Messaging",
            description = "Marca los mensajes de la contraparte y le avisa por el WebSocket (`message.read`).")
    @ApiResponse(responseCode = "204", description = "Marcados")
    @ApiResponse(responseCode = "403", description = "`FORBIDDEN`: quien llama no participa")
    @PostMapping("/conversations/{conversationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markRead(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                  @Parameter(description = "Id de la conversación") @PathVariable long conversationId) {
        messaging.markRead(Caller.of(jwt), conversationId);
    }

    @Operation(summary = "Bloquear una conversación", tags = "Messaging",
            description = "Ningún participante puede enviar mensajes mientras siga bloqueada. Los anteriores se siguen viendo.")
    @ApiResponse(responseCode = "204", description = "Bloqueada")
    @ApiResponse(responseCode = "403", description = "`FORBIDDEN`: quien llama no participa")
    @PutMapping("/conversations/{conversationId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void block(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
               @Parameter(description = "Id de la conversación") @PathVariable long conversationId) {
        messaging.block(Caller.of(jwt), conversationId);
    }

    @Operation(summary = "Desbloquear una conversación", tags = "Messaging",
            description = "Levanta el bloqueo; los participantes pueden volver a escribir.")
    @ApiResponse(responseCode = "204", description = "Desbloqueada")
    @ApiResponse(responseCode = "404", description = "`NOT_FOUND`: la conversación no existe para quien llama")
    @DeleteMapping("/conversations/{conversationId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unblock(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                 @Parameter(description = "Id de la conversación") @PathVariable long conversationId) {
        messaging.unblock(Caller.of(jwt), conversationId);
    }

    @Operation(summary = "Reportar una conversación", tags = "Messaging",
            description = "Solo puede haber un reporte OPEN por conversación y denunciante.")
    @ApiResponse(responseCode = "201", description = "Reportada")
    @ApiResponse(responseCode = "409", description = "`REPORT_ALREADY_OPEN`: ya hay un reporte abierto")
    @PostMapping("/conversations/{conversationId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    void report(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                @Parameter(description = "Id de la conversación") @PathVariable long conversationId,
                @RequestBody ReportRequest request) {
        messaging.report(Caller.of(jwt), conversationId, request);
    }
}
