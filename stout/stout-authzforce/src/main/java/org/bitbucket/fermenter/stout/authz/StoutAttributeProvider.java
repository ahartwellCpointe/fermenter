package org.bitbucket.fermenter.stout.authz;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aeonbits.owner.KrauseningConfigFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bitbucket.fermenter.stout.authz.attribute._2.StoutAttributeExtension;
import org.bitbucket.fermenter.stout.authz.config.AuthorizationConfig;
import org.bitbucket.fermenter.stout.authz.json.StoutAttribute;
import org.ow2.authzforce.core.pdp.api.AttributeFqn;
import org.ow2.authzforce.core.pdp.api.AttributeProvider;
import org.ow2.authzforce.core.pdp.api.BaseNamedAttributeProvider;
import org.ow2.authzforce.core.pdp.api.CloseableNamedAttributeProvider;
import org.ow2.authzforce.core.pdp.api.EnvironmentProperties;
import org.ow2.authzforce.core.pdp.api.EvaluationContext;
import org.ow2.authzforce.core.pdp.api.IndeterminateEvaluationException;
import org.ow2.authzforce.core.pdp.api.value.AttributeBag;
import org.ow2.authzforce.core.pdp.api.value.AttributeValue;
import org.ow2.authzforce.core.pdp.api.value.AttributeValueFactoryRegistry;
import org.ow2.authzforce.core.pdp.api.value.Bags;
import org.ow2.authzforce.core.pdp.api.value.Datatype;
import org.ow2.authzforce.core.pdp.api.value.SimpleValue;
import org.ow2.authzforce.xacml.identifiers.XacmlAttributeId;
import org.ow2.authzforce.xacml.identifiers.XacmlStatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeDesignatorType;

/**
 * Provides an Authzforce attribute provider that allows relatively easy configuration of various attributes for use in
 * the policy decision point.
 */
public class StoutAttributeProvider extends BaseNamedAttributeProvider {

    private static final String XML_SCHEMA_DATE = "http://www.w3.org/2001/XMLSchema#date";

    private static final String XML_SCHEMA_DOUBLE = "http://www.w3.org/2001/XMLSchema#double";

    private static final String XML_SCHEMA_ANY_URI = "http://www.w3.org/2001/XMLSchema#anyURI";

    private static final String XML_SCHEMA_BOOLEAN = "http://www.w3.org/2001/XMLSchema#boolean";

    private static final String XML_SCHEMA_STRING = "http://www.w3.org/2001/XMLSchema#string";

    private static final String XML_SCHEMA_INTEGER = "http://www.w3.org/2001/XMLSchema#integer";

    private static final TypeReference<List<StoutAttribute>> attributeListTypeReference = new TypeReference<List<StoutAttribute>>() {
    };

    private static final Logger logger = LoggerFactory.getLogger(StoutAttributeProvider.class);
    protected AuthorizationConfig config = KrauseningConfigFactory.create(AuthorizationConfig.class);
    protected Map<String, AttributeDesignatorType> supportedDesignatorTypes = new HashMap<>();
    protected Map<Class<StoutAttributePoint>, StoutAttributePoint> pointClassToInstanceMap = new HashMap<>();
    protected Map<String, StoutAttributePoint> idToAttributePointMap = new HashMap<>();

