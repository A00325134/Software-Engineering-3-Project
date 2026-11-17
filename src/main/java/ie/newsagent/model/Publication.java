package ie.newsagent.model;

public class Publication {
    private final String publicationId;
    private String name;
    private int stockQuantity;
    private int lowStockThreshold = 5;

    public Publication(String publicationId, String name, int stockQuantity) {
        this.publicationId = publicationId; this.name = name; this.stockQuantity = stockQuantity;
    }
    public String getPublicationId() { return publicationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public boolean isLowStock() { return stockQuantity < lowStockThreshold; }
}
