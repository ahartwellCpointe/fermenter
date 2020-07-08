package org.bitbucket.fermenter.mda.metamodel.element;

/**
 * Provides baseline decorator functionality for {@link Message}.
 * 
 * The goal is to make it easier to apply the decorator pattern in various implementations of generators (e.g., Java,
 * Typescript, Dart) so that each concrete decorate does only has to decorate those aspects of the class that are
 * needed, not all the pass-through methods that each decorate requires by default.
 */
public class BaseMessageDecorator implements Message {

    protected Message wrapped;

    /**
     * New decorator for {@link Message}.
     * 
     * @param messageToDecorate
     *            instance to decorate
     */
    public BaseMessageDecorator(Message messageToDecorate) {
        MetamodelUtils.validateWrappedInstanceIsNonNull(getClass(), messageToDecorate);
        wrapped = messageToDecorate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return wrapped.getName();
    }   

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return wrapped.getText();
    }

}
