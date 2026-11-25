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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.floorgeneral.service.CourtService;
import rs.floorgeneral.web.dto.court.CourtInfo;
import rs.floorgeneral.web.dto.court.CourtResult;
import rs.floorgeneral.web.dto.court.CreateCourtCmd;
import rs.floorgeneral.web.dto.court.UpdateCourtCmd;

@RestController
@RequestMapping("/api/v1/courts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Tereni")
@SecurityRequirement(name = "bearerAuth")
public class CourtController {

    private final CourtService courtService;

    @GetMapping
    public List<CourtResult> search() {
        return courtService.search();
    }

    @GetMapping("/{id}")
    public CourtInfo findById(@PathVariable Long id) {
        return courtService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourtInfo create(@Valid @RequestBody CreateCourtCmd cmd) {
        return courtService.create(cmd);
    }

    @PutMapping("/{id}")
    public CourtInfo update(@PathVariable Long id, @Valid @RequestBody UpdateCourtCmd cmd) {
        return courtService.update(id, cmd);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        courtService.delete(id);
    }
}
