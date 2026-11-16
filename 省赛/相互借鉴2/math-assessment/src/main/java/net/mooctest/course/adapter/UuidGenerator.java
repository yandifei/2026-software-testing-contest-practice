package net.mooctest.course.adapter;

import net.mooctest.course.port.IdGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidGenerator implements IdGenerator {

    @Override
    public String nextId() {
        return UUID.randomUUID().toString();
    }
}
