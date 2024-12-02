package ch.admin.bit.jeap.web.configuration;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeaderConfigurationTest {

    @Test
    void accept_defaults() {
        HeaderConfiguration headerConfiguration = new HeaderConfiguration();

        assertFalse(headerConfiguration.accept("/api"));
        assertFalse(headerConfiguration.accept("/ui-api"));
        assertFalse(headerConfiguration.accept("/some-consumer-api"));
        assertTrue(headerConfiguration.accept("/foo/api"));
        assertTrue(headerConfiguration.accept("/foo/some-other-api"));
        assertTrue(headerConfiguration.accept("/"));
        assertTrue(headerConfiguration.accept(""));
        assertTrue(headerConfiguration.accept("/index.html"));
        assertTrue(headerConfiguration.accept("/some/path"));
    }

    @Test
    void accept_customized_accept_prefix() {
        HeaderConfiguration headerConfiguration = new HeaderConfiguration();
        headerConfiguration.setAcceptPathPrefixes(Set.of("/myresources"));

        assertTrue(headerConfiguration.accept("/myresources"));
        assertTrue(headerConfiguration.accept("/myresources/"));
        assertTrue(headerConfiguration.accept("/myresources/foo"));

        assertFalse(headerConfiguration.accept("/"));
        assertFalse(headerConfiguration.accept("/other"));
    }

    @Test
    void accept_customized_accept_prefix_and_pattern() {
        HeaderConfiguration headerConfiguration = new HeaderConfiguration();
        headerConfiguration.setAcceptPathPrefixes(Set.of("/myresources"));
        headerConfiguration.setAcceptPathPattern(Pattern.compile(".*match.*"));

        assertTrue(headerConfiguration.accept("/myresources"));
        assertTrue(headerConfiguration.accept("/myresources/"));
        assertTrue(headerConfiguration.accept("/myresources/foo"));
        assertTrue(headerConfiguration.accept("/any/match"));

        assertFalse(headerConfiguration.accept("/"));
        assertFalse(headerConfiguration.accept("/other"));
    }

    @Test
    void accept_customized_skip_prefix_and_pattern() {
        HeaderConfiguration headerConfiguration = new HeaderConfiguration();
        headerConfiguration.setSkipPathPattern(Pattern.compile(".*noheader.*"));
        headerConfiguration.setSkipPathPrefixes(Set.of("/skipme"));

        assertFalse(headerConfiguration.accept("/skipme"));
        assertFalse(headerConfiguration.accept("/noheader"));
        assertTrue(headerConfiguration.accept("/api"));
        assertTrue(headerConfiguration.accept("/"));
    }
}