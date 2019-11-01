package org.synyx.urlaubsverwaltung.security.ldap;

import org.junit.Before;
import org.junit.Test;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonService;
import org.synyx.urlaubsverwaltung.testdatacreator.TestDataCreator;

import java.util.Collections;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.synyx.urlaubsverwaltung.person.MailNotification.NOTIFICATION_USER;
import static org.synyx.urlaubsverwaltung.person.Role.USER;


public class LdapUserDataImporterTest {

    private LdapUserDataImporter ldapUserDataImporter;

    private LdapUserService ldapUserServiceMock;
    private PersonService personServiceMock;

    @Before
    public void setUp() {

        ldapUserServiceMock = mock(LdapUserService.class);
        personServiceMock = mock(PersonService.class);

        ldapUserDataImporter = new LdapUserDataImporter(ldapUserServiceMock, personServiceMock);
    }


    @Test
    public void ensureFetchesLdapUsers() {

        ldapUserDataImporter.sync();

        verify(ldapUserServiceMock).getLdapUsers();
    }


    @Test
    public void ensureCreatesPersonIfLdapUserNotYetExists() {

        when(personServiceMock.getPersonByUsername(anyString())).thenReturn(Optional.empty());
        when(ldapUserServiceMock.getLdapUsers())
            .thenReturn(Collections.singletonList(
                new LdapUser("muster", Optional.empty(), Optional.empty(), Optional.empty())));

        ldapUserDataImporter.sync();

        verify(personServiceMock, times(1)).getPersonByUsername("muster");
        verify(personServiceMock)
            .create("muster", null, null, null, singletonList(NOTIFICATION_USER), singletonList(USER));
    }


    @Test
    public void ensureUpdatesPersonIfLdapUserExists() {

        Person person = TestDataCreator.createPerson();

        when(personServiceMock.getPersonByUsername(anyString())).thenReturn(Optional.of(person));
        when(ldapUserServiceMock.getLdapUsers())
            .thenReturn(Collections.singletonList(
                new LdapUser(person.getUsername(), Optional.of("Vorname"), Optional.of("Nachname"),
                    Optional.of("Email"))));

        ldapUserDataImporter.sync();

        verify(personServiceMock, times(1)).getPersonByUsername(person.getUsername());
        assertThat(person.getEmail()).isEqualTo("Email");
        assertThat(person.getFirstName()).isEqualTo("Vorname");
        assertThat(person.getLastName()).isEqualTo("Nachname");
        verify(personServiceMock).save(person);
    }
}
