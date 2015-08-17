package org.oc.orchestra.rest;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;









import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
//import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class PRO extends ServerResource {
	private static String pro_path = "/pro/";
	
	@Get
	public Representation getPro() throws URISyntaxException {
		System.out.println("Pro resource was invoked");
		String filename = getQuery().getValues("filename");
		System.out.println("filename is " + filename);
		
		File file = new File(getClass().getResource(pro_path + filename).toURI());
		if(!file.exists()) {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			return null;
		}
		getResponse().setStatus(Status.SUCCESS_OK);
		FileRepresentation fr = new FileRepresentation(file, MediaType.TEXT_PLAIN);
//		fr.getDisposition().setType(Disposition.TYPE_ATTACHMENT);
//		fr.getDisposition().setFilename(filename);
		return fr;
	}
	
	@Post
	public Representation createPro(Representation input) throws Exception {
		RestletFileUpload fileUpload = new RestletFileUpload(
				new DiskFileItemFactory());
		List<FileItem> fileItems = fileUpload.parseRepresentation(input);
		for(FileItem fi : fileItems) {
			String filename = fi.getName();
			File tmp = new File(filename);//get the filename, in case it's a path
			filename = tmp.getName();
			String path = tmp.getPath();
			System.out.println(path + ":" + filename);
			String pro_base_path = "resources/pro/";
			File file = new File(pro_base_path + filename);
			file.createNewFile();
			fi.write(file);
		}
		getResponse().setStatus(Status.SUCCESS_OK);
		return new StringRepresentation("File uploaded.");
	}
}
