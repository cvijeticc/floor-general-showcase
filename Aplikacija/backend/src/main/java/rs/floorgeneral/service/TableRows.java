package rs.floorgeneral.service;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Citanje i vracanje pojedinacnog reda tabele, bez ijednog imena entiteta (UC-ADM-23).
 * <p>
 * Namerno ide kroz {@link JdbcTemplate}, a ne kroz JPA: vraceni red mora da dobije <b>isti</b>
 * primarni kljuc koji je imao pre brisanja, sto JPA sa {@code GenerationType.IDENTITY} ne dozvoljava.
 * Sekvenca se pritom ne dira — kljuc je vec bio dodeljen, pa se ne moze ponovo izdati.
 */
@Component
@RequiredArgsConstructor
class TableRows {

    /** Nazivi tabela i kolona dolaze iz {@link UndoTables} i iz metapodataka baze, nikad od korisnika. */
    private static final Pattern IDENTIFIER = Pattern.compile("^[a-z_][a-z0-9_]*$");
    private static final Pattern TYPE_NAME = Pattern.compile("^[a-z0-9_ ]+$");

    private final JdbcTemplate jdbcTemplate;

    /** Trenutno stanje reda, ili {@code null} ako reda nema. */
    RowSnapshot capture(RowSnapshot.RowOp op, String table, String pkColumn, Object pkValue) {
        String sql = "select * from " + identifier(table) + " where " + identifier(pkColumn) + " = ?";

        return jdbcTemplate.query(sql, resultSet -> {
            if (!resultSet.next()) {
                return null;
            }
            ResultSetMetaData metaData = resultSet.getMetaData();
            Map<String, String> values = new LinkedHashMap<>();
            Map<String, String> types = new LinkedHashMap<>();

            for (int column = 1; column <= metaData.getColumnCount(); column++) {
                String name = metaData.getColumnLabel(column).toLowerCase(Locale.ROOT);
                // getString daje tekstualni zapis bilo kog tipa — isti onaj koji cast ume da vrati nazad.
                values.put(name, resultSet.getString(column));
                types.put(name, metaData.getColumnTypeName(column).toLowerCase(Locale.ROOT));
            }
            return new RowSnapshot(op, table, pkColumn, String.valueOf(pkValue), values, types);
        }, pkValue);
    }

    /** Vraca red u zapamceno stanje: napravljen se brise, izmenjen ili obrisan se upisuje nazad. */
    void restore(RowSnapshot snapshot) {
        if (snapshot.op() == RowSnapshot.RowOp.CREATED) {
            delete(snapshot);
        } else {
            upsert(snapshot);
        }
    }

    private void delete(RowSnapshot snapshot) {
        String sql = "delete from " + identifier(snapshot.table())
                + " where " + identifier(snapshot.pkColumn()) + " = " + castPlaceholder(snapshot, snapshot.pkColumn());

        jdbcTemplate.update(sql, snapshot.pkValue());
    }

    /**
     * Jedan upis pokriva i izmenu i brisanje: ako reda nema, upisuje se ceo (sa starim kljucem),
     * a ako postoji, sve kolone se vracaju na staru vrednost.
     */
    private void upsert(RowSnapshot snapshot) {
        List<String> columns = new ArrayList<>(snapshot.values().keySet());

        String columnList = columns.stream().map(TableRows::identifier).collect(Collectors.joining(", "));
        String placeholders = columns.stream()
                .map(column -> castPlaceholder(snapshot, column))
                .collect(Collectors.joining(", "));
        String updates = columns.stream()
                .filter(column -> !column.equals(snapshot.pkColumn()))
                .map(column -> identifier(column) + " = excluded." + identifier(column))
                .collect(Collectors.joining(", "));

        String sql = "insert into " + identifier(snapshot.table()) + " (" + columnList + ") values ("
                + placeholders + ") on conflict (" + identifier(snapshot.pkColumn()) + ") "
                + (updates.isEmpty() ? "do nothing" : "do update set " + updates);

        Object[] arguments = columns.stream().map(column -> snapshot.values().get(column)).toArray();
        jdbcTemplate.update(sql, arguments);
    }

    /**
     * {@code cast(? as timestamp)} — vrednost putuje kao tekst, a baza je vraca u tip kolone.
     * Bez toga bi datumi i brojevi morali da se pogadjaju u Javi, po tipu koji je Jackson zapamtio.
     */
    private static String castPlaceholder(RowSnapshot snapshot, String column) {
        String type = snapshot.types().get(column);
        if (type == null || !TYPE_NAME.matcher(type).matches()) {
            throw new IllegalStateException("Nepoznat tip kolone " + column + " u tabeli " + snapshot.table());
        }
        return "cast(? as " + castableType(type) + ")";
    }

    /**
     * {@code serial} nije pravi tip nego skracenica za „integer + sekvenca", pa se u {@code cast}
     * ne sme staviti. Drajver ga ipak tako prijavljuje za kolone primarnog kljuca.
     */
    private static String castableType(String type) {
        return switch (type) {
            case "bigserial" -> "int8";
            case "serial" -> "int4";
            case "smallserial" -> "int2";
            default -> type;
        };
    }

    private static String identifier(String name) {
        if (name == null || !IDENTIFIER.matcher(name).matches()) {
            throw new IllegalStateException("Nedozvoljen naziv u undo zapisu: " + name);
        }
        return name;
    }
}
