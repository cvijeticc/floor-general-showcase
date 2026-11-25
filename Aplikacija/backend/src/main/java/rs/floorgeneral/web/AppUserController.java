package rs.floorgeneral.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.floorgeneral.service.AppUserService;
import rs.floorgeneral.web.dto.user.ChangeOwnPasswordCmd;
import rs.floorgeneral.web.dto.user.CreateUserCmd;
import rs.floorgeneral.web.dto.user.ResetPasswordCmd;
import rs.floorgeneral.web.dto.user.UserInfo;
import rs.floorgeneral.web.dto.user.UserResult;

/** UC-ADM-13 — admin nalozi. Lozinka se nikad ne vraca u odgovoru. */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Nalozi")
@SecurityRequirement(name = "bearerAuth")
public class AppUserController {

    private final AppUserService appUserService;

    @GetMapping
    public List<UserResult> search() {
        return appUserService.search();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserInfo create(@Valid @RequestBody CreateUserCmd cmd) {
        return appUserService.create(cmd);
    }

    /** Prijavljeni korisnik menja svoju lozinku — trazi se stara. */
    @PatchMapping("/me/password")
    public UserInfo changeOwnPassword(Principal principal, @Valid @RequestBody ChangeOwnPasswordCmd cmd) {
        return appUserService.changeOwnPassword(principal.getName(), cmd);
    }

    /** Reset tudje lozinke. */
    @PatchMapping("/{id}/password")
    public UserInfo resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordCmd cmd) {
        return appUserService.resetPassword(id, cmd);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        appUserService.delete(id);
    }
}
