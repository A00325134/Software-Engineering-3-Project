package ie.newsagent.dao.sqlite;

import ie.newsagent.dao.BillDao;
import ie.newsagent.database.DatabaseException;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.model.Bill;
import ie.newsagent.model.BillItem;
import ie.newsagent.model.BillPaymentStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** SQLite persistence for monthly bills and their delivered items. */
public final class SqliteBillDao implements BillDao {
    private final SqliteDatabase database;
    public SqliteBillDao(SqliteDatabase database) { this.database = Objects.requireNonNull(database); }

    @Override public Bill save(Bill bill) {
        Objects.requireNonNull(bill);
        String billSql = "INSERT INTO bill (bill_reference,customer_id,billing_month,monthly_total,payment_status) VALUES (?,?,?,?,?)";
        try (var c = database.openConnection()) {
            c.setAutoCommit(false);
            try (var s = c.prepareStatement(billSql)) {
                s.setString(1,bill.getBillReference()); s.setString(2,bill.getCustomerId());
                s.setString(3,bill.getBillingMonth().toString()); s.setBigDecimal(4,bill.getMonthlyTotal());
                s.setString(5,bill.getPaymentStatus().name()); s.executeUpdate();
            }
            try (var s = c.prepareStatement("INSERT INTO bill_item (bill_reference,item_number,item_name,delivery_date,amount,delivery_reference) VALUES (?,?,?,?,?,?)")) {
                int n=1;
                for (BillItem item:bill.getItems()) {
                    s.setString(1,bill.getBillReference()); s.setInt(2,n++); s.setString(3,item.itemName());
                    s.setString(4,item.deliveryDate().toString()); s.setBigDecimal(5,item.amount()); s.setString(6,item.deliveryReference()); s.addBatch();
                }
                s.executeBatch();
            }
            c.commit(); return bill;
        } catch (SQLException ex) { throw new DatabaseException("Could not save bill " + bill.getBillReference(),ex); }
    }
    @Override public Optional<Bill> findByReference(String reference) {
        String sql="SELECT bill_reference,customer_id,billing_month,monthly_total,payment_status,docket_reference FROM bill WHERE bill_reference=?";
        try (var c=database.openConnection(); var s=c.prepareStatement(sql)) {
            s.setString(1,reference); try(var r=s.executeQuery()) { return r.next()?Optional.of(mapBill(r)):Optional.empty(); }
        } catch(SQLException ex) { throw new DatabaseException("Could not retrieve bill " + reference,ex); }
    }
    @Override public List<Bill> findByCustomerId(String customerId) { return find("SELECT bill_reference,customer_id,billing_month,monthly_total,payment_status,docket_reference FROM bill WHERE customer_id=? ORDER BY billing_month",customerId); }
    @Override public List<Bill> findAll() { return find("SELECT bill_reference,customer_id,billing_month,monthly_total,payment_status,docket_reference FROM bill ORDER BY billing_month,customer_id",null); }
    private List<Bill> find(String sql,String arg) {
        try(var c=database.openConnection(); var s=c.prepareStatement(sql)) {
            if(arg!=null)s.setString(1,arg); try(var r=s.executeQuery()) { List<Bill> out=new ArrayList<>(); while(r.next())out.add(mapBill(r)); return List.copyOf(out); }
        } catch(SQLException ex) { throw new DatabaseException("Could not list bills",ex); }
    }
    @Override public boolean markPaid(String reference) {
        try(var c=database.openConnection();var s=c.prepareStatement("UPDATE bill SET payment_status='PAID' WHERE bill_reference=?")) { s.setString(1,reference); return s.executeUpdate()==1; }
        catch(SQLException ex) { throw new DatabaseException("Could not update bill payment status",ex); }
    }
    public Optional<String> docketForBill(String reference) {
        try(var c=database.openConnection();var s=c.prepareStatement("SELECT docket_reference FROM bill WHERE bill_reference=?")){s.setString(1,reference);try(var r=s.executeQuery()){if(!r.next())return Optional.empty();return Optional.ofNullable(r.getString(1));}}
        catch(SQLException ex){throw new DatabaseException("Could not retrieve bill docket",ex);}
    }
    private Bill mapBill(ResultSet r) throws SQLException {
        String ref=r.getString("bill_reference"); List<BillItem> items=new ArrayList<>();
        try(var c=database.openConnection();var s=c.prepareStatement("SELECT item_name,delivery_date,amount,delivery_reference FROM bill_item WHERE bill_reference=? ORDER BY item_number")) {
            s.setString(1,ref); try(var ir=s.executeQuery()) { while(ir.next())items.add(new BillItem(ir.getString(1),java.time.LocalDate.parse(ir.getString(2)),ir.getBigDecimal(3),ir.getString(4))); }
        }
        Bill b=new Bill(ref,r.getString("customer_id"),java.time.YearMonth.parse(r.getString("billing_month")),items,r.getBigDecimal("monthly_total"));
        b.setPaymentStatus(BillPaymentStatus.valueOf(r.getString("payment_status")));
        b.setDocketReference(r.getString("docket_reference")); return b;
    }
}
