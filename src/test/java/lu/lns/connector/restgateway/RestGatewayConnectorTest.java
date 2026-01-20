package lu.lns.connector.restgateway;

import org.identityconnectors.framework.common.objects.*;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class RestGatewayConnectorTest {

    private RestGatewayConnector connector;
    private RestGatewayConfiguration configuration;

    @Before
    public void setup() {
        configuration = new RestGatewayConfiguration();
        configuration.setGatewayUrl("http://localhost:5000");
        configuration.setConnectionTimeout(30000);
        configuration.setRequestTimeout(60000);

        connector = new RestGatewayConnector();
    }

    @Test
    public void testConfiguration() {
        assertNotNull(configuration);
        assertEquals("http://localhost:5000", configuration.getGatewayUrl());
        assertEquals(Integer.valueOf(30000), configuration.getConnectionTimeout());
        assertEquals(Integer.valueOf(60000), configuration.getRequestTimeout());
    }

    @Test
    public void testConfigurationValidation() {
        configuration.validate(); // Should not throw

        configuration.setGatewayUrl("");
        try {
            configuration.validate();
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("cannot be empty"));
        }
    }

    @Test
    public void testConfigurationInvalidUrl() {
        configuration.setGatewayUrl("ftp://invalid.com");
        try {
            configuration.validate();
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must start with http"));
        }
    }

    @Test
    public void testInit() {
        connector.init(configuration);
        assertNotNull(connector.getConfiguration());
        connector.dispose();
    }

    @Test
    public void testSchema() {
        connector.init(configuration);

        Schema schema = connector.schema();

        assertNotNull(schema);
        Set<ObjectClassInfo> objectClasses = schema.getObjectClassInfo();
        assertEquals(4, objectClasses.size());

        // Check User (Account) class
        ObjectClassInfo accountClass = schema.findObjectClassInfo(ObjectClass.ACCOUNT_NAME);
        assertNotNull(accountClass);
        assertTrue(accountClass.is(ObjectClass.ACCOUNT_NAME));

        // Check attributes
        assertNotNull(findAttribute(accountClass, Name.NAME));
        assertNotNull(findAttribute(accountClass, "firstName"));
        assertNotNull(findAttribute(accountClass, "lastName"));
        assertNotNull(findAttribute(accountClass, "email"));
        assertNotNull(findAttribute(accountClass, "enabled"));
        assertNotNull(findAttribute(accountClass, "roles"));

        // Check roles is multi-valued
        AttributeInfo rolesAttr = findAttribute(accountClass, "roles");
        assertTrue(rolesAttr.isMultiValued());

        // Check Role class
        ObjectClassInfo roleClass = schema.findObjectClassInfo("Role");
        assertNotNull(roleClass);
        assertNotNull(findAttribute(roleClass, Name.NAME));
        assertNotNull(findAttribute(roleClass, "description"));

        // Check Service class
        ObjectClassInfo serviceClass = schema.findObjectClassInfo("Service");
        assertNotNull(serviceClass);
        assertNotNull(findAttribute(serviceClass, "url"));

        // Check Organisation class
        ObjectClassInfo orgClass = schema.findObjectClassInfo("Organisation");
        assertNotNull(orgClass);
        assertNotNull(findAttribute(orgClass, "parentOrgRef"));
        assertNotNull(findAttribute(orgClass, "displayName"));

        connector.dispose();
    }

    @Test
    public void testGetEntityType() {
        // This tests the private method indirectly through schema
        connector.init(configuration);

        Schema schema = connector.schema();

        // __ACCOUNT__ should map to "User" entity type
        ObjectClassInfo accountClass = schema.findObjectClassInfo(ObjectClass.ACCOUNT_NAME);
        assertNotNull(accountClass);

        connector.dispose();
    }

    private AttributeInfo findAttribute(ObjectClassInfo classInfo, String name) {
        for (AttributeInfo attr : classInfo.getAttributeInfo()) {
            if (attr.getName().equals(name)) {
                return attr;
            }
        }
        return null;
    }
}
