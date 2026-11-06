package com.sourcecodesolutions.techassesment.westpac.account.nickname;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Flags a nickname when, after normalisation, it contains any term from a word list. Normalisation
 * lower-cases, strips accents, undoes common character substitutions and drops separators, so
 * "B.a.D-W0rd" matches "badword".
 *
 * <p>TODO: substring matching has false positives (the "Scunthorpe problem"); a real implementation
 * would add an allow-list or delegate to a managed moderation service, and load the list from a
 * centrally managed source that can be updated without a deployment.
 */
@Component
@EnableConfigurationProperties(NicknameModerationProperties.class)
class WordListOffensiveNicknameChecker implements OffensiveNicknameChecker {

    private static final Logger log = LoggerFactory.getLogger(WordListOffensiveNicknameChecker.class);

    private static final String COMMENT_PREFIX = "#";
    private static final String COMBINING_MARKS = "\\p{M}";
    private static final String NON_LETTERS = "[^\\p{L}]";
    private static final String EMPTY = "";

    // Look-alike characters commonly used to dodge word filters.
    private static final Map<Character, Character> SUBSTITUTIONS = Map.of(
            '0', 'o',
            '1', 'i',
            '3', 'e',
            '4', 'a',
            '5', 's',
            '7', 't',
            '@', 'a',
            '$', 's',
            '!', 'i');

    private final List<String> offensiveTerms;

    WordListOffensiveNicknameChecker(NicknameModerationProperties properties) {
        this.offensiveTerms = load(properties.wordListLocation());
        log.info("Loaded {} offensive nickname terms", offensiveTerms.size());
    }

    @Override
    public boolean isOffensive(String nickname) {
        String normalised = normalise(nickname);
        return offensiveTerms.stream().anyMatch(normalised::contains);
    }

    private static List<String> load(Resource wordList) {
        try {
            return wordList.getContentAsString(StandardCharsets.UTF_8).lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty())
                    .filter(line -> !line.startsWith(COMMENT_PREFIX))
                    .map(WordListOffensiveNicknameChecker::normalise)
                    .filter(term -> !term.isEmpty())
                    .distinct()
                    .toList();
        } catch (IOException e) {
            // Fail startup: running without the filter would silently accept offensive nicknames.
            throw new UncheckedIOException("Could not load offensive nickname list from " + wordList, e);
        }
    }

    private static String normalise(String text) {
        String lowerCase = text.toLowerCase(Locale.ROOT);
        StringBuilder substituted = new StringBuilder(lowerCase.length());
        for (char c : lowerCase.toCharArray()) {
            substituted.append(SUBSTITUTIONS.getOrDefault(c, c));
        }
        return Normalizer.normalize(substituted, Normalizer.Form.NFD)
                .replaceAll(COMBINING_MARKS, EMPTY)
                .replaceAll(NON_LETTERS, EMPTY);
    }
}
