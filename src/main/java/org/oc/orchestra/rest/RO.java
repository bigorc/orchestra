package org.oc.orchestra.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;













import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.FileUtils;
//import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

public class RO extends ServerResource {
	private static String ro_path = "ro/";
	private String tmp_path = "/tmp";
	
	@Get
	public Representation getRo() throws URISyntaxException {
		System.out.println("Pro resource was invoked");
		String filename = getQuery().getValues("filename");
		System.out.println("filename is " + filename);
		
		File file = new File(ro_path + filename);
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
	@Put
	public Representation createRo(Representation input) throws Exception {
		RestletFileUpload fileUpload = new RestletFileUpload(
				new DiskFileItemFactory());
		List<FileItem> fileItems = fileUpload.parseRepresentation(input);
		
		Series<Header> headers = (Series<Header>)getRequest().getAttributes().get("org.restlet.http.headers");
		String md5 = headers.getFirstValue("Content-MD5", true);
		List<InputStream> isList = new ArrayList<InputStream>();
		for(FileItem fi : fileItems) {
			isList.add(fi.getInputStream());
		}
		SequenceInputStream fis = new SequenceInputStream(Collections.enumeration(isList));
		Object digest = null;
		try {
			digest = DigestUtils.md5Hex(fis);
		}catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("header md5:" + md5 + ";calculated md5:" + digest);
		if(md5 == null || !md5.equals(digest)) {
			getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY);
			return new StringRepresentation("File upload failed.");
		}
		boolean recursive = "true".equalsIgnoreCase(getQuery().getFirstValue("recursive")) ? true : false;		
		for(FileItem fi : fileItems) {
			String filename = fi.getName();
			System.out.println(filename);
			File tmp = new File(filename);//get the filename, in case it's a path
			if(!recursive) filename = tmp.getName();
			
			String upload_path = getQuery().getValues("upload_path");

			File file;
			if(upload_path == null) {
				file = new File(ro_path + filename);
			} else {
				file = new File(ro_path + upload_path);
				file = new File(ro_path + upload_path + "/" + filename);
			}
			file.getParentFile().mkdirs();
			file.createNewFile();
			fi.write(file);
		}
		getResponse().setStatus(Status.SUCCESS_OK);
		return new StringRepresentation("File uploaded.");
	}
	
	@Delete
	public Representation deleteRo(Representation input) {
		boolean recursive = "true".equals(getQuery().getFirstValue("recursive")) ? true : false;
		String[] filenames = getQuery().getValuesArray("filename");
		for(String filename : filenames) {
			System.out.println("filename is " + filename);

			File file = new File(ro_path + filename);
			if(file.exists()) {
				if(recursive) {
					try {
						FileUtils.deleteDirectory(file);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					file.delete();
				}
			}
		}
		getResponse().setStatus(Status.SUCCESS_OK);
		return new StringRepresentation("Ro deleted.");
	}
}
