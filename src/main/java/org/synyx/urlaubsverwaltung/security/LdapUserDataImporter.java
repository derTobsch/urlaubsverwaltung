package org.synyx.urlaubsverwaltung.security;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonService;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Import person data from configured LDAP or Active Directory.
 */
@Service
@Transactional
@ConditionalOnExpression(
    "('${uv.security.auth}'=='activeDirectory' and '${uv.security.activeDirectory.sync.enabled}'=='true') or ('${uv.security.auth}'=='ldap' and '${uv.security.ldap.sync.enabled}'=='true')" // NOSONAR
)
public class LdapUserDataImporter {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final LdapUserService ldapUserService;
    private final LdapSyncService ldapSyncService;
    private final PersonService personService;

    @Autowired
    public LdapUserDataImporter(LdapUserService ldapUserService, LdapSyncService ldapSyncService,
        PersonService personService) {

        this.ldapUserService = ldapUserService;
        this.ldapSyncService = ldapSyncService;
        this.personService = personService;
    }

    // Sync LDAP/AD data during startup and on uv.cron.ldapSync
    @PostConstruct
    @Scheduled(cron = "${uv.cron.ldapSync}")
    public void sync() {

        LOG.info("STARTING LDAP SYNC --------------------------------------------------------------------------------");

        List<LdapUser> users = ldapUserService.getLdapUsers();

        LOG.info("Found {} user(s)", users.size());

        for (LdapUser user : users) {
            String username = user.getUsername();
            Optional<String> firstName = user.getFirstName();
            Optional<String> lastName = user.getLastName();
            Optional<String> email = user.getEmail();

            Optional<Person> optionalPerson = personService.getPersonByLogin(username);

            if (optionalPerson.isPresent()) {
                ldapSyncService.syncPerson(optionalPerson.get(), firstName, lastName, email);
            } else {
                ldapSyncService.createPerson(username, firstName, lastName, email);
            }
        }

        LOG.info("DONE LDAP SYNC ------------------------------------------------------------------------------------");
    }
}
