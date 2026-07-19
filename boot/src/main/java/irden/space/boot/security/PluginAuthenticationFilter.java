package irden.space.boot.security;

import irden.space.proxy.plugin.api.security.AuthenticationRequest;
import irden.space.proxy.plugin.api.security.AuthenticationResult;
import irden.space.proxy.plugin.api.security.RestAuthenticator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;


public class PluginAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PluginAuthenticationFilter.class);

    private final DefaultAuthenticatorRegistry registry;

    public PluginAuthenticationFilter(DefaultAuthenticatorRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            AuthenticationRequest authRequest = adapt(request);

            for (RestAuthenticator authenticator : registry.ordered()) {
                if (!authenticator.supports(authRequest)) {
                    continue;
                }

                AuthenticationResult result = authenticator.authenticate(authRequest);
                if (result.authenticated()) {
                    List<SimpleGrantedAuthority> authorities = result.roles().stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(result.principal(), null, authorities)
                    );
                } else {
                    log.debug("Authentication rejected for {} {}: {}",
                            request.getMethod(), request.getRequestURI(), result.failureReason());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, result.failureReason());
                    return;
                }
                break; // первый подходящий аутентификатор принимает
            }
        }

        chain.doFilter(request, response);
    }

    private AuthenticationRequest adapt(HttpServletRequest request) {
        return new AuthenticationRequest() {
            @Override
            public String method() {
                return request.getMethod();
            }

            @Override
            public String path() {
                return request.getRequestURI();
            }

            @Override
            public String header(String name) {
                return request.getHeader(name);
            }
        };
    }
}
