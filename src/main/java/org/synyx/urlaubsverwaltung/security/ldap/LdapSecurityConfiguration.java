package org.synyx.urlaubsverwaltung.security.ldap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.synyx.urlaubsverwaltung.person.PersonService;
import org.synyx.urlaubsverwaltung.security.PersonSyncService;

@Configuration
public class LdapSecurityConfiguration {

    @Configuration
    @ConditionalOnProperty(name = "uv.security.auth", havingValue = "ldap")
    static class LdapAuthConfiguration {

        private final DirectoryServiceSecurityProperties directoryServiceSecurityProperties;
        private final LdapSecurityConfigurationProperties ldapProperties;

        @Autowired
        public LdapAuthConfiguration(DirectoryServiceSecurityProperties directoryServiceSecurityProperties, LdapSecurityConfigurationProperties ldapProperties) {
            this.directoryServiceSecurityProperties = directoryServiceSecurityProperties;
            this.ldapProperties = ldapProperties;
        }

        @Bean
        public LdapContextSource ldapContextSource() {

            LdapContextSource source = new LdapContextSource();
            source.setUserDn(ldapProperties.getManagerDn());
            source.setPassword(ldapProperties.getManagerPassword());
            source.setBase(ldapProperties.getBase());
            source.setUrl(ldapProperties.getUrl());

            return source;
        }

        @Bean
        public LdapAuthoritiesPopulator authoritiesPopulator() {

            return new DefaultLdapAuthoritiesPopulator(ldapContextSource(), null);
        }

        @Bean
        public FilterBasedLdapUserSearch ldapUserSearch() {

            String searchBase = ldapProperties.getUserSearchBase();
            String searchFilter = ldapProperties.getUserSearchFilter();

            return new FilterBasedLdapUserSearch(searchBase, searchFilter, ldapContextSource());
        }

        @Bean
        public LdapAuthenticator authenticator() {

            BindAuthenticator authenticator = new BindAuthenticator(ldapContextSource());
            authenticator.setUserSearch(ldapUserSearch());

            return authenticator;
        }

        @Bean
        public AuthenticationProvider ldapAuthenticationProvider(LdapPersonContextMapper ldapPersonContextMapper) {

            LdapAuthenticationProvider ldapAuthenticationProvider = new LdapAuthenticationProvider(authenticator(), authoritiesPopulator());
            ldapAuthenticationProvider.setUserDetailsContextMapper(ldapPersonContextMapper);

            return ldapAuthenticationProvider;
        }


        @Bean
        public LdapUserMapper ldapUserMapper() {

            return new LdapUserMapper(directoryServiceSecurityProperties);
        }

        @Bean
        public LdapPersonContextMapper personContextMapper(PersonService personService, PersonSyncService personSyncService,
                                                           LdapUserMapper ldapUserMapper) {
            return new LdapPersonContextMapper(personService, personSyncService, ldapUserMapper);
        }

        @Bean
        public PersonSyncService ldapSyncService(PersonService personService) {
            return new PersonSyncService(personService);
        }
    }

    @Configuration
    @ConditionalOnExpression("'${uv.security.auth}'=='ldap' and '${uv.security.ldap.sync.enabled}'=='true'")
    public static class LdapAuthSyncConfiguration {

        private final DirectoryServiceSecurityProperties directoryServiceSecurityProperties;
        private final LdapSecurityConfigurationProperties ldapProperties;

        @Autowired
        public LdapAuthSyncConfiguration(DirectoryServiceSecurityProperties directoryServiceSecurityProperties, LdapSecurityConfigurationProperties ldapProperties) {
            this.directoryServiceSecurityProperties = directoryServiceSecurityProperties;
            this.ldapProperties = ldapProperties;
        }

        @Bean
        public LdapContextSourceSync ldapContextSourceSync() {
            return new LdapContextSourceSync(ldapProperties);
        }

        @Bean
        public LdapUserDataImporter ldapUserDataImporter(LdapUserService ldapUserService, PersonSyncService personSyncService,
                                                         PersonService personService) {
            return new LdapUserDataImporter(ldapUserService, personSyncService, personService, directoryServiceSecurityProperties);
        }

        @Bean
        public LdapUserServiceImpl ldapUserService(LdapTemplate ldapTemplate, LdapUserMapper ldapUserMapper) {
            return new LdapUserServiceImpl(ldapTemplate, ldapUserMapper, directoryServiceSecurityProperties);
        }

        @Bean
        public LdapTemplate ldapTemplate() {
            return new UVLdapTemplate(ldapContextSourceSync());
        }
    }
}
