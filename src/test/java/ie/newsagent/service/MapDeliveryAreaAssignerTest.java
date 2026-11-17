package ie.newsagent.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapDeliveryAreaAssignerTest {
    @Test
    void returnsAreaFromSuppliedAddressMapping() {
        MapDeliveryAreaAssigner assigner = new MapDeliveryAreaAssigner(Map.of("10 Main Street", 7));

        assertEquals(7, assigner.assignArea("10 Main Street"));
        assertEquals(7, assigner.assignArea(" 10 Main Street "));
    }

    @Test
    void rejectsBlankAndUnmappedAddresses() {
        MapDeliveryAreaAssigner assigner = new MapDeliveryAreaAssigner(Map.of("10 Main Street", 7));

        assertThrows(IllegalArgumentException.class, () -> assigner.assignArea("  "));
        assertThrows(UnmappedAddressException.class, () -> assigner.assignArea("Unknown Road"));
    }

    @Test
    void rejectsAreasOutsideTheConfiguredRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new MapDeliveryAreaAssigner(Map.of("Some Road", 25)));
    }
}
