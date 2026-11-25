package rs.floorgeneral.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.floorgeneral.domain.League;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.repository.LeagueRepository;
import rs.floorgeneral.web.dto.league.CreateLeagueCmd;
import rs.floorgeneral.web.dto.league.LeagueInfo;
import rs.floorgeneral.web.dto.league.LeagueResult;
import rs.floorgeneral.web.dto.league.UpdateLeagueCmd;
import rs.floorgeneral.web.mapper.LeagueMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeagueService {

    private final LeagueRepository leagueRepository;
    private final LeagueMapper leagueMapper;

    public List<LeagueResult> search() {
        return leagueRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(leagueMapper::toResult)
                .toList();
    }

    public LeagueInfo findById(Long id) {
        return leagueMapper.toInfo(getLeagueOrThrow(id));
    }

    @Transactional
    public LeagueInfo create(CreateLeagueCmd cmd) {
        League league = leagueMapper.toEntity(cmd);
        return leagueMapper.toInfo(leagueRepository.save(league));
    }

    @Transactional
    public LeagueInfo update(Long id, UpdateLeagueCmd cmd) {
        League league = getLeagueOrThrow(id);
        leagueMapper.updateEntityFromCmd(cmd, league);
        return leagueMapper.toInfo(league);
    }

    @Transactional
    public void delete(Long id) {
        leagueRepository.delete(getLeagueOrThrow(id));
    }

    private League getLeagueOrThrow(Long id) {
        return leagueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Liga sa ID " + id + " ne postoji"));
    }
}
