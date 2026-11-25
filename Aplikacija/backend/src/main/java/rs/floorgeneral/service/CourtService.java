package rs.floorgeneral.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.floorgeneral.domain.Court;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.CourtRepository;
import rs.floorgeneral.web.dto.court.CourtInfo;
import rs.floorgeneral.web.dto.court.CourtResult;
import rs.floorgeneral.web.dto.court.CreateCourtCmd;
import rs.floorgeneral.web.dto.court.UpdateCourtCmd;
import rs.floorgeneral.web.mapper.CourtMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourtService {

    private final CourtRepository courtRepository;
    private final CourtMapper courtMapper;

    public List<CourtResult> search() {
        return courtRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(courtMapper::toResult)
                .toList();
    }

    public CourtInfo findById(Long id) {
        return courtMapper.toInfo(getCourtOrThrow(id));
    }

    @Transactional
    public CourtInfo create(CreateCourtCmd cmd) {
        Court court = courtMapper.toEntity(cmd);
        return courtMapper.toInfo(courtRepository.save(court));
    }

    @Transactional
    public CourtInfo update(Long id, UpdateCourtCmd cmd) {
        Court court = getCourtOrThrow(id);
        courtMapper.updateEntityFromCmd(cmd, court);
        return courtMapper.toInfo(court);
    }

    @Transactional
    public void delete(Long id) {
        courtRepository.delete(getCourtOrThrow(id));
    }

    private Court getCourtOrThrow(Long id) {
        return courtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teren sa ID " + id + " ne postoji"));
    }
}
