import java.sql.*;

public class PurchaseDao {
    private Connection conn;

    public PurchaseDao(Connection conn) {
        this.conn = conn;
    }
    public boolean createPurchase(Purchase newPurchase) {
        PreparedStatement preparedStatement = null;
        String insertQueryStatement = "INSERT INTO Purchases (storeID, customerID, purchaseDate, items) " +
                "VALUES (?,?,?,?)";

        try {
            preparedStatement = conn.prepareStatement(insertQueryStatement);
            preparedStatement.setInt(1, newPurchase.getStoreID());
            preparedStatement.setInt(2, newPurchase.getCustomerID());
            preparedStatement.setString(3, newPurchase.getDate());
            preparedStatement.setString(4, newPurchase.getPurchaseItems());

            // execute insert SQL statement
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        return true;
    }
}