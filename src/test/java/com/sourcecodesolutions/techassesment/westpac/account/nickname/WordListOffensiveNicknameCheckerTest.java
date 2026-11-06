package com.sourcecodesolutions.techassesment.westpac.account.nickname;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

class WordListOffensiveNicknameCheckerTest {

    private static final String WORD_LIST = """
            # comment lines and blanks are ignored

            badword
            """;
    private static final String MISSING_RESOURCE = "does-not-exist.txt";

    private final OffensiveNicknameChecker checker = new WordListOffensiveNicknameChecker(
            new NicknameModerationProperties(new ByteArrayResource(WORD_LIST.getBytes(StandardCharsets.UTF_8))));

    @ParameterizedTest
    @ValueSource(strings = {"badword", "BadWord", "my badword fund", "b.a.d-w.o.r.d", "b4dw0rd", "bádwörd"})
    void flagsListedTermsRegardlessOfDisguise(String nickname) {
        assertThat(checker.isOffensive(nickname)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Holiday Fund", "Rainy day savings", "comment"})
    void acceptsCleanNicknames(String nickname) {
        assertThat(checker.isOffensive(nickname)).isFalse();
    }

    @Test
    void bundledWordListLoads() {
        OffensiveNicknameChecker bundled = new WordListOffensiveNicknameChecker(
                new NicknameModerationProperties(new ClassPathResource("moderation/offensive-nicknames.txt")));

        assertThat(bundled.isOffensive("Total Idiot Fund")).isTrue();
        assertThat(bundled.isOffensive("House Deposit")).isFalse();
    }

    @Test
    void failsFastWhenWordListIsMissing() {
        NicknameModerationProperties properties =
                new NicknameModerationProperties(new ClassPathResource(MISSING_RESOURCE));

        assertThatThrownBy(() -> new WordListOffensiveNicknameChecker(properties))
                .isInstanceOf(UncheckedIOException.class);
    }
}
