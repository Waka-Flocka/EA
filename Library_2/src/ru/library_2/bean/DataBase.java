package ru.library_2.bean;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import ru.library_2.entities.*;

@ManagedBean(name = "dbBean")
@ApplicationScoped
public class DataBase {

	private static volatile Connection connectionOracle = null;
	private static volatile Connection connectionMySQl = null;

	static {
		/*
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			connectionOracle = DriverManager.getConnection(
					"jdbc:oracle:thin:@192.168.100.49:1521:STUDENT", "IT15_12",
					"123456");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
*/
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connectionMySQl = DriverManager.getConnection(
					"jdbc:mysql://db4free.net:3306/entapp", "waka", "123qwe");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public int checkUser(String login, String password) throws Exception {
		String sql = "SELECT id FROM reader WHERE login=? AND password=?";
		PreparedStatement pstatement = connectionMySQl.prepareStatement(sql);
		pstatement.setString(1, login);
		pstatement.setString(2, password);
		ResultSet result = pstatement.executeQuery();
		if (result.next())
			return result.getInt("id");
		else
			return -1;
	}

	public User getUserInfo(String fio) throws Exception {
		String tmp[] = fio.split(" ");
		String sql = "SELECT kod_sotr, name_dolgn, name_otdel, TO_CHAR(date_rab,'DD-MON-YYYY') AS on_rab, salary, uvolen "
				+ "FROM SOTR, OTDEL, DOLGN "
				+ "WHERE SOTR.kod_dolgn = DOLGN.kod_dolgn AND "
				+ "SOTR.kod_otdel = OTDEL.kod_otdel AND "
				+ "UPPER(SOTR.fam) = ? AND UPPER(SOTR.name) = ? AND UPPER(SOTR.otch) = ?";
		PreparedStatement pstatement = connectionOracle.prepareStatement(sql);
		pstatement.setString(1, tmp[0]);
		pstatement.setString(2, tmp[1]);
		pstatement.setString(3, tmp[2]);
		ResultSet result = pstatement.executeQuery();
		result.next();

		int id = result.getInt("kod_sotr");
		String date_rab = result.getString("on_rab");
		String dolgn = result.getString("name_dolgn");
		String otdel = result.getString("name_otdel");
		String uvolen = result.getString("uvolen").equalsIgnoreCase("�") ? "��������"
				: "������";
		double salary = result.getDouble("salary");

		User user = new User(id, fio, date_rab, dolgn, otdel, uvolen, salary);
		return user;
	}

	public ArrayList<BookOnHand> getBooksOnHand() throws Exception {
		String sql = "SELECT bookonhands.id as id, idBook, title, fio, date_in, penalty "
				+ "FROM bookonhands, reader, book "
				+ "WHERE bookonhands.idreader = reader.id AND "
				+ "bookonhands.idbook = book.id AND " + "status = 0";
		PreparedStatement pstatment = connectionMySQl.prepareStatement(sql);
		ResultSet result = pstatment.executeQuery();
		ArrayList<BookOnHand> list = new ArrayList<BookOnHand>();
		while (result.next()) {
			int idRec = result.getInt("id");
			int idBook = result.getInt("idBook");
			String title = result.getString("title");
			String fio = result.getString("fio");
			String dIn = result.getString("date_in");
			double penalty = result.getDouble("penalty");
			list.add(new BookOnHand(idRec, idBook, title, fio, dIn, penalty));
		}

		return list;
	}

	public ArrayList<BookOnHand> getBooksOnHand(int id) throws Exception {
		String sql = "SELECT bookonhands.id as id, idBook, title, fio, date_in, penalty "
				+ "FROM bookonhands, reader, book "
				+ "WHERE bookonhands.idreader = reader.id AND "
				+ "bookonhands.idbook = book.id AND "
				+ "status = 0 AND idreader = ?";
		PreparedStatement pstatment = connectionMySQl.prepareStatement(sql);
		pstatment.setInt(1, id);
		ResultSet result = pstatment.executeQuery();
		ArrayList<BookOnHand> list = new ArrayList<BookOnHand>();

		while (result.next()) {
			int idRec = result.getInt("id");
			int idBook = result.getInt("idBook");
			String title = result.getString("title");
			String fio = result.getString("fio");
			String dIn = result.getString("date_in");
			double penalty = result.getDouble("penalty");
			list.add(new BookOnHand(idRec, idBook, title, fio, dIn, penalty));
		}

		return list;
	}

	public void takeBook(String fio, String title, String dOut, String dIn)
			throws Exception {
		int idReader = getIdReaderByFio(fio);
		if (idReader == -1)
			return;
		int idBook = getIdBookByTitle(title);
		if (idBook == -1)
			return;
		if (!bookINnStorage(idBook))
			return;
		if (checkRareBook(idBook)) {
			if (LibrarianBean.getDifferenceDateInDay(dOut,dIn) > 7)
				return;
		}
		String sql = "UPDATE book SET amount = amount - 1 WHERE id = ?";
		PreparedStatement pstatment = connectionMySQl.prepareStatement(sql);
		pstatment.setInt(1, idBook);
		pstatment.executeUpdate();

		sql = "INSERT INTO bookonhands "
				+ "(idbook, idreader, date_out, date_in) "
				+ "VALUES (?, ?, ?, ?)";
		pstatment = connectionMySQl.prepareStatement(sql);
		pstatment.setInt(1, idBook);
		pstatment.setInt(2, idReader);
		pstatment.setString(3, dOut);
		pstatment.setString(4, dIn);
		pstatment.executeUpdate();
	}

	public int getIdReaderByFio(String fio) throws Exception {
		fio = fio.toUpperCase();
		String sql = "SELECT id FROM reader WHERE UPPER(fio) = ?";
		PreparedStatement pstatment = connectionMySQl.prepareStatement(sql);
		pstatment.setString(1, fio);
		ResultSet result = pstatment.executeQuery();

		if (result.next()){
			return result.getInt("id");
		}
		return -1;
	}

	public int getIdBookByTitle(String title) throws Exception {
		String sql = "SELECT id FROM book WHERE title = ?";
		PreparedStatement pstatment = connectionMySQl.prepareStatement(sql);
		pstatment.setString(1, title);
		ResultSet result = pstatment.executeQuery();
		if (result.next())
			return result.getInt("id");
		return -1;
	}

	public String getFioById(int id) throws Exception {
		String sql = "SELECT fio FROM reader WHERE id = ?";
		PreparedStatement pstatment = connectionMySQl.prepareStatement(sql);
		pstatment.setInt(1, id);
		ResultSet result = pstatment.executeQuery();
		result.next();

		return result.getString("fio").toUpperCase();
	}

	public void returnBook(int id) throws Exception {
		String sql = "UPDATE bookonhands SET status = 1 "
				+ "WHERE id = ?";
		PreparedStatement pstatment = connectionMySQl.prepareStatement(sql);
		pstatment.setInt(1, id);
		pstatment.executeUpdate();
	}
	
	public void addAmountBook(int idBook) throws Exception{
		String sql = "UPDATE book SET amount = amount + 1 WHERE id = " + idBook;
		PreparedStatement pstatment = connectionMySQl.prepareStatement(sql);
		pstatment.executeUpdate(sql);
	}

	public boolean bookINnStorage(int id) throws Exception {
		String sql = "SELECT amount FROM book WHERE amount > 0 AND id = ?";
		PreparedStatement pstatment = connectionMySQl.prepareStatement(sql);
		pstatment.setInt(1, id);
		ResultSet result = pstatment.executeQuery();
		if (result.next())
			return true;
		return false;
	}

	public String getDateReturn(int id) throws Exception {
		String sql = "SELECT date_in FROM bookonhands WHERE idbook = ? ORDER BY date_in ASC LIMIT 1";
		PreparedStatement pstatment = connectionMySQl.prepareStatement(sql);
		pstatment.setInt(1, id);
		ResultSet result = pstatment.executeQuery();
		result.next();
		return result.getString("date_in");
	}
	
	public boolean checkRareBook(int idBook) throws Exception {
		String sql = "SELECT value FROM book WHERE id = ?";
		PreparedStatement pstatment = connectionMySQl.prepareStatement(sql);
		pstatment.setInt(1, idBook);
		ResultSet result = pstatment.executeQuery();
		result.next();
		int value = result.getInt("value");
		if (value == 1)
			return true;
		return false;
	}
}
