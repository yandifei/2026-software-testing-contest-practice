package net.mooctest.port;

/** Generates stable request identifiers. */
@FunctionalInterface
public interface RequestIdGenerator {

    String nextId();
}
