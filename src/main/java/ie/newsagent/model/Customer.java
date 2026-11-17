package ie.newsagent.model;

public class Customer {
    private final String customerId;
    private String name;
    private String phoneNumber;
    private String address;
    private String eircode;
    private int deliveryArea;

    public Customer(String customerId, String name, String phoneNumber, String address, String eircode, int deliveryArea) {
        this.customerId = customerId;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.eircode = eircode;
        this.deliveryArea = deliveryArea;
    }
    public String getCustomerId() { return customerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEircode() { return eircode; }
    public void setEircode(String eircode) { this.eircode = eircode; }
    public int getDeliveryArea() { return deliveryArea; }
    public void setDeliveryArea(int deliveryArea) { this.deliveryArea = deliveryArea; }
}
