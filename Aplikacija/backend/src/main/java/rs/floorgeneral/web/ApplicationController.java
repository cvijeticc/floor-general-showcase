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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.floorgeneral.domain.ApplicationStatus;
import org.springframework.security.core.Authentication;
import rs.floorgeneral.service.ApplicationService;
import rs.floorgeneral.service.mail.ApplicationMailer;
import rs.floorgeneral.web.dto.application.ApplicationResult;
import rs.floorgeneral.web.dto.application.ChangeApplicationStatusCmd;
import rs.floorgeneral.web.dto.application.MailStatusInfo;

/** D-20 — prijave u ligu pristigle sa javnog sajta. */
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Prijave")
@SecurityRequirement(name = "bearerAuth")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationMailer applicationMailer;

    @GetMapping
    public List<ApplicationResult> search(@RequestParam(required = false) ApplicationStatus status) {
        return applicationService.search(status);
    }

    /** Da li server šalje mejl o novim prijavama i na koje adrese. */
    @GetMapping("/mail-status")
    public MailStatusInfo mailStatus() {
        return applicationMailer.status();
    }

    /** Probni mejl na adresu lige — provera podešavanja pre nego što se sajt prebaci na domen. */
    @PostMapping("/mail-test")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mailTest(Authentication authentication) {
        applicationMailer.sendTest(authentication == null ? null : authentication.getName());
    }

    @PatchMapping("/{id}/status")
    public ApplicationResult changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeApplicationStatusCmd cmd) {
        return applicationService.changeStatus(id, cmd);
    }

    /** Pravi igraca iz prijave i oznacava je kao primljenu. */
    @PostMapping("/{id}/accept")
    public ApplicationResult accept(@PathVariable Long id) {
        return applicationService.accept(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        applicationService.delete(id);
    }
}
