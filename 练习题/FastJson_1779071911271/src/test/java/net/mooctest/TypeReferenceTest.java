package net.mooctest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.fastjson.util.ParameterizedTypeImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests type capture, ordered substitution and canonicalization without leaking cache state. */
class TypeReferenceTest {
    private ConcurrentMap<Type, Type> savedCache;

    @BeforeEach
    void isolateCache() {
        savedCache = TypeReference.classTypeCache;
        TypeReference.classTypeCache = new ConcurrentHashMap<>();
    }

    @AfterEach
    void restoreCache() {
        TypeReference.classTypeCache = savedCache;
    }

    @Test
    void capturesSimpleClassAndPublishesItInCache() {
        TypeReference<String> reference = new TypeReference<String>() { };

        assertSame(String.class, reference.getType());
        assertSame(String.class, TypeReference.classTypeCache.get(String.class));
        assertEquals(1, TypeReference.classTypeCache.size());
    }

    @Test
    void capturesNestedArgumentsWithoutLosingTheirStructure() {
        Type result = new TypeReference<Map<String, List<Integer>>>() { }.getType();

        ParameterizedType map = parameterized(result, Map.class);
        assertSame(String.class, map.getActualTypeArguments()[0]);
        ParameterizedType list = parameterized(map.getActualTypeArguments()[1], List.class);
        assertArrayEquals(new Type[] {Integer.class}, list.getActualTypeArguments());
    }

    @Test
    void noArgumentConstructorReusesEqualCachedTypeByIdentity() {
        Type canonical = ((ParameterizedType) StringListReference.class.getGenericSuperclass())
                .getActualTypeArguments()[0];
        TypeReference.classTypeCache.put(canonical, canonical);

        Type result = new TypeReference<List<String>>() { }.getType();

        assertSame(canonical, result);
        assertEquals(1, TypeReference.classTypeCache.size());
    }

    @Test
    void equalTypesCapturedByDifferentAnonymousClassesShareOneEntry() {
        Type first = new TypeReference<List<Long>>() { }.getType();
        Type second = new TypeReference<List<Long>>() { }.getType();

        assertSame(first, second);
        assertSame(first, TypeReference.classTypeCache.get(first));
        assertEquals(1, TypeReference.classTypeCache.size());
    }

    @Test
    void capturesWildcardsAndArrayTypes() {
        ParameterizedType list = parameterized(
                new TypeReference<List<? extends Number>>() { }.getType(), List.class);
        WildcardType wildcard = assertInstanceOf(WildcardType.class, list.getActualTypeArguments()[0]);
        assertArrayEquals(new Type[] {Number.class}, wildcard.getUpperBounds());
        assertArrayEquals(new Type[0], wildcard.getLowerBounds());
        assertSame(String[].class, new TypeReference<String[]>() { }.getType());

        GenericArrayType array = assertInstanceOf(GenericArrayType.class,
                new TypeReference<List<String>[]>() { }.getType());
        ParameterizedType component = parameterized(array.getGenericComponentType(), List.class);
        assertArrayEquals(new Type[] {String.class}, component.getActualTypeArguments());
    }

    @Test
    void predefinedListStringTypeHasThePubliclyAdvertisedArguments() {
        ParameterizedType type = parameterized(TypeReference.LIST_STRING, List.class);
        assertArrayEquals(new Type[] {String.class}, type.getActualTypeArguments());
    }

    @Test
    void substitutesMultipleVariablesInOrderAndCachesTheResult() {
        Type result = new MapReference<String, Integer>(String.class, Integer.class).getType();

        ParameterizedType map = parameterized(result, Map.class);
        assertArrayEquals(new Type[] {String.class, Integer.class}, map.getActualTypeArguments());
        assertSame(MapReference.class, map.getOwnerType());
        assertSame(result, TypeReference.classTypeCache.get(result));
        assertEquals(1, TypeReference.classTypeCache.size());
    }

    @Test
    void parameterizedConstructorReusesCanonicalObjectWhenCacheAlreadyContainsKey() {
        Type canonical = new ParameterizedTypeImpl(
                new Type[] {String.class}, ListReference.class, List.class);
        TypeReference.classTypeCache.put(canonical, canonical);

        Type result = new ListReference<String>(String.class).getType();

        assertSame(canonical, result);
        assertEquals(1, TypeReference.classTypeCache.size());
    }

    @Test
    void repeatedParameterizedConstructionUsesCacheButDifferentArgumentsStayDistinct() {
        Type strings = new ListReference<String>(String.class).getType();
        Type stringsAgain = new ListReference<String>(String.class).getType();
        Type integers = new ListReference<Integer>(Integer.class).getType();

        assertSame(strings, stringsAgain);
        assertNotSame(strings, integers);
        assertArrayEquals(new Type[] {Integer.class}, parameterized(integers, List.class).getActualTypeArguments());
        assertEquals(2, TypeReference.classTypeCache.size());
    }

