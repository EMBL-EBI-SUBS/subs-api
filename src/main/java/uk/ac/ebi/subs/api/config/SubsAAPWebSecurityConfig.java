package uk.ac.ebi.subs.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import uk.ac.ebi.tsc.aap.client.security.StatelessAuthenticationEntryPoint;
import uk.ac.ebi.tsc.aap.client.security.StatelessAuthenticationFilter;
import uk.ac.ebi.tsc.aap.client.security.TokenAuthenticationService;

/**
 * AAP security settings. It defines if the various endpoints/paths accessible with or without a JWT token.
 *
 * Created by neilg on 24/05/2017.
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnWebApplication
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@ComponentScan("uk.ac.ebi.tsc.aap.client.security")
@ConditionalOnProperty(prefix = "aap", name = "enabled", matchIfMissing = true)
@Order(SecurityProperties.BASIC_AUTH_ORDER - 15)
public class SubsAAPWebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubsAAPWebSecurityConfig.class);


    @Autowired
    private StatelessAuthenticationEntryPoint unauthorizedHandler;

    @Autowired
    private TokenAuthenticationService tokenAuthenticationService;

    private StatelessAuthenticationFilter statelessAuthenticationFilterBean() throws Exception {
        LOGGER.info("this.tokenAuthenticationService: " + this.tokenAuthenticationService);
        return new StatelessAuthenticationFilter(this.tokenAuthenticationService);
    }

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        LOGGER.info("[StatelessAuthenticationEntryPoint]- " + unauthorizedHandler);

        httpSecurity
                // we don't need CSRF because our token is invulnerable
                .csrf().disable()
                .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
                // don't create session
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests().antMatchers("/").permitAll()
                .antMatchers("/browser/**/*").permitAll()
                .antMatchers("/docs").permitAll()
                .antMatchers("/docs/**/*").permitAll()
                .antMatchers("/health").permitAll()
                .antMatchers("/health/").permitAll()
                .antMatchers("/health/summary").permitAll()
                .antMatchers(HttpMethod.GET,"/info").permitAll()
                .antMatchers(HttpMethod.GET,"/loggers").permitAll()
                .antMatchers(HttpMethod.GET,"/metrics").permitAll()
                .antMatchers(HttpMethod.GET,"/auditevents").permitAll()
                .antMatchers(HttpMethod.GET,"/logfile  ").permitAll()
                .antMatchers(HttpMethod.GET,"/env").permitAll()
                .antMatchers(HttpMethod.GET,"/dump").permitAll()
                .antMatchers(HttpMethod.GET,"/trace").permitAll()
                .antMatchers(HttpMethod.GET,"/uiSupportItems/**").permitAll()
                .antMatchers(HttpMethod.GET,"/submissionPlans/**").permitAll()
                .antMatchers(HttpMethod.GET,"/dataTypes/**").permitAll()
                .antMatchers(HttpMethod.GET,"/checklists/**").permitAll()
                .antMatchers(HttpMethod.GET,"/validationSchemas/**").permitAll()
                .mvcMatchers(HttpMethod.OPTIONS,"/**").permitAll()
                .mvcMatchers(HttpMethod.HEAD,"/**").permitAll()
                .anyRequest().authenticated();

        httpSecurity.addFilterBefore(statelessAuthenticationFilterBean(),
                UsernamePasswordAuthenticationFilter.class);
        // disable page caching
        httpSecurity.headers().cacheControl();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService());
    }

}
