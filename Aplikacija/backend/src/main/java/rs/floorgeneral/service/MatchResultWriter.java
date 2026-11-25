package rs.floorgeneral.service;

import org.springframework.http.HttpStatus;
import rs.floorgeneral.domain.Match;
import rs.floorgeneral.domain.MatchStatus;
import rs.floorgeneral.engine.MatchResultRules;
import rs.floorgeneral.exception.ApiException;

/**
 * Jedino mesto koje upisuje rezultat na mec. Koriste ga i unos vec odigranog meca (UC-ADM-08),
 * i pojedinacni i masovni unos rezultata (UC-ADM-09), i korekcija (UC-ADM-11) — da pravila
 * BR-11 ne bi postojala u tri primerka.
 * <p>
 * Odluku o ispravnosti donosi {@link MatchResultRules} (paket {@code engine}); ovde se ona samo
 * prevodi u HTTP odgovor i upisuje u entitet.
 */
final class MatchResultWriter {

    private MatchResultWriter() {
    }

    /**
     * Upisuje rezultat, izvodi pobednika i postavlja status {@code FINISHED}.
     * Gornja granica rezultata se cita iz sezone kojoj mec pripada — nikad se ne hardkoduje.
     */
    static void applyOrThrow(Match match, int scorePlayer1, int scorePlayer2) {
        int maxScore = match.getStage().getSeason().getMaxScore();

        MatchResultRules.validate(maxScore, scorePlayer1, scorePlayer2)
                .ifPresent(violation -> {
                    throw new ApiException(HttpStatus.BAD_REQUEST, describe(violation, maxScore));
                });

        match.setScorePlayer1(scorePlayer1);
        match.setScorePlayer2(scorePlayer2);
        match.setWinner(MatchResultRules.firstPlayerWon(scorePlayer1, scorePlayer2)
                ? match.getPlayer1()
                : match.getPlayer2());
        match.setStatus(MatchStatus.FINISHED);
    }

    /**
     * Mec serije playoff-a se menja samo kroz {@code PlayoffService}: tamo se posle svake izmene
     * racuna ko je dobio seriju i ko ide dalje u drvetu. Opsti unos bi to zaobisao.
     */
    static void rejectSeriesGame(Match match) {
        if (match.getSeries() != null) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Mec br. " + match.getMatchID() + " je mec playoff serije — menja se na ekranu Playoff");
        }
    }

    static String describe(MatchResultRules.Violation violation, int maxScore) {
        return switch (violation) {
            case NEGATIVE_SCORE -> "Rezultat ne moze biti negativan";
            case DRAW -> "Mec nema neresen rezultat — resava se zlatnim kosem (BR-11)";
            case ABOVE_MAX -> "Rezultat ne moze biti veci od " + maxScore
                    + " — toliko je najvise moguce u ovoj sezoni";
        };
    }
}
