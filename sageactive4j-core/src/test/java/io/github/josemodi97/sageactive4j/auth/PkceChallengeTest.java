package io.github.josemodi97.sageactive4j.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PkceChallengeTest {

    @Test
    void matchesTheRfc7636AppendixBVector() {
        PkceChallenge pkce = PkceChallenge.of("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk");
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", pkce.getChallenge());
        assertEquals("S256", pkce.getMethod());
    }

    @Test
    void generatesUrlSafeUniqueVerifiers() {
        PkceChallenge a = PkceChallenge.generate();
        PkceChallenge b = PkceChallenge.generate();
        assertEquals(43, a.getVerifier().length());
        assertTrue(a.getVerifier().matches("[A-Za-z0-9_-]+"));
        assertTrue(a.getChallenge().matches("[A-Za-z0-9_-]+"));
        assertNotEquals(a.getVerifier(), b.getVerifier());
        assertEquals(PkceChallenge.of(a.getVerifier()).getChallenge(), a.getChallenge());
    }

    @Test
    void rejectsOutOfRangeVerifiers() {
        assertThrows(IllegalArgumentException.class, () -> PkceChallenge.of("too-short"));
    }

    @Test
    void toStringHidesTheVerifier() {
        PkceChallenge pkce = PkceChallenge.generate();
        assertFalse(pkce.toString().contains(pkce.getVerifier()));
    }
}
