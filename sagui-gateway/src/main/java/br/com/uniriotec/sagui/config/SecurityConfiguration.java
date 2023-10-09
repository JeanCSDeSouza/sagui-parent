package br.com.uniriotec.sagui.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Expõe os beans relacionados a segurança.
 * Sobrescreve o bean SecurityWebFilterChain para gerenciar os acessos e
 * o conversor do JWT enviado pelo Keycloak
 * @Author Jean Carlos
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String KeycloakUrl;
    /**
     * Permite acesso não logado ao Eureka Client e habilita a autenticação para as rotas expostas pelo api gateway
     * @param serverHttpSecurity
     * @return
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity serverHttpSecurity){
        serverHttpSecurity
                .csrf().disable()
                .authorizeExchange( exchange ->
                        exchange.pathMatchers(  "/eureka/**","/eureka/web" ).permitAll()
                                .pathMatchers("/webjars/swagger-ui/**").permitAll()
                                .pathMatchers("/api/v3/api-docs/**").permitAll()
                                .pathMatchers("/v3/api-docs/**").permitAll()
                                .pathMatchers("/api/v3/api-docs/produtos").permitAll()
                                .pathMatchers(HttpMethod.GET,"/api/produtos").permitAll()
                                .pathMatchers(HttpMethod.GET,"/api/produtos/**").permitAll()
                        .anyExchange()
                        .authenticated()
                )
                .cors().configurationSource( request -> {
                    CorsConfiguration configuration = new CorsConfiguration();
                    configuration.setAllowedOrigins(List.of("*"));
                    configuration.setAllowedMethods(List.of("*"));
                    configuration.setAllowedHeaders(List.of("*"));
                    return configuration;
                }).and()
                .oauth2ResourceServer( oauth2 -> oauth2
                        .jwt( jwt -> jwt.jwtAuthenticationConverter( grantedAuthoritiesExtractor() ))
                );
        return serverHttpSecurity.build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        //essa propriedade precisa ser pega do config.
        return ReactiveJwtDecoders.fromIssuerLocation(KeycloakUrl);
    }

    /**
     * Cria o conversor usado para ler as propriedade dos jwt
     * @return Converter
     */
    Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new GrantedAuthoritiesExtractor());
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }

    /**
     * Implementação da classe converter para recuperar as roles do Jwt enviado pelo Keycloak
     * No padrão do jwt enviado pelo keycloak as roles estão na propriedade "realm_acess"
     * Sem este conversor o Spring não consegue encontrar as roles do usuário
     */
    static class GrantedAuthoritiesExtractor
            implements Converter<Jwt, Collection<GrantedAuthority>> {

        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Map<String, List<String> > realmAcess =
                    (Map<String, List<String>>) jwt.getClaims().getOrDefault("realm_access", Collections.emptyList());//O Keycloak retorna as roles do usuaário dentro de "realm_acess"
            return  realmAcess.get("roles").stream()
                    .map( roleName -> "ROLE_" + roleName )//Adiciona suffixo de modo a mapear para o padrão de role do spring security
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
    }
}
