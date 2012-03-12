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
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.tapestry5.internal.services.ResourceStreamerImpl;
import org.apache.tapestry5.internal.services.assets.ResourceChangeTracker;
import org.apache.tapestry5.ioc.OperationTracker;
import org.apache.tapestry5.ioc.Resource;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.services.ResponseCompressionAnalyzer;
import org.apache.tapestry5.services.assets.CompressionStatus;
import org.apache.tapestry5.services.assets.StreamableResource;
import org.apache.tapestry5.services.assets.StreamableResourceSource;

public class StaticAssetResourceStreamer extends ResourceStreamerImpl {
    
    private final Request request;
    
    public StaticAssetResourceStreamer(Request request, Response response,
            StreamableResourceSource streamableResourceSource,
            ResponseCompressionAnalyzer analyzer, OperationTracker tracker,
            boolean productionMode, ResourceChangeTracker resourceChangeTracker) {
        super(request, response, streamableResourceSource, analyzer, tracker,
                productionMode, resourceChangeTracker);
        
        this.request = request;
    }

    public static final String ASSETS_STATIC_PROPERTIES = "assets/static.properties";
    
    @Override
    public void streamResource(StreamableResource resource) throws IOException {
        String header = request.getHeader(StaticAssetPathConverter.ASSETS_PRECOMPILATION);
        if (header != null) {
            //  Only create static assets during precompilation
            String path = request.getPath();
            if (path.startsWith("/assets")) {
                try {
                    createStaticAsset(path, (resource.getCompression() == CompressionStatus.COMPRESSED)
                            ? uncompress(resource.openStream()) : resource.openStream());
                } catch (Exception e) {
                    throw new RuntimeException("Try running application with system property -D--enable_all_permissions=true", e);
                }
            }
        }
        super.streamResource(resource);
    }

    private InputStream uncompress(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        GZIPInputStream gzip = new GZIPInputStream(input);
        IOUtils.copy(gzip, output);
        gzip.close();
        return new ByteArrayInputStream(output.toByteArray());
    }

    @Override
    public void streamResource(Resource resource) throws IOException {
        super.streamResource(resource);
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