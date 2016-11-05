package org.bnb.pluginhub.dao;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bnb.athena.pojos.SearchData;
import org.bnb.athena.utils.ResultsetJsonConverter;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Backup;
import org.h2.tools.Restore;
import org.json.JSONArray;
import org.json.JSONException;

public class PluginHubCoreDAO {
	Logger logger = Logger.getLogger("PluginHubCoreDAO");
	private static final String jdbcString = "jdbc:h2:~/pluginHub";
	private static final String baseDir = System.getProperty("user.home");
	private static Connection conn;
	ObjectMapper mapper = new ObjectMapper();
	private static PluginHubCoreDAO dao = null;
	private static final String fullSearchQuery = "SELECT * FROM PLUGINS WHERE pluginName LIKE %?% OR pluginDesc LIKE %?% OR pluginFileName LIKE %?% OR createdBy LIKE %?% AND isLatest=1";
	private static final String fetchPlugin = "SELECT * FROM PLUGINS WHERE id = ? AND isLatest=1";
	private static final String insertQuery = "INSERT INTO PLUGINS (pluginName, pluginDescription, fileName, version, downloadCount, isLatest, createdBy, createdDate) VALUES(?,?,?,0,0,1,?,?)";
	private static final String decommission = "UPDATE PLUGINS SET isLatest=0 WHERE id=? AND isLatest=1";
	private static final String updateQuery = "INSERT INTO PLUGINS (pluginName, pluginDescription, fileName, version, downloadCount, isLatest, createdBy, createdDate) VALUES(?,?,?,?,0,1,?,?)";
	private PluginHubCoreDAO(){
		try {
			System.out.println(jdbcString);
			JdbcDataSource ds = new JdbcDataSource();
			ds.setURL(jdbcString);
			conn = ds.getConnection();
			runInitScripts();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			e.printStackTrace();
		}
	}
	
	public static PluginHubCoreDAO getInstance(){
		if(dao == null){
			dao = new PluginHubCoreDAO();
		}
		return dao;
	}
	
	public List<SearchData> search(String name, String desc, String fileName, String createdBy) throws SQLException, JsonParseException, JsonMappingException, JSONException, IOException{
		
		PreparedStatement stmt = conn.prepareStatement(fullSearchQuery);
		stmt.setString(1, name);
		stmt.setString(2, desc);
		stmt.setString(3, fileName);
		stmt.setString(4, createdBy);
		JSONArray array = ResultsetJsonConverter.convert(stmt.executeQuery());
		List<SearchData> list = new ArrayList<SearchData>(array.length());
		SearchData searchdata;
		for(int i = 0; i < array.length(); i++){
			searchdata = mapper.readValue(array.getJSONObject(i).toString(), SearchData.class);
			list.add(searchdata);
		}
		stmt.close();
		return list;
	}
	
	public SearchData fetch(int id) throws SQLException, JsonParseException, JsonMappingException, JSONException, IOException{
		PreparedStatement stmt = conn.prepareStatement(fetchPlugin);
		stmt.setInt(1, id);
		JSONArray array = ResultsetJsonConverter.convert(stmt.executeQuery());
		return mapper.readValue(array.getJSONObject(0).toString(), SearchData.class);
	}
	
	public void insert(int id, String pluginName, String pluginDesc, String fileName, String createdBy) throws SQLException{
		PreparedStatement stmt = conn.prepareStatement(insertQuery);
		stmt.setString(1, pluginName);
		stmt.setString(2, pluginDesc);
		stmt.setString(3, fileName);
		stmt.setString(4, createdBy);
		stmt.execute();
	}
	
	public void update(int id, String description, String fileName, int version) throws SQLException, JsonParseException, JsonMappingException, JSONException, IOException{
		SearchData data = fetch(id);
		PreparedStatement stmt = conn.prepareStatement(decommission);
		stmt.setInt(1, id);
		stmt.execute();
		stmt.close();
		stmt = conn.prepareStatement(insertQuery);
		stmt.setString(1, data.getPluginName());
		stmt.setString(2, description);
		stmt.setString(3, fileName);
		stmt.setInt(4, data.getVersion() + 1);
		stmt.setString(5, data.getCreatedBy());
		stmt.execute();
	}
	
	private void runInitScripts() throws SQLException{
		File backupFile = new File(baseDir + "/backup.zip");
		if(backupFile.exists()){
			Restore.execute(baseDir + "/backup.zip", baseDir, "pluginHub");
		}
		else{
			Statement stmt = conn.createStatement();
			stmt.execute("CREATE TABLE PLUGINS (id BIGINT auto_increment,pluginName VARCHAR2(50), pluginDesc VARCHAR2(500), fileName VARCHAR2(100), version INT, downloadCount BIGINT, isLatest INT, createdBy VARCHAR2(20), createdDate VARCHAR2(50)");
			stmt.execute("CREATE TABLE USERS (USERNAME VARCHAR2(20) PRIMARY KEY, PASSWORD VARCHAR2(20)");
		}
	}
	
	public static void backupData() throws SQLException{
		Backup.execute(baseDir + "/backup.zip", baseDir, "pluginHub", true);
	}
}
