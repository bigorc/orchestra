package org.oc.orchestra.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.oc.orchestra.ResourceFactory;
import org.oc.orchestra.resource.Resource;

public class RoTarget extends Target {

	public RoTarget(HttpCommandBuilder builder) {
		this.builder = builder;
	}

	@Override
	public void execute(String method, CommandLine cmd) {
		String[] args = cmd.getArgs();
		if(args.length < 3) ArgsHelper.usage();
		
		String upload_path = cmd.hasOption("path") ? cmd.getOptionValue("path") : null;
		String path = null;
		Collection<File> files;
		if(cmd.hasOption('r')) {
			path = args[2];
			File fpath = new File(path);
			if(!fpath.isDirectory()) {
				System.out.println("Argument is not a directory when recursive is set.");
				System.exit(1);
			}
			files = FileUtils.listFiles(fpath, null, true);
			path = fpath.getAbsolutePath();
		} else {
			files = new ArrayList<File>();
			for(int i=2;i < args.length;i++) {
				files.add(new File(args[i]));
			}
		}
		
		if(method.equals("run")) {
			for(int i=2;i < args.length;i++) {
				String filename = args[i];
				List<Resource> resources = ResourceFactory.makeResources(filename);
				for(Resource resource : resources) {
					resource.realize();
				}
			}
			
		} else if(method.equals("upload")) {
			builder.setTarget("ro").setMethod("post");
			MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
			List<InputStream> isList = new ArrayList<InputStream>();
			for(File file : files) {
				String relativeDir = "";
				if(cmd.hasOption('r')) {
					builder.setParameter("recursive", "true");
					Path thisPath = Paths.get(path).getParent();
					Path otherPath = Paths.get(file.getAbsolutePath()).getParent();
					Path relativePath = thisPath.relativize(otherPath);
					relativeDir = relativePath.toString() + "/";
				}
				entityBuilder.addBinaryBody("field1", file, 
						ContentType.APPLICATION_OCTET_STREAM, 
						relativeDir + file.getName());
				try {
					isList.add(new FileInputStream(file));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
						
			HttpEntity multipart = entityBuilder.build();
			builder.setEntity(multipart);

			SequenceInputStream fis = new SequenceInputStream(Collections.enumeration(isList));
            // Calculates the MD5 digest of the upload file.
            // It will generate a 32 characters hex string.
            try {
				String digest = DigestUtils.md5Hex(fis);
				builder.addHeader("Content-Md5", digest);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(upload_path != null && upload_path.length() != 0) 
				builder.setParameter("upload_path", upload_path);
			output(builder.build().execute());
		} else if(method.equals("download")) {
			String filename = args[2];
			builder.setTarget("ro").setMethod("get").setParameter("filename", filename);
//			for(int i=2;i < args.length;i++) {
//				String filename = args[i];
//				builder.addParameter("filename", filename);
//			}
			
			HttpResponse response = builder.build().execute();
			if(response.getStatusLine().getStatusCode() == 200) {
				try {
					InputStream is = response.getEntity().getContent();
					FileOutputStream os = new FileOutputStream(FilenameUtils.getName(filename));
					byte[] buffer = new byte[1024];
		            int bytesRead;
		            while((bytesRead = is.read(buffer)) !=-1){
		                os.write(buffer, 0, bytesRead);
		            }
		            is.close();
		            os.flush();
		            os.close();
				} catch (IllegalStateException | IOException e) {
					e.printStackTrace();
				}
			} else {
				output(response);
			}
		} else if(method.equals("delete")) {
			builder.setTarget("ro").setMethod("delete");
			for(int i=2;i < args.length;i++) {
				String filename = args[i];
				builder.addParameter("filename", filename);
				if(cmd.hasOption('r')) {
					builder.setParameter("recursive", "true");
				}
			}
			output(builder.build().execute());
		}
	}

}