    @Test
    void emptySubstitutionArrayLeavesAllVariablesUnresolved() {
        TypeVariable<?>[] variables = MapReference.class.getTypeParameters();
        ParameterizedType result = parameterized(new MapReference<>().getType(), Map.class);

        assertArrayEquals(variables, result.getActualTypeArguments());
    }

    @Test
    void exhaustedSubstitutionArrayLeavesOnlyRemainingVariablesUnresolved() {
        ParameterizedType result = parameterized(new MapReference<>(String.class).getType(), Map.class);

        assertArrayEquals(new Type[] {String.class, MapReference.class.getTypeParameters()[1]},
                result.getActualTypeArguments());
    }

    @Test
    void fixedArgumentDoesNotConsumeAReplacementAndSurplusReplacementsAreIgnored() {
        ParameterizedType result = parameterized(
                new FixedKeyReference<Integer>(Integer.class, Double.class).getType(), Map.class);

        assertArrayEquals(new Type[] {String.class, Integer.class}, result.getActualTypeArguments());
    }

    @Test
    void alreadyConcreteArgumentsRemainUnchangedWhenReplacementsAreSupplied() {
        ParameterizedType result = parameterized(
                new TypeReference<Map<String, Integer>>(Double.class, Long.class) { }.getType(), Map.class);

        assertArrayEquals(new Type[] {String.class, Integer.class}, result.getActualTypeArguments());
    }

    @Test
    void normalizesPrimitiveGenericArraySubstitutionsAtTheTopLevel() {
        GenericArrayType primitiveArray = () -> int.class;
        GenericArrayType primitiveMatrix = () -> primitiveArray;
        ParameterizedType result = parameterized(
                new MapReference<>(primitiveArray, primitiveMatrix).getType(), Map.class);

        assertArrayEquals(new Type[] {int[].class, int[][].class}, result.getActualTypeArguments());
    }

    @Test
    void preservesUnresolvedGenericArrayInsteadOfSubstitutingItsComponent() {
        ParameterizedType result = parameterized(new ArrayReference<String>(String.class).getType(), List.class);
        GenericArrayType array = assertInstanceOf(GenericArrayType.class, result.getActualTypeArguments()[0]);

        assertSame(ArrayReference.class.getTypeParameters()[0], array.getGenericComponentType());
    }

    @Test
    void substitutesNestedVariablesInOrder() {
        ParameterizedType outer = parameterized(
                new NestedMapReference<String, Integer>(String.class, Integer.class).getType(), List.class);
        ParameterizedType map = parameterized(outer.getActualTypeArguments()[0], Map.class);

        assertArrayEquals(new Type[] {String.class, Integer.class}, map.getActualTypeArguments());
        assertSame(NestedMapReference.class, map.getOwnerType());
    }

    @Test
    void nestedVariablesStayUnresolvedWhenNoReplacementsExist() {
        ParameterizedType outer = parameterized(new NestedMapReference<>().getType(), List.class);
        ParameterizedType map = parameterized(outer.getActualTypeArguments()[0], Map.class);

        assertArrayEquals(NestedMapReference.class.getTypeParameters(), map.getActualTypeArguments());
    }

    @Test
    void nestedSubstitutionStopsExactlyAtTheEndOfReplacementArray() {
        ParameterizedType outer = parameterized(new NestedMapReference<>(Long.class).getType(), List.class);
        ParameterizedType map = parameterized(outer.getActualTypeArguments()[0], Map.class);

        assertArrayEquals(new Type[] {Long.class, NestedMapReference.class.getTypeParameters()[1]},
                map.getActualTypeArguments());
    }

    @Test
    void outerVariableConsumptionIsPassedIntoNestedSubstitution() {
        ParameterizedType outer = parameterized(
                new VariableThenNestedReference<String, Integer>(String.class, Integer.class).getType(), Map.class);
        assertSame(String.class, outer.getActualTypeArguments()[0]);
        ParameterizedType nested = parameterized(outer.getActualTypeArguments()[1], List.class);

        assertArrayEquals(new Type[] {Integer.class}, nested.getActualTypeArguments());
    }

    @Test
    void normalizesPrimitiveGenericArraysInsideNestedParameterizedTypes() {
        GenericArrayType primitiveArray = () -> long.class;
        GenericArrayType primitiveMatrix = () -> primitiveArray;
        ParameterizedType outer = parameterized(
                new NestedMapReference<>(primitiveArray, primitiveMatrix).getType(), List.class);
        ParameterizedType map = parameterized(outer.getActualTypeArguments()[0], Map.class);

        assertArrayEquals(new Type[] {long[].class, long[][].class}, map.getActualTypeArguments());
    }

