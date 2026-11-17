package ie.newsagent.dao.sqlite;

import ie.newsagent.dao.DeliveryDao;
import ie.newsagent.database.DatabaseException;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.model.Delivery;
import ie.newsagent.model.DeliveryStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Persists deliveries and reads the customer/publication details for reporting. */
public final class SqliteDeliveryDao implements DeliveryDao {
    private final SqliteDatabase database;
    public SqliteDeliveryDao(SqliteDatabase database) { this.database=Objects.requireNonNull(database); }

    @Override public Delivery save(Delivery delivery) {
        Objects.requireNonNull(delivery,"delivery");
        if(delivery.getDeliveryReference()==null||delivery.getDeliveryReference().isBlank()||delivery.getCustomerId()==null||delivery.getPublicationId()==null||delivery.getStatus()==null)
            throw new IllegalArgumentException("Delivery reference, customer, publication and status are required");
        try(var c=database.openConnection()) {
            c.setAutoCommit(false);
            int area;
            try(var s=c.prepareStatement("SELECT delivery_area FROM customer WHERE customer_id=?")) { s.setString(1,delivery.getCustomerId()); try(var r=s.executeQuery()){ if(!r.next())throw new IllegalArgumentException("Customer not found: "+delivery.getCustomerId()); area=r.getInt(1); } }
            String subscription;
            try(var s=c.prepareStatement("SELECT subscription_id FROM subscription WHERE customer_id=? AND publication_id=? AND paused=0 ORDER BY subscription_id LIMIT 1")) {
                s.setString(1,delivery.getCustomerId()); s.setString(2,delivery.getPublicationId()); try(var r=s.executeQuery()){ if(!r.next())throw new IllegalArgumentException("No active customer subscription for publication"); subscription=r.getString(1); }
            }
            LocalDate date=delivery.getDeliveryDate()==null?LocalDate.now():delivery.getDeliveryDate();
            String docket=delivery.getDeliveryReference()+"-D";
            try(var s=c.prepareStatement("INSERT INTO delivery_docket(docket_reference,delivery_date,delivery_area) VALUES(?,?,?)")){s.setString(1,docket);s.setString(2,date.toString());s.setInt(3,area);s.executeUpdate();}
            try(var s=c.prepareStatement("INSERT INTO delivery(delivery_reference,docket_reference,subscription_id,status) VALUES(?,?,?,?)")){s.setString(1,delivery.getDeliveryReference());s.setString(2,docket);s.setString(3,subscription);s.setString(4,delivery.getStatus().name());s.executeUpdate();}
            if(delivery.getStatus()==DeliveryStatus.OUT_OF_STOCK) try(var s=c.prepareStatement("INSERT INTO out_of_stock_record(delivery_reference,publication_id,recorded_at,note) VALUES(?,?,?,?)")){s.setString(1,delivery.getDeliveryReference());s.setString(2,delivery.getPublicationId());s.setString(3,date.toString());s.setString(4,"Out of stock");s.executeUpdate();}
            // A newly recorded next delivery docket carries the customer's outstanding bill.
            try(var s=c.prepareStatement("UPDATE bill SET docket_reference=? WHERE customer_id=? AND docket_reference IS NULL AND billing_month <= ?")){s.setString(1,docket);s.setString(2,delivery.getCustomerId());s.setString(3,YearMonth.from(date).toString());s.executeUpdate();}
            c.commit(); delivery.setSummaryDetails(null,null,date); return delivery;
        } catch(SQLException ex){throw new DatabaseException("Could not save delivery "+delivery.getDeliveryReference(),ex);}
    }
    @Override public Optional<Delivery> findByReference(String reference) {
        return query("WHERE d.delivery_reference=?",reference,null).stream().findFirst();
    }
    @Override public List<Delivery> findAll(){return query("",null,null);}
    @Override public List<Delivery> findDeliveredForCustomer(String customerId, YearMonth month) {
        return query("WHERE s.customer_id=? AND d.status='DELIVERED' AND substr(k.delivery_date,1,7)=?",customerId,month.toString());
    }
    private List<Delivery> query(String where,String first,String second) {
        String sql="SELECT d.delivery_reference,s.customer_id,p.publication_id,d.status,c.name customer_name,p.name publication_name,k.delivery_date FROM delivery d JOIN subscription s ON s.subscription_id=d.subscription_id JOIN customer c ON c.customer_id=s.customer_id JOIN publication p ON p.publication_id=s.publication_id JOIN delivery_docket k ON k.docket_reference=d.docket_reference "+where+" ORDER BY k.delivery_date,d.delivery_reference";
        try(var c=database.openConnection();var s=c.prepareStatement(sql)) {
            if(first!=null)s.setString(1,first); if(second!=null)s.setString(2,second);
            try(var r=s.executeQuery()){List<Delivery> out=new ArrayList<>();while(r.next())out.add(map(r));return List.copyOf(out);}
        }catch(SQLException ex){throw new DatabaseException("Could not retrieve delivery history",ex);}
    }
    private Delivery map(ResultSet r)throws SQLException {
        Delivery d=new Delivery(r.getString("delivery_reference"),r.getString("customer_id"),r.getString("publication_id"));
        d.setStatus(DeliveryStatus.valueOf(r.getString("status")));
        d.setSummaryDetails(r.getString("customer_name"),r.getString("publication_name"),LocalDate.parse(r.getString("delivery_date"))); return d;
    }
}
