package it.unibo.fantasyf1.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class SimpleScoringPolicyTest {

    private SimpleScoringPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new SimpleScoringPolicy();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("weekendPerformances")
    void calculatesScoreFromSportingPerformance(
        final String description,
        final PerformanceData performance,
        final int expectedScore
    ) {
        assertEquals(expectedScore, policy.score(performance), description);
    }

    @Test
    void missingResultsHaveNoPositionPoints() {
        final PerformanceData performance =
            new PerformanceData(null, null, false, false);

        assertEquals(0, policy.score(performance));
    }

    private static Stream<Arguments> weekendPerformances() {
        return Stream.of(
            Arguments.of(
                "pole, vittoria e giro veloce",
                new PerformanceData(1, 1, false, true),
                27
            ),
            Arguments.of(
                "secondo in qualifica e terzo in gara",
                new PerformanceData(2, 3, false, false),
                22
            ),
            Arguments.of(
                "ultima posizione in gara",
                new PerformanceData(20, 20, false, false),
                1
            ),
            Arguments.of(
                "solo risultato di gara",
                new PerformanceData(null, 10, false, false),
                11
            ),
            Arguments.of(
                "solo risultato di qualifica",
                new PerformanceData(4, null, false, false),
                2
            ),
            Arguments.of(
                "penalizzazione",
                new PerformanceData(null, null, true, false),
                -5
            ),
            Arguments.of(
                "giro veloce e penalizzazione",
                new PerformanceData(null, null, true, true),
                -3
            )
        );
    }
}
