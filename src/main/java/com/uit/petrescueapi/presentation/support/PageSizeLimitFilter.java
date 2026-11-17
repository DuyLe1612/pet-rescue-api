package com.uit.petrescueapi.presentation.support;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Caps page sizes for every endpoint in one place: a single {@code pageSize=100000} could otherwise
 * load a whole table into a 256 MB heap. Values above the limit are lowered, not rejected.
 *
 * <p>Also accepts {@code size} as an alias of {@code pageSize}: the web client sends {@code size}, and
 * before this the value was silently ignored and every list fell back to 20 rows.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PageSizeLimitFilter extends OncePerRequestFilter {

    static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SIZE_PARAMETERS = Set.of("pageSize", "size");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean tooLarge = SIZE_PARAMETERS.stream().anyMatch(name -> isTooLarge(request.getParameter(name)));
        boolean aliased = request.getParameter("size") != null && request.getParameter("pageSize") == null;
        chain.doFilter(tooLarge || aliased ? new CappedRequest(request) : request, response);
    }

    private static boolean isTooLarge(String value) {
        if (value == null) {
            return false;
        }
        try {
            return Long.parseLong(value.trim()) > MAX_PAGE_SIZE;
        } catch (NumberFormatException notANumber) {
            return false;       // binding reports it as usual
        }
    }

    private static final class CappedRequest extends HttpServletRequestWrapper {

        private final Map<String, String[]> parameters;

        CappedRequest(HttpServletRequest request) {
            super(request);
            parameters = new HashMap<>(request.getParameterMap());
            if (!parameters.containsKey("pageSize") && parameters.containsKey("size")) {
                parameters.put("pageSize", parameters.get("size"));
            }
            for (String name : SIZE_PARAMETERS) {
                if (isTooLarge(getParameter(name))) {
                    parameters.put(name, new String[]{String.valueOf(MAX_PAGE_SIZE)});
                }
            }
        }

        @Override
        public String getParameter(String name) {
            String[] values = parameters.get(name);
            return values == null || values.length == 0 ? null : values[0];
        }

        @Override
        public String[] getParameterValues(String name) {
            return parameters.get(name);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return parameters;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(parameters.keySet());
        }
    }
}
