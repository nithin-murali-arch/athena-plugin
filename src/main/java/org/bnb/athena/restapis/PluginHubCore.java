package org.bnb.athena.restapis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.bnb.athena.pojos.SearchData;
import org.bnb.pluginhub.dao.PluginHubCoreDAO;
import org.bnb.pluginhub.pojos.User;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.JSONException;

import com.sun.jersey.core.header.FormDataContentDisposition;

@Path("/pluginhub")
public class PluginHubCore {
	PluginHubCoreDAO dao;

	public PluginHubCore() {
		dao = PluginHubCoreDAO.getInstance();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/searchAll/{search}")
	public List<SearchData> searchPluginHub(@PathParam("search") String search) throws Exception {
		return dao.search(search, search, search, search);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/fetch/{id}")
	public SearchData fetch(@PathParam("id") int id) throws Exception {
		return dao.fetch(id);
	}

	@POST
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/add")
	public String insert(SearchData data, @Context HttpServletRequest request) throws Exception {
		HttpSession session = request.getSession();
		String createdBy = (String) session.getAttribute("username");
		if (createdBy != null && !createdBy.equals(data.getCreatedBy())) {
			return "Don't try to mess with us. We have a smart architect ;)";
		}
		dao.insert(0, data.getPluginName(), data.getPluginDescription(), data.getFileName(), createdBy);
		return "Success!";
	}

	@POST
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/update")
	public String update(SearchData data, @Context HttpServletRequest request) throws Exception {
		HttpSession session = request.getSession();
		String createdBy = (String) session.getAttribute("username");
		if (createdBy != null && !createdBy.equals(data.getCreatedBy())) {
			return "Don't try to mess with us. We have a smart architect ;)";
		}
		dao.update(data.getId(), data.getPluginDescription(), data.getFileName(), data.getVersion());
		dao.insert(0, data.getPluginName(), data.getPluginDescription(), data.getFileName(), createdBy);
		return "Success!";
	}

	@POST
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Path("/upload")
	public String upload(@FormParam("file") InputStream uploadedInputStream,
			@FormParam("file") FormDataContentDisposition fileDetail, @Context HttpServletRequest request) {
		HttpSession session = request.getSession();
		String createdBy = (String) session.getAttribute("username");
		if (createdBy != null) {
			String fileLocation = System.getProperty("user.home") + "/" + createdBy + "/" + fileDetail.getFileName();
			try {
				FileOutputStream out = new FileOutputStream(new File(fileLocation));
				int read = 0;
				byte[] bytes = new byte[1024];
				out = new FileOutputStream(new File(fileLocation));
				while ((read = uploadedInputStream.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			return "Please login before you do that!";
		}
		return "Success!";
	}
	
	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Path("/download/{id}")
	public void getFile(@PathParam("id") int id, @Context HttpServletResponse response) throws Exception{
		SearchData data = dao.fetch(id);
		String fileLocation = System.getProperty("user.home") + "/" + data.getCreatedBy() + "/" + data.getFileName();
		InputStream is = new FileInputStream(fileLocation);
	      IOUtils.copy(is, response.getOutputStream());
	      response.setContentType("application/octet-stream");
	      response.flushBuffer();
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/login")
	public User login(User user){
		//TODO
		return null;
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/register")
	public User register(User user){
		//TODO
		return null;
	}
	
}