    @Test
    void nestedConcreteArgumentsAndGenericArrayComponentsArePreserved() {
        ParameterizedType outer = parameterized(
                new NestedArrayReference<String>(Integer.class).getType(), List.class);
        ParameterizedType nested = parameterized(outer.getActualTypeArguments()[0], Map.class);
        assertSame(String.class, nested.getActualTypeArguments()[0]);
        GenericArrayType array = assertInstanceOf(GenericArrayType.class, nested.getActualTypeArguments()[1]);

        assertSame(NestedArrayReference.class.getTypeParameters()[0], array.getGenericComponentType());
    }

    @Test
    void characterizationRecursiveSubstitutionDropsTheIntermediateContainer() {
        // Known behavior: List<List<List<T>>> becomes List<List<String>>.
        // This records the recursive early-return defect; it does not endorse it as the desired contract.
        ParameterizedType outer = parameterized(new DeepReference<String>(String.class).getType(), List.class);
        ParameterizedType inner = parameterized(outer.getActualTypeArguments()[0], List.class);

        assertArrayEquals(new Type[] {String.class}, inner.getActualTypeArguments());
    }

    @Test
    void characterizationRecursiveEarlyReturnUsesAdvancedIndexAndDropsOtherArguments() {
        // Known behavior: the Map container is lost when the nested List is encountered.
        ParameterizedType outer = parameterized(
                new DeepMapReference<String, Integer>(String.class, Integer.class).getType(), List.class);
        ParameterizedType inner = parameterized(outer.getActualTypeArguments()[0], List.class);

        assertArrayEquals(new Type[] {Integer.class}, inner.getActualTypeArguments());
    }

    @Test
    void characterizationNestedConsumptionDoesNotAdvanceTheOuterReplacementIndex() {
        // Known behavior: the first replacement is reused for the second, outer variable.
        ParameterizedType outer = parameterized(
                new NestedThenVariableReference<String, Integer>(String.class, Integer.class).getType(), Map.class);
        ParameterizedType inner = parameterized(outer.getActualTypeArguments()[0], List.class);

        assertArrayEquals(new Type[] {String.class}, inner.getActualTypeArguments());
        assertSame(String.class, outer.getActualTypeArguments()[1]);
    }

    @Test
    void constructorsRejectMissingGenericSuperclassAndNonParameterizedSubstitutionTarget() {
        assertThrows(ClassCastException.class, RawReference::new);
        assertThrows(ClassCastException.class, () -> new TypeReference<String>(Integer.class) { });
    }

    @Test
    void nullReplacementArrayFailsOnlyWhenAVariableNeedsItsLength() {
        assertThrows(NullPointerException.class, () -> new ListReference<>((Type[]) null));
        ParameterizedType concrete = parameterized(
                new TypeReference<List<String>>((Type[]) null) { }.getType(), List.class);

        assertArrayEquals(new Type[] {String.class}, concrete.getActualTypeArguments());
    }

    private static ParameterizedType parameterized(Type type, Type rawType) {
        ParameterizedType result = assertInstanceOf(ParameterizedType.class, type);
        assertEquals(rawType, result.getRawType());
        return result;
    }

    private static class ListReference<T> extends TypeReference<List<T>> {
        ListReference(Type... types) { super(types); }
    }

    private static class StringListReference extends TypeReference<List<String>> {
    }

    private static class MapReference<K, V> extends TypeReference<Map<K, V>> {
        MapReference(Type... types) { super(types); }
    }

    private static class FixedKeyReference<T> extends TypeReference<Map<String, T>> {
        FixedKeyReference(Type... types) { super(types); }
    }

    private static class ArrayReference<T> extends TypeReference<List<T[]>> {
        ArrayReference(Type... types) { super(types); }
    }

    private static class NestedMapReference<K, V> extends TypeReference<List<Map<K, V>>> {
        NestedMapReference(Type... types) { super(types); }
    }

    private static class VariableThenNestedReference<K, V> extends TypeReference<Map<K, List<V>>> {
        VariableThenNestedReference(Type... types) { super(types); }
    }

    private static class NestedThenVariableReference<K, V> extends TypeReference<Map<List<K>, V>> {
        NestedThenVariableReference(Type... types) { super(types); }
    }

    private static class NestedArrayReference<T> extends TypeReference<List<Map<String, T[]>>> {
        NestedArrayReference(Type... types) { super(types); }
    }

    private static class DeepReference<T> extends TypeReference<List<List<List<T>>>> {
        DeepReference(Type... types) { super(types); }
    }

    private static class DeepMapReference<K, V> extends TypeReference<List<Map<K, List<V>>>> {
        DeepMapReference(Type... types) { super(types); }
    }

    @SuppressWarnings("rawtypes")
    private static class RawReference extends TypeReference {
    }
}
