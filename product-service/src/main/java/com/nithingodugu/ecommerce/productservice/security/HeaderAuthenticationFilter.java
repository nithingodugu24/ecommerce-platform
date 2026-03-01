package com.nithingodugu.ecommerce.productservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    )
            throws ServletException, IOException {
        String userIdHeader = request.getHeader("X-USER-ID");
        String userRole = request.getHeader("X-USER-ROLE");

        if (userIdHeader != null && userRole != null){
            List<GrantedAuthority> authorities= List.of(
                    new SimpleGrantedAuthority("ROLE_" + userRole)
            );
           try {
               UUID userId = UUID.fromString(userIdHeader);

               UsernamePasswordAuthenticationToken auth =
                       new UsernamePasswordAuthenticationToken(
                               userId,
                               null,
                               authorities
                       );

               SecurityContextHolder.getContext().setAuthentication(auth);
           } catch (Exception e) {
               log.info("Error parsig uuid, {}", e.getMessage());
           }
        }

        filterChain.doFilter(request, response);

    }
}
