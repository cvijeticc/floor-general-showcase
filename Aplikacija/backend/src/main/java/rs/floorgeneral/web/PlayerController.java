package rs.floorgeneral.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.floorgeneral.domain.PlayerStatus;
import rs.floorgeneral.service.PlayerService;
import rs.floorgeneral.web.dto.player.ChangePlayerStatusCmd;
import rs.floorgeneral.web.dto.player.CreatePlayerCmd;
import rs.floorgeneral.web.dto.player.PlayerInfo;
import rs.floorgeneral.web.dto.player.PlayerResult;
import rs.floorgeneral.web.dto.player.UpdatePlayerCmd;

@RestController
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Igraci")
@SecurityRequirement(name = "bearerAuth")
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping
    public List<PlayerResult> search(@RequestParam(required = false) String search,
                                     @RequestParam(required = false) Long groupId,
                                     @RequestParam(required = false) PlayerStatus status) {
        return playerService.search(search, groupId, status);
    }

    @GetMapping("/{id}")
    public PlayerInfo findById(@PathVariable Long id) {
        return playerService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerInfo create(@Valid @RequestBody CreatePlayerCmd cmd) {
        return playerService.create(cmd);
    }

    @PutMapping("/{id}")
    public PlayerInfo update(@PathVariable Long id, @Valid @RequestBody UpdatePlayerCmd cmd) {
        return playerService.update(id, cmd);
    }

    /** BR-10 — odustanak i diskvalifikacija idu zasebnom operacijom, ne kroz PUT. */
    @PatchMapping("/{id}/status")
    public PlayerInfo changeStatus(@PathVariable Long id, @Valid @RequestBody ChangePlayerStatusCmd cmd) {
        return playerService.changeStatus(id, cmd);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        playerService.delete(id);
    }
}
