package br.com.uniriotec.sagui.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration{
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf().disable()
                .authorizeHttpRequests( request -> request
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers( HttpMethod.GET, "/produtos/admin").authenticated()
                        .anyRequest().permitAll()
                )
//                .cors().configurationSource( request -> {
//                    CorsConfiguration configuration = new CorsConfiguration();
//                    configuration.setAllowedOrigins(List.of("*"));
//                    configuration.setAllowedMethods(List.of("*"));
//                    configuration.setAllowedHeaders(List.of("*"));
//                    return configuration;
//                })
//                .and()
                .oauth2ResourceServer( oauth2 -> oauth2
                        .jwt( jwt -> jwt.jwtAuthenticationConverter( grantedAuthoritiesExtractor() ) ) );
        return httpSecurity.build();
    }
    /**
     * Cria o conversor usado para ler as propriedade dos jwt
     * @return Converter
     */
    Converter<Jwt, AbstractAuthenticationToken> grantedAuthoritiesExtractor() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new GrantedAuthoritiesExtractor());
        return jwtAuthenticationConverter;
    }
    /**
     * Implementação da classe de extração para recuperar as roles do Jwt enviado pelo Keycloak
     * No padrão do jwt enviado pelo keycloak as roles estão na propriedade "realm_acess"
     * Sem este conversor o Spring não consegue encontrar as roles do usuário
     */
    static class GrantedAuthoritiesExtractor
            implements Converter<Jwt, Collection<GrantedAuthority>> {

        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Map<String, List<String>> realmAcess =
                    (Map<String, List<String>>) jwt.getClaims().getOrDefault("realm_access", Collections.emptyList());//O Keycloak retorna as roles do usuaário dentro de "realm_acess"
            return  realmAcess.get("roles").stream()
                    .map( roleName -> "ROLE_" + roleName )//Adiciona suffixo de modo a mapear para o padrão de role do spring security
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());//todo sonar indica trocar para ".toList()", é preciso analisar o comportamento desse retorno.
        }
    }
}