    private StoutAttributeProvider(StoutAttributeExtension conf) {
        super(conf.getId());

        loadAttributeConfiguration();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        // nothing to close
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<AttributeDesignatorType> getProvidedAttributes() {
        return Collections.unmodifiableSet(new HashSet<>(supportedDesignatorTypes.values()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <AV extends AttributeValue> AttributeBag<AV> get(final AttributeFqn attributeGUID,
            final Datatype<AV> attributeDatatype, final EvaluationContext context)
            throws IndeterminateEvaluationException {
        String id = attributeGUID.getId();
        String subject = findSubjectInEnvironmentContext(context);

        // lookup the correct attribute point to use:
        StoutAttributePoint attributePoint = idToAttributePointMap.get(id);
        org.bitbucket.fermenter.stout.authz.AttributeValue<?> retrievedValue = attributePoint.getValueForAttribute(id,
                subject);

        SimpleValue<?> simpleValue = convertRetrievedValueToXacmlFormat(attributeDatatype, id, subject, retrievedValue);

        AttributeBag<AV> attrVals = null;
        if (simpleValue != null) {
            Collection<AV> attributeCollection = new ArrayList<>();
            attributeCollection.add((AV) simpleValue);
            attrVals = Bags.newAttributeBag(attributeDatatype, attributeCollection);
        }

        if (attrVals == null || attrVals.getElementDatatype().equals(attributeDatatype)) {
            return (AttributeBag<AV>) attrVals;
        }

        throw new IndeterminateEvaluationException("Requested datatype (" + attributeDatatype + ") != provided by "
                + this + " (" + attrVals.getElementDatatype() + ")", XacmlStatusCode.MISSING_ATTRIBUTE.value());
    }

    protected <AV extends AttributeValue> SimpleValue<?> convertRetrievedValueToXacmlFormat(
            Datatype<AV> attributeDatatype, String id, String subject,
            org.bitbucket.fermenter.stout.authz.AttributeValue<?> retrievedValue) {
        SimpleValue<?> simpleValue = null;
        if (retrievedValue != null) {
            switch (attributeDatatype.toString()) {
            case XML_SCHEMA_INTEGER:
                simpleValue = retrievedValue.getAsIntegerValue();
                break;
            case XML_SCHEMA_STRING:
                simpleValue = retrievedValue.getAsStringValue();
                break;
            case XML_SCHEMA_BOOLEAN:
                simpleValue = retrievedValue.getAsBooleanValue();
                break;
            case XML_SCHEMA_ANY_URI:
                simpleValue = retrievedValue.getAsAnyUriValue();
                break;
            case XML_SCHEMA_DOUBLE:
                simpleValue = retrievedValue.getAsDoubleValue();
                break;
            case XML_SCHEMA_DATE:
                simpleValue = retrievedValue.getAsDateValue();
                break;
            default:
                simpleValue = retrievedValue.getAsStringValue();
                logger.warn("No type of '{}' was found for attribute '{}', converting to string!", attributeDatatype,
                        id);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Retrieved attribute '{}' for subject '{}' with the value '{}'", id, subject, simpleValue);

        }
        return simpleValue;
    }

    protected String findSubjectInEnvironmentContext(final EvaluationContext context) {
        String subject = null;
        Iterator<Entry<AttributeFqn, AttributeBag<?>>> contextAttributeIterator = context.getNamedAttributes();
        while (contextAttributeIterator.hasNext()) {
            Entry<AttributeFqn, AttributeBag<?>> contextAttribute = contextAttributeIterator.next();
            AttributeFqn attributeFullyQualifiedName = contextAttribute.getKey();
            if (XacmlAttributeId.XACML_1_0_SUBJECT_ID.value().equals(attributeFullyQualifiedName.getId())) {
                subject = contextAttribute.getValue().getSingleElement().toString();
                break;
            }

        }
        return subject;
    }

    /**
     * {@link StoutAttributeExtension} factory
     * 
     */
    public static class Factory extends CloseableNamedAttributeProvider.FactoryBuilder<StoutAttributeExtension> {

        @Override
        public Class<StoutAttributeExtension> getJaxbClass() {
            return StoutAttributeExtension.class;
        }

        @Override
        public DependencyAwareFactory getInstance(StoutAttributeExtension conf, EnvironmentProperties envProperties) {
            return new DependencyAwareFactory() {

                @Override
                public Set<AttributeDesignatorType> getDependencies() {
                    // no dependency
                    return null;
                }

                @Override
                public CloseableNamedAttributeProvider getInstance(AttributeValueFactoryRegistry attrDatatypeFactory,
                        AttributeProvider depAttrProvider) {
                    return new StoutAttributeProvider(conf);
                }
            };
        }

    }

    protected void loadAttributeConfiguration() {
        String attributeLocation = config.getAttributeDefinitionLocation();
        ObjectMapper mapper = new ObjectMapper();
        File root = new File(attributeLocation);
        if (root.exists()) {
            String[] extensions = { "json" };
            Collection<File> files = FileUtils.listFiles(root, extensions, true);
            if (files.isEmpty()) {

                logger.warn(
                        "Your PDP is configured to use custom attributes, but the location that defines "
                                + "these attributes does not contain any attribute definitions! - {}",
                        root.getAbsolutePath());

            }

            for (File attributeDefintionFile : files) {
                List<StoutAttribute> attributes = null;
                try {
                    attributes = mapper.readValue(attributeDefintionFile, attributeListTypeReference);
                    for (StoutAttribute attribute : attributes) {
                        addAttributeDefinition(attribute);

                    }

                } catch (IOException e) {
                    logger.error("Problem loading attributes in file {}!", attributeDefintionFile.getAbsolutePath(), e);
                    attributes = Collections.emptyList();
                }
                logger.info("Found {} attribute definitions in file {}", attributes.size(),
                        attributeDefintionFile.getAbsolutePath());
            }

        } else {
            logger.warn("\n");
            logger.warn("*********************************************************************************");
            logger.warn("Your PDP is configured to use custom attributes, but the location that defines these "
                    + "attributes does not exist! - {}", root.getAbsolutePath());
            logger.warn("Update the your authorization.properties via Krausening to point to a valid location "
                    + "for attribute json files!");
            logger.warn("*********************************************************************************");
            logger.warn("\n");

        }
    }

    protected void addAttributeDefinition(StoutAttribute attribute) {
        String id = attribute.getId();

        AttributeDesignatorType designatorType = StoutAttributeUtils.translateAttributeToXacmlFormat(attribute);
        AttributeDesignatorType existingDesignatorType = supportedDesignatorTypes.put(id, designatorType);

        StoutAttributePoint attributePoint = findAttributePointImplementation(attribute);
        idToAttributePointMap.put(id, attributePoint);

        logger.info("Translated Stout attribute definition '{}' into the fully qualified \n\t{}", id, designatorType);

        if (existingDesignatorType != null) {
            logger.warn("Multiple attributes named '{}' exist!  The last one in read will be used {}", id,
                    designatorType);
        }
    }

    protected StoutAttributePoint findAttributePointImplementation(StoutAttribute attribute) {
        StoutAttributePoint attributePoint = null;

        String attributePointClassName = null;
        try {
            attributePointClassName = attribute.getAttributePointClass();
            if (StringUtils.isBlank(attributePointClassName)) {
                logger.error("No attribute point specified for attribute '{}'!", attribute.getId());

            } else {
                Class<StoutAttributePoint> attributePointClass = (Class<StoutAttributePoint>) Class
                        .forName(attributePointClassName);

                // reuse an existing instance if we have already encountered this class:
                attributePoint = pointClassToInstanceMap.get(attributePointClass);

                if (attributePoint == null) {
                    // add a new reusable instance if we have not already encountered this class:
                    attributePoint = attributePointClass.newInstance();
                    pointClassToInstanceMap.put(attributePointClass, attributePoint);
                    logger.debug("Instantiated AttributePoint '{}'", attributePointClass.getName());
                }

            }

        } catch (ClassNotFoundException e) {
            logger.error("Could not find attribute point '{}' in classpath!", attributePointClassName);

        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("Could not instantiate attribute point '" + attributePointClassName + "'!", e);

        }      

        return attributePoint;
    }

}
