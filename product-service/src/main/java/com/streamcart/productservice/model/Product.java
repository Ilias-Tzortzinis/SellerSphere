package com.streamcart.productservice.model;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public sealed interface Product permits Laptop, Ram {

    Category category();

    enum Category {
        LAPTOP,
        RAM;

        public static Optional<Category> of(String name) {
            return Optional.ofNullable(LOOKUP_TABLE.get(name));
        }

        private static final Map<String, Category> LOOKUP_TABLE = lookupTable(Category.class, c -> c.name().toLowerCase(Locale.US));


    }

    static <E extends Enum<E>> Map<String, E> lookupTable(Class<E> enumClass, Function<E, String> keyFunction){
        var enumConstants = enumClass.getEnumConstants();
        @SuppressWarnings("unchecked")
        Map.Entry<String, E>[] entries = new Map.Entry[enumConstants.length];
        for (int i = 0; i < enumConstants.length; i++) {
            var enumConstant = enumConstants[i];
            entries[i] = Map.entry(keyFunction.apply(enumConstant), enumConstant);
        }
        return Map.ofEntries(entries);
    }

}
