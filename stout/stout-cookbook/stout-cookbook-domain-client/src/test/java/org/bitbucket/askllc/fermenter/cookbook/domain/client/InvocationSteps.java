package org.bitbucket.askllc.fermenter.cookbook.domain.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.bitbucket.askllc.fermenter.cookbook.domain.client.service.ContractTestDelegate;
import org.bitbucket.askllc.fermenter.cookbook.domain.client.service.impl.DelegateMaintenanceTransactionSynchronization;
import org.bitbucket.askllc.fermenter.cookbook.domain.enumeration.SimpleDomainEnumeration;
import org.bitbucket.askllc.fermenter.cookbook.domain.transfer.SimpleDomain;
import org.bitbucket.fermenter.stout.messages.Message;
import org.bitbucket.fermenter.stout.messages.MessageManager;
import org.bitbucket.fermenter.stout.messages.MessageManagerInitializationDelegate;
import org.bitbucket.fermenter.stout.messages.Messages;
import org.bitbucket.fermenter.stout.page.PageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

@ContextConfiguration({ "classpath:application-test-context.xml", "classpath:h2-spring-ds-context.xml" })
@Transactional
public class InvocationSteps {

    private static final Logger logger = LoggerFactory.getLogger(InvocationSteps.class);

    @Inject
    private ContractTestDelegate delegate;

    private Messages messages;
    private String standardTypeResponseValue;
    private SimpleDomainEnumeration enumerationTypeResponseValue;
    private SimpleDomain entityResponseValue;
    private Collection<String> multipleStandardTypeResponseValue;
    private Collection<SimpleDomain> multipleEntitiesResponseValue;
    private PageWrapper<String> multiplePagedStandardTypeResponseValue;
    private PageWrapper<SimpleDomain> multiplePagedEntitiesResponseValue;

    private long start;
    private long stop;

    @Before("@invocationOfRemoteService")
    public void setUp() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("testUser", "abc123");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertNotNull("Could not access service delegate!", delegate);

        MessageManagerInitializationDelegate.initializeMessageManager();

