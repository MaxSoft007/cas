package org.apereo.cas.web.flow.config;

import com.google.common.collect.Lists;
import org.apereo.cas.CentralAuthenticationService;
import org.apereo.cas.authentication.AuthenticationSystemSupport;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.Beans;
import org.apereo.cas.web.flow.CasWebflowConfigurer;
import org.apereo.cas.web.flow.SpengoWebflowConfigurer;
import org.apereo.cas.web.flow.SpnegoCredentialsAction;
import org.apereo.cas.web.flow.SpnegoNegociateCredentialsAction;
import org.apereo.cas.web.flow.client.BaseSpnegoKnownClientSystemsFilterAction;
import org.apereo.cas.web.flow.client.HostNameSpnegoKnownClientSystemsFilterAction;
import org.apereo.cas.web.flow.client.LdapSpnegoKnownClientSystemsFilterAction;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;
import org.springframework.web.util.CookieGenerator;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;
import org.springframework.webflow.execution.Action;

/**
 * This is {@link SpnegoWebflowConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Configuration("spnegoWebflowConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class SpnegoWebflowConfiguration {
    
    @Autowired
    @Qualifier("spnegoPrincipalFactory")
    private PrincipalFactory spnegoPrincipalFactory;
    
    @Autowired
    @Qualifier("warnCookieGenerator")
    private CookieGenerator warnCookieGenerator;
    
    @Autowired
    @Qualifier("centralAuthenticationService")
    private CentralAuthenticationService centralAuthenticationService;

    @Autowired
    @Qualifier("defaultAuthenticationSystemSupport")
    private AuthenticationSystemSupport authenticationSystemSupport;
    
    @Autowired
    @Qualifier("loginFlowRegistry")
    private FlowDefinitionRegistry loginFlowDefinitionRegistry;

    @Autowired
    private FlowBuilderServices flowBuilderServices;

    @Autowired
    private CasConfigurationProperties casProperties;
    
    @Bean
    public CasWebflowConfigurer spnegoWebflowConfigurer() {
        final SpengoWebflowConfigurer w = new SpengoWebflowConfigurer();
        w.setLoginFlowDefinitionRegistry(loginFlowDefinitionRegistry);
        w.setFlowBuilderServices(flowBuilderServices);
        return w;
    }
    
    
    @Bean
    @RefreshScope
    public SpnegoCredentialsAction spnego() {
        final SpnegoCredentialsAction a = new SpnegoCredentialsAction();
        a.setNtlm(casProperties.getAuthn().getSpnego().isNtlm());
        a.setSend401OnAuthenticationFailure(casProperties.getAuthn().getSpnego().isSend401OnAuthenticationFailure());
        a.setAuthenticationSystemSupport(this.authenticationSystemSupport);
        a.setPrincipalFactory(this.spnegoPrincipalFactory);
        a.setWarnCookieGenerator(this.warnCookieGenerator);
        a.setCentralAuthenticationService(this.centralAuthenticationService);
        return a;
    }

    @Bean
    @RefreshScope
    public Action negociateSpnego() {
        final SpnegoNegociateCredentialsAction a = new SpnegoNegociateCredentialsAction();
        a.setMixedModeAuthentication(casProperties.getAuthn().getSpnego().isMixedModeAuthentication());
        a.setNtlm(casProperties.getAuthn().getSpnego().isNtlm());
        final String[] browsers = StringUtils.commaDelimitedListToStringArray(casProperties.getAuthn().getSpnego().getSupportedBrowsers());
        a.setSupportedBrowsers(Lists.newArrayList(browsers));
        return a;
    }

    @Bean
    @RefreshScope
    public Action baseSpnegoClientAction() {
        final BaseSpnegoKnownClientSystemsFilterAction a =
                new BaseSpnegoKnownClientSystemsFilterAction();

        a.setIpsToCheckPattern(casProperties.getAuthn().getSpnego().getIpsToCheckPattern());
        a.setAlternativeRemoteHostAttribute(casProperties.getAuthn().getSpnego().getAlternativeRemoteHostAttribute());
        a.setTimeout(casProperties.getAuthn().getSpnego().getDnsTimeout());
        return a;
    }

    @Bean
    @RefreshScope
    public Action hostnameSpnegoClientAction() {
        final HostNameSpnegoKnownClientSystemsFilterAction a =
                new HostNameSpnegoKnownClientSystemsFilterAction(casProperties.getAuthn().getSpnego().getHostNamePatternString());
        a.setIpsToCheckPattern(casProperties.getAuthn().getSpnego().getIpsToCheckPattern());
        a.setAlternativeRemoteHostAttribute(casProperties.getAuthn().getSpnego().getAlternativeRemoteHostAttribute());
        a.setTimeout(casProperties.getAuthn().getSpnego().getDnsTimeout());
        return a;
    }

    @Lazy
    @Bean
    @RefreshScope
    public Action ldapSpnegoClientAction() {
        final ConnectionFactory connectionFactory = Beans.newPooledConnectionFactory(casProperties.getAuthn().getSpnego().getLdap());
        final SearchFilter filter = Beans.newSearchFilter(casProperties.getAuthn().getSpnego().getLdap().getSearchFilter());
                
        final SearchRequest searchRequest = Beans.newSearchRequest(
                casProperties.getAuthn().getSpnego().getLdap().getBaseDn(),
                filter);
        
        final LdapSpnegoKnownClientSystemsFilterAction l =
                new LdapSpnegoKnownClientSystemsFilterAction(connectionFactory,
                        searchRequest, casProperties.getAuthn().getSpnego().getSpnegoAttributeName());

        l.setIpsToCheckPattern(casProperties.getAuthn().getSpnego().getIpsToCheckPattern());
        l.setAlternativeRemoteHostAttribute(casProperties.getAuthn().getSpnego().getAlternativeRemoteHostAttribute());
        l.setTimeout(casProperties.getAuthn().getSpnego().getDnsTimeout());
        return l;
    }
}
