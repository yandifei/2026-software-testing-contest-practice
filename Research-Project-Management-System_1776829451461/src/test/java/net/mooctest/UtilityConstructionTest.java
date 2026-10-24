package net.mooctest;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Covers private utility constructors counted by PIT but filtered by JaCoCo. */
public class UtilityConstructionTest {
    @Test
    void graphUtilityConstructorIsPrivateAndCanBeInitialized() throws Exception {
        Constructor<GraphUtils> constructor = GraphUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        assertEquals(GraphUtils.class, constructor.newInstance().getClass());
    }

    @Test
    void idUtilityConstructorIsPrivateAndCanBeInitialized() throws Exception {
        Constructor<IdGenerator> constructor = IdGenerator.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        assertEquals(IdGenerator.class, constructor.newInstance().getClass());
    }
}
