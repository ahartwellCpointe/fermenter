package org.bitbucket.fermenter.stout.mda;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.bitbucket.fermenter.mda.metamodel.element.BaseEnumertionDecorator;
import org.bitbucket.fermenter.mda.metamodel.element.Enum;
import org.bitbucket.fermenter.mda.metamodel.element.Enumeration;

public class JavaEnumeration extends BaseEnumertionDecorator implements Enumeration, JavaNamedElement {

    private List<Enum> decoratedEnums;

    public JavaEnumeration(Enumeration enumerationToDecorate) {
        super(enumerationToDecorate);
    }

    /**
     * @see org.bitbucket.fermenter.stout.mda.EnumerationElement.Enumeration#getEnumList()
     */
    public List<Enum> getEnums() {
        if (decoratedEnums == null) {
            List<Enum> enums = wrapped.getEnums();
            if (CollectionUtils.isEmpty(enums)) {
                decoratedEnums = Collections.emptyList();

            } else {
                Enum e;
                decoratedEnums = new ArrayList<>();
                Iterator<Enum> i = enums.iterator();
                while (i.hasNext()) {
                    e = i.next();
                    decoratedEnums.add(new JavaEnum(e));

                }

            }
        }

        return decoratedEnums;
    }

}
