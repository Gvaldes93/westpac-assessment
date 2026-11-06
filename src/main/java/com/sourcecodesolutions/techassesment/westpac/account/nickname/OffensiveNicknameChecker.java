package com.sourcecodesolutions.techassesment.westpac.account.nickname;

/** Abstraction so the word list can later be swapped for an external content-moderation service. */
public interface OffensiveNicknameChecker {

    boolean isOffensive(String nickname);
}
