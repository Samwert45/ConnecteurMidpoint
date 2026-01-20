package lu.lns.connector.restgateway;

import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

@ConnectorClass(
    displayNameKey = "connector.restgateway.display",
    configurationClass = RestGatewayConfiguration.class
)
public class RestGatewayConnector implements PoolableConnector, CreateOp, UpdateDeltaOp, DeleteOp, TestOp, SchemaOp {

    private static final Logger LOG = LoggerFactory.getLogger(RestGatewayConnector.class);

    private RestGatewayConfiguration configuration;
    private RestGatewayClient client;

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void init(Configuration configuration) {
        LOG.info("Initializing REST Gateway Connector");
        this.configuration = (RestGatewayConfiguration) configuration;
        this.configuration.validate();
        this.client = new RestGatewayClient(this.configuration);
        LOG.info("REST Gateway Connector initialized with URL: {}", this.configuration.getGatewayUrl());
    }

    @Override
    public void dispose() {
        LOG.info("Disposing REST Gateway Connector");
        this.client = null;
        this.configuration = null;
    }

    @Override
    public void checkAlive() {
        LOG.debug("Check alive called");
        // Simple check - could ping the gateway if needed
        if (client == null) {
            throw new IllegalStateException("Connector not initialized");
        }
    }

    @Override
    public void test() {
        LOG.info("Testing connection to gateway");
        client.testConnection();
        LOG.info("Connection test successful");
    }

    @Override
    public Schema schema() {
        LOG.debug("Building schema");

        SchemaBuilder schemaBuilder = new SchemaBuilder(RestGatewayConnector.class);

        // User (Account) ObjectClass
        ObjectClassInfoBuilder userClassBuilder = new ObjectClassInfoBuilder();
        userClassBuilder.setType(ObjectClass.ACCOUNT_NAME);
        userClassBuilder.addAttributeInfo(Name.INFO);
        userClassBuilder.addAttributeInfo(AttributeInfoBuilder.build("firstName", String.class));
        userClassBuilder.addAttributeInfo(AttributeInfoBuilder.build("lastName", String.class));
        userClassBuilder.addAttributeInfo(AttributeInfoBuilder.build("email", String.class));
        userClassBuilder.addAttributeInfo(AttributeInfoBuilder.build("enabled", Boolean.class));
        AttributeInfoBuilder rolesBuilder = new AttributeInfoBuilder("roles", String.class);
        rolesBuilder.setMultiValued(true);
        userClassBuilder.addAttributeInfo(rolesBuilder.build());
        userClassBuilder.addAttributeInfo(OperationalAttributeInfos.PASSWORD);
        schemaBuilder.defineObjectClass(userClassBuilder.build());

        // Role ObjectClass
        ObjectClassInfoBuilder roleClassBuilder = new ObjectClassInfoBuilder();
        roleClassBuilder.setType("Role");
        roleClassBuilder.addAttributeInfo(Name.INFO);
        roleClassBuilder.addAttributeInfo(AttributeInfoBuilder.build("description", String.class));
        roleClassBuilder.addAttributeInfo(AttributeInfoBuilder.build("roleType", String.class));
        schemaBuilder.defineObjectClass(roleClassBuilder.build());

        // Service ObjectClass
        ObjectClassInfoBuilder serviceClassBuilder = new ObjectClassInfoBuilder();
        serviceClassBuilder.setType("Service");
        serviceClassBuilder.addAttributeInfo(Name.INFO);
        serviceClassBuilder.addAttributeInfo(AttributeInfoBuilder.build("description", String.class));
        serviceClassBuilder.addAttributeInfo(AttributeInfoBuilder.build("url", String.class));
        serviceClassBuilder.addAttributeInfo(AttributeInfoBuilder.build("enabled", Boolean.class));
        serviceClassBuilder.addAttributeInfo(AttributeInfoBuilder.build("serviceType", String.class));
        schemaBuilder.defineObjectClass(serviceClassBuilder.build());

        // Organisation ObjectClass
        ObjectClassInfoBuilder orgClassBuilder = new ObjectClassInfoBuilder();
        orgClassBuilder.setType("Organisation");
        orgClassBuilder.addAttributeInfo(Name.INFO);
        orgClassBuilder.addAttributeInfo(AttributeInfoBuilder.build("parentOrgRef", String.class));
        orgClassBuilder.addAttributeInfo(AttributeInfoBuilder.build("displayName", String.class));
        orgClassBuilder.addAttributeInfo(AttributeInfoBuilder.build("locality", String.class));
        orgClassBuilder.addAttributeInfo(AttributeInfoBuilder.build("costCenter", String.class));
        schemaBuilder.defineObjectClass(orgClassBuilder.build());

        Schema schema = schemaBuilder.build();
        LOG.info("Schema built with {} object classes", schema.getObjectClassInfo().size());
        return schema;
    }

    @Override
    public Uid create(ObjectClass objectClass, Set<Attribute> createAttributes, OperationOptions options) {
        String entityType = getEntityType(objectClass);
        LOG.info("Creating {} with attributes: {}", entityType, createAttributes);

        Map<String, Object> attributesMap = JsonMapper.attributesToMap(createAttributes);
        String jsonPayload = JsonMapper.createPayload("CREATE", entityType, attributesMap);

        String response = client.post("/create", jsonPayload);

        // Try to extract UID from response, or generate one
        String uid = JsonMapper.parseUidFromResponse(response);
        if (uid == null) {
            // If gateway doesn't return UID, use the __NAME__ value
            Attribute nameAttr = AttributeUtil.find(Name.NAME, createAttributes);
            if (nameAttr != null) {
                uid = AttributeUtil.getStringValue(nameAttr);
            } else {
                uid = java.util.UUID.randomUUID().toString();
            }
        }

        LOG.info("Created {} with UID: {}", entityType, uid);
        return new Uid(uid);
    }

    @Override
    public Set<AttributeDelta> updateDelta(ObjectClass objectClass, Uid uid, Set<AttributeDelta> modifications, OperationOptions options) {
        String entityType = getEntityType(objectClass);
        LOG.info("Updating {} with UID {} - modifications: {}", entityType, uid.getUidValue(), modifications);

        Map<String, Object> attributesMap = JsonMapper.attributeDeltasToMap(modifications);
        String jsonPayload = JsonMapper.createPayload("UPDATE", entityType, uid.getUidValue(), attributesMap);

        client.post("/update", jsonPayload);

        LOG.info("Updated {} with UID: {}", entityType, uid.getUidValue());
        return null; // No side effects
    }

    @Override
    public void delete(ObjectClass objectClass, Uid uid, OperationOptions options) {
        String entityType = getEntityType(objectClass);
        LOG.info("Deleting {} with UID: {}", entityType, uid.getUidValue());

        String jsonPayload = JsonMapper.createPayload("DELETE", entityType, uid.getUidValue(), null);

        client.post("/delete", jsonPayload);

        LOG.info("Deleted {} with UID: {}", entityType, uid.getUidValue());
    }

    private String getEntityType(ObjectClass objectClass) {
        String type = objectClass.getObjectClassValue();

        // Map __ACCOUNT__ to User
        if (ObjectClass.ACCOUNT_NAME.equals(type)) {
            return "User";
        }

        return type;
    }
}
