package org.hyperic.util.Relationship;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by bharel on 4/1/2015.
 */
public class RelationshipModelWrapperTest {

    @Test
    public void testGetRelationshipModelAsString() {
        RelationshipModelWrapper relationshipModelWrapper;
        relationshipModelWrapper = new RelationshipModelWrapper();
        relationshipModelWrapper.put("test 1", Arrays.asList("a", "b", "c"));
        relationshipModelWrapper.put("test 2", Arrays.asList("a,", "b"));
        relationshipModelWrapper.put("test 3", Arrays.asList("a,", "b=", "=c=", ",d,,", "==e=="));

        String relationshipModel = relationshipModelWrapper.toString();
        System.out.println(relationshipModel);

        RelationshipModelWrapper deserializedRelationshipModelWrapper = new RelationshipModelWrapper(relationshipModel);

        Assert.assertEquals(relationshipModelWrapper, deserializedRelationshipModelWrapper);
    }
}
