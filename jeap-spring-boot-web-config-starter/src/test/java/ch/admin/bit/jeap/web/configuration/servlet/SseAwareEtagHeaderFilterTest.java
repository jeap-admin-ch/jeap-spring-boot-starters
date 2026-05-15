package ch.admin.bit.jeap.web.configuration.servlet;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SseAwareEtagHeaderFilterTest {

    @Mock
    private HttpServletRequest request;

    private final SseAwareEtagHeaderFilter filter = new SseAwareEtagHeaderFilter();

    @Test
    void shouldNotFilter_sseRequest_returnsTrue() {
        when(request.getHeader("Accept")).thenReturn(MediaType.TEXT_EVENT_STREAM_VALUE);

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_acceptContainingSseAmongOtherTypes_returnsTrue() {
        when(request.getHeader("Accept")).thenReturn("application/json, text/event-stream");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_regularJsonRequest_returnsFalse() {
        when(request.getHeader("Accept")).thenReturn(MediaType.APPLICATION_JSON_VALUE);

        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void shouldNotFilter_nullAcceptHeader_returnsFalse() {
        when(request.getHeader("Accept")).thenReturn(null);

        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void doFilter_regularRequest_addsEtagHeader() throws Exception {
        byte[] responseBody = "Hello, ETag World!".getBytes();

        MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "/api/data");
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> res.getOutputStream().write(responseBody);

        filter.doFilter(mockRequest, mockResponse, filterChain);

        assertThat(mockResponse.getHeader("ETag")).matches("\"0[0-9a-f]{32}\"");
    }

    @Test
    void doFilter_regularRequestWithMatchingEtag_returns304() throws Exception {
        byte[] responseBody = "Hello, ETag World!".getBytes();
        FilterChain filterChain = (req, res) -> res.getOutputStream().write(responseBody);

        // First request to obtain the ETag
        MockHttpServletRequest firstRequest = new MockHttpServletRequest("GET", "/api/data");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, filterChain);
        String etag = firstResponse.getHeader("ETag");

        // Second request with the ETag in If-None-Match
        MockHttpServletRequest secondRequest = new MockHttpServletRequest("GET", "/api/data");
        secondRequest.addHeader("If-None-Match", etag);
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, filterChain);

        assertThat(secondResponse.getStatus()).isEqualTo(304);
    }

    @Test
    void doFilter_sseRequest_doesNotAddEtagHeader() throws Exception {
        byte[] responseBody = "data: event\n\n".getBytes();

        MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "/events");
        mockRequest.addHeader("Accept", MediaType.TEXT_EVENT_STREAM_VALUE);
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> res.getOutputStream().write(responseBody);

        filter.doFilter(mockRequest, mockResponse, filterChain);

        assertThat(mockResponse.getHeader("ETag")).isNull();
    }
}