        startInvocationTimer();
    }

    @After("@invocationOfRemoteService")
    public void cleanUp() {
        stopAndLogInvocationTimer();

        MessageManagerInitializationDelegate.cleanupMessageManager();
                        
        messages = null;
    }

    @When("^a service with a void return type is invoked$")
    public void a_service_with_a_void_return_type_is_invoked() throws Throwable {
        delegate.voidResponseMethod();
        
        storeMessagesForFinalCheck();
    }

    @When("^a service with a standard return type is invoked$")
    public void a_service_with_a_standard_return_type_is_invoked() throws Throwable {
        standardTypeResponseValue = delegate.stringResponseMethod();
        storeMessagesForFinalCheck();
    }

    @When("^a service with an enumeration return type is invoked$")
    public void a_service_with_an_enumeration_return_type_is_invoked() throws Throwable {
        enumerationTypeResponseValue = delegate.enumerationResponseMethod();
        storeMessagesForFinalCheck();
    }

    @When("^a service with an entity return type is invoked$")
    public void a_service_with_an_entity_return_type_is_invoked() throws Throwable {
        entityResponseValue = delegate.entityResponseMethod();
        storeMessagesForFinalCheck();
    }

    @When("^a service with a collection of standard types as the return type is invoked$")
    public void a_service_with_a_collection_of_standard_types_as_the_return_type_is_invoked() throws Throwable {
        multipleStandardTypeResponseValue = delegate.multipleStringsResponseMethod();
        storeMessagesForFinalCheck();
    }

    @When("^a service with a collection of entities as the return type is invoked$")
    public void a_service_with_a_collection_of_entities_as_the_return_type_is_invoked() throws Throwable {
        multipleEntitiesResponseValue = delegate.multipleEntitiesResponseMethod();
        storeMessagesForFinalCheck();
    }

    @When("^a service with a paged collection of standard types as the return type is invoked$")
    public void a_service_with_a_paged_collection_of_standard_types_as_the_return_type_is_invoked() throws Throwable {
        multiplePagedStandardTypeResponseValue = delegate.multipleStringsPagedResponseMethod(1, 100);
        storeMessagesForFinalCheck();
    }

    @When("^a service with a paged collection of entities as the return type is invoked$")
    public void a_service_with_a_paged_collection_of_entities_as_the_return_type_is_invoked() throws Throwable {
        multiplePagedEntitiesResponseValue = delegate.multipleEntitiesPagedResponseMethod(1, 100);
        storeMessagesForFinalCheck();
    }

    @When("^a service with no parameters is invoked$")
    public void a_service_with_no_parameters_is_invoked() throws Throwable {
        delegate.noParameterMethod();
        storeMessagesForFinalCheck();
    }

    @When("^a service with a standard type parameter is invoked$")
    public void a_service_with_a_standard_type_parameter_is_invoked() throws Throwable {
        String input = RandomStringUtils.randomAlphanumeric(10);
        delegate.stringParameterMethod(input);
        storeMessagesForFinalCheck();
    }

    @When("^a service with an enumeration type parameter is invoked$")
    public void a_service_with_an_enumeration_type_parameter_is_invoked() throws Throwable {
        SimpleDomainEnumeration enumerationValue = SimpleDomainEnumeration.FOURTH;
        delegate.enumerationParameterMethod(enumerationValue);
        storeMessagesForFinalCheck();
    }

    @When("^a service with an entity parameter is invoked$")
    public void a_service_with_an_entity_parameter_is_invoked() throws Throwable {
        SimpleDomain entity = createRandomEntity();
        delegate.entityParameterMethod(entity);
        storeMessagesForFinalCheck();
    }

    @When("^a service with a collection of entities as a parameter is invoked$")
    public void a_service_with_a_collection_of_entities_as_a_parameter_is_invoked() throws Throwable {
        List<SimpleDomain> entities = new ArrayList<>();
        entities.add(createRandomEntity());
        entities.add(createRandomEntity());

        delegate.multipleEntitiesParameterMethod(entities);
        storeMessagesForFinalCheck();
    }

    @When("^a service with both a standard type and entity parameter is invoked$")
    public void a_service_with_both_a_standard_type_and_entity_parameter_is_invoked() throws Throwable {
        String standardType = RandomStringUtils.randomAlphanumeric(10);
        SimpleDomain entity = createRandomEntity();

        delegate.entityAndStringParametersMethod(standardType, entity);
        storeMessagesForFinalCheck();
    }

    @When("^a service that generates an single error message is invoked$")
    public void a_service_that_generates_an_single_error_message_is_invoked() throws Throwable {
        delegate.errorMessagesReturnedMethod();
        storeMessagesForFinalCheck();
    }
    
    @When("^a flush is called while outside a transaction$")
    public void a_flush_is_called_while_outside_a_transaction() throws Throwable {
        DelegateMaintenanceTransactionSynchronization sync = new DelegateMaintenanceTransactionSynchronization();
        sync.flush();
        
        delegate.voidResponseMethod();
        
        storeMessagesForFinalCheck();
    }    

    @Then("^a valid standard type is returned$")
    public void a_valid_standard_type_is_returned() throws Throwable {
        assertTrue("A string value should have been returned!", StringUtils.isNoneBlank(standardTypeResponseValue));
    }

    @Then("^a valid enumeration type is returned$")
    public void a_valid_enumeration_type_is_returned() throws Throwable {
        assertEquals("An enumeration value should have been returned!", SimpleDomainEnumeration.FIRST,
                enumerationTypeResponseValue);
    }

    @Then("^a valid entity is returned$")
    public void a_valid_entity_is_returned() throws Throwable {
        assertNotNull("An entity value should have been returned!", entityResponseValue);
    }

    @Then("^a valid collection of standard types is returned$")
    public void a_valid_collection_of_standard_types_is_returned() throws Throwable {
        assertNotNull("A collection of standard types should have been returned!", multipleStandardTypeResponseValue);
        assertEquals("Multiple values should have been returned!", 2, multipleStandardTypeResponseValue.size());
    }

    @Then("^a valid collection of entities is returned$")
    public void a_valid_collection_of_entities_is_returned() throws Throwable {
        assertNotNull("A collection of entities should have been returned!", multipleEntitiesResponseValue);
        assertEquals("Multiple values should have been returned!", 2, multipleEntitiesResponseValue.size());
    }

    @Then("^a valid paged collection of standard types is returned$")
    public void a_valid_paged_collection_of_standard_types_is_returned() throws Throwable {
        assertNotNull("A paged collection of standard types should have been returned!",
                multiplePagedStandardTypeResponseValue);
        assertEquals("Page count should have been returned!", 2,
                multiplePagedStandardTypeResponseValue.getNumberOfElements().intValue());
        assertEquals("Multiple paged values should have been returned!", 2,
                multiplePagedStandardTypeResponseValue.getContent().size());
    }

    @Then("^a valid paged collection of entities is returned$")
    public void a_valid_paged_collection_of_entities_is_returned() throws Throwable {
        assertNotNull("A paged collection of entities should have been returned!", multiplePagedEntitiesResponseValue);
        assertEquals("Page count should have been returned!", 2,
                multiplePagedEntitiesResponseValue.getNumberOfElements().intValue());
        assertEquals("Multiple paged values should have been returned!", 2,
                multiplePagedEntitiesResponseValue.getContent().size());
    }

    @Then("^a single informational messages is returned indicating successful invocation$")
    public void a_single_informational_messages_is_returned_indicating_successful_invocation() throws Throwable {
        assertEquals("Expected one info message to indicate success!", 1, messages.getInformationalMessageCount());
    }

    @Then("^a single error messages is returned indicating a busines-logic troubled invocation$")
    public void a_single_error_messages_is_returned_indicating_a_busines_logic_troubled_invocation() throws Throwable {
        assertEquals("Expected one error message to indicate success!", 1, messages.getErrorMessageCount());
    }
    
    @Then("^no errors result$")
    public void no_errors_result() throws Throwable {
        assertEquals("No error messages expected!", 0, messages.getErrorMessageCount());
    }    

    private void storeMessagesForFinalCheck() {
        messages = MessageManager.getMessages();
    }

    private SimpleDomain createRandomEntity() {
        SimpleDomain entity = new SimpleDomain();
        entity.setName(RandomStringUtils.randomAlphabetic(10));
        return entity;
    }

    private void startInvocationTimer() {
        start = System.currentTimeMillis();
    }

    private void stopAndLogInvocationTimer() {
        stop = System.currentTimeMillis();
        Message message = messages.getAllMessages().iterator().next();
        logger.info("**REMOTE** Invocation of {} took {}ms", message.getInserts().iterator().next(),(stop - start));
    }

}
