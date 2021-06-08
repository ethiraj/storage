package org.opengroup.osdu.storage.provider.mongodb.util;

import org.opengroup.osdu.core.common.model.storage.Schema;
import org.opengroup.osdu.core.common.model.storage.SchemaItem;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class SchemaGenerator {

    public static Schema one(String kind) {
        SchemaItem[] items = new SchemaItem[2];
        items[0] = new SchemaItem();
        items[0].setKind("string");
        items[0].setPath("user.name");
        items[1] = new SchemaItem();
        items[1].setKind("string");
        items[1].setPath("user.mail");
        return Schema.builder()
                .schema(items)
                .kind(kind)
                .ext(new HashMap<>())
                .build();
    }

    public static List<Schema> withIds(String... kinds) {
        return Arrays.stream(kinds)
                .map(SchemaGenerator::one)
                .collect(Collectors.toList());
    }
}
