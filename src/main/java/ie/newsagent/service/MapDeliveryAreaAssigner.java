package ie.newsagent.service;

import java.util.Map;
import java.util.Objects;

/** Exact-address lookup strategy; mapping data must be supplied by the project. */
public final class MapDeliveryAreaAssigner implements DeliveryAreaAssigner {
    private final Map<String, Integer> areaByAddress;

    public MapDeliveryAreaAssigner(Map<String, Integer> areaByAddress) {
        this.areaByAddress = Map.copyOf(Objects.requireNonNull(areaByAddress, "areaByAddress"));
        if (this.areaByAddress.values().stream().anyMatch(area -> area < 1 || area > 24)) {
            throw new IllegalArgumentException("Mapped delivery areas must be between 1 and 24");
        }
    }

    @Override
    public int assignArea(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address is required for delivery-area assignment");
        }
        Integer area = areaByAddress.get(address.trim());
        if (area == null) throw new UnmappedAddressException(address);
        return area;
    }
}
