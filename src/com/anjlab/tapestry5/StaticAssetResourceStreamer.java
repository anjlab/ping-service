package com.anjlab.tapestry5;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.tapestry5.internal.services.ResourceStreamer;
import org.apache.tapestry5.ioc.Resource;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.assets.StreamableResource;

public class StaticAssetResourceStreamer implements ResourceStreamer {
    
    public static final String ASSETS_STATIC_PROPERTIES = "assets/static.properties";
    
    private final RequestGlobals requestGlobals;
    private final ResourceStreamer streamer;

    public StaticAssetResourceStreamer(RequestGlobals requestGlobals, ResourceStreamer streamer) {
        this.requestGlobals = requestGlobals;
        this.streamer = streamer;
    }

    @Override
    public void streamResource(StreamableResource resource) throws IOException {
        streamer.streamResource(resource);
    }

    @Override
    public void streamResource(Resource resource) throws IOException {
        String path = requestGlobals.getRequest().getPath();
        if (path.startsWith("/assets")) {
            try {
                createStaticAsset(path, resource.openStream());
            } catch (Exception e) {
                throw new RuntimeException("Try running application with system property -D--enable_all_permissions=true", e);
            }
        }
        streamer.streamResource(resource);
    }

    private synchronized void createStaticAsset(String assetPathVersioned, InputStream input)
            throws IOException, ClientProtocolException, NoSuchAlgorithmException, FileNotFoundException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(input, baos);
        byte[] responseBytes = baos.toByteArray();
        
        File staticPropertiesFile = new File(ASSETS_STATIC_PROPERTIES);

        Properties properties = loadStaticProperties(staticPropertiesFile);
        
        copyFile(responseBytes, assetPathVersioned.substring(1));
        
        String fileHash = calculateDigest(inputStream(responseBytes));
        
        properties.put(assetPathVersioned, fileHash);
        
        FileOutputStream outputStream = new FileOutputStream(staticPropertiesFile);
        properties.store(outputStream, "Static Assets For Tapestry5 Application");
        outputStream.close();
    }

    public Properties loadStaticProperties(File staticPropertiesFile) throws IOException
    {
        Properties properties = new Properties();
        if (staticPropertiesFile.exists()) {
            FileInputStream inputStream = new FileInputStream(staticPropertiesFile);
            properties.load(inputStream);
            inputStream.close();
        }
        return properties;
    }

    private ByteArrayInputStream inputStream(byte[] responseBytes) {
        return new ByteArrayInputStream(responseBytes);
    }

    private void copyFile(byte[] responseBytes, String targetPath) throws IOException
    {
        File targetFile = new File(targetPath);
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        
        if (!targetFile.exists()) {
            FileOutputStream outputStream = new FileOutputStream(targetFile);
            IOUtils.copy(inputStream(responseBytes), outputStream);
            outputStream.close();
        }
    }

    private String calculateDigest(InputStream input) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA1");
        digest.reset();
        
        byte[] buf = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buf)) > 0) {
            digest.update(buf, 0, bytesRead);
        }
        
        return new BigInteger(1, digest.digest()).toString(16);
    }
}