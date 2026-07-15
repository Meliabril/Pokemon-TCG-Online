package ar.edu.utn.frc.tup.piii.controllers.matchmaking;

import ar.edu.utn.frc.tup.piii.dtos.websocket.MatchmakingQueueStatusDto;
import ar.edu.utn.frc.tup.piii.services.matchmaking.MatchmakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/matchmaking/queue")
@RequiredArgsConstructor
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    @PostMapping
    public ResponseEntity<MatchmakingQueueStatusDto> joinQueue(Authentication authentication) {
        return ResponseEntity.ok(matchmakingService.joinQueue(currentUserId(authentication)));
    }

    @DeleteMapping
    public ResponseEntity<Void> leaveQueue(Authentication authentication) {
        matchmakingService.leaveQueue(currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MatchmakingQueueStatusDto> getMyQueueStatus(Authentication authentication) {
        return ResponseEntity.ok(matchmakingService.getMyQueueStatus(currentUserId(authentication)));
    }

    @PostMapping("/custom")
    public ResponseEntity<MatchmakingQueueStatusDto> joinCustomQueue(Authentication authentication) {
        return ResponseEntity.ok(matchmakingService.joinCustomQueue(currentUserId(authentication)));
    }

    @PostMapping("/custom/join/{code}")
    public ResponseEntity<MatchmakingQueueStatusDto> joinCustomGame(@PathVariable String code, Authentication authentication) {
        return ResponseEntity.ok(matchmakingService.joinCustomGame(currentUserId(authentication), code));
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
