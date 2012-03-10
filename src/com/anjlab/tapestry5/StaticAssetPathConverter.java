package com.anjlab.tapestry5;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.tapestry5.services.AssetPathConverter;
import org.apache.tapestry5.services.RequestGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class StaticAssetPathConverter implements AssetPathConverter {
    
    private final Logger logger = LoggerFactory.getLogger(StaticAssetPathConverter.class);
    
    private final RequestGlobals requestGlobals;
    private final Properties staticProperties;

    public static final String ASSETS_PRECOMPILATION = "Assets-Precompilation";

    public StaticAssetPathConverter(RequestGlobals requestGlobals) {
        this.requestGlobals = requestGlobals;
        this.staticProperties = new Properties();
        
        String staticPropertiesPath = getRealPath(StaticAssetResourceStreamer.ASSETS_STATIC_PROPERTIES);
        try {
            staticProperties.load(new FileInputStream(staticPropertiesPath));
        } catch (IOException e) {
            logger.error("Failed to load properties from {}: {}", staticPropertiesPath, e);
        }
    }

    @Override
    public boolean isInvariant() {
        return true;
    }

    @Override
    public String convertAssetPath(String assetPath) {
        String header = requestGlobals.getRequest().getHeader(StaticAssetPathConverter.ASSETS_PRECOMPILATION);
        if (header != null) {
            //  Don't use static assets during assets precompilation
            return assetPath;
        }
        
        String assetHash = staticProperties.getProperty(assetPath);
        return assetHash == null ? assetPath : assetPath + "?" + assetHash;
    }

    public String getRealPath(String path) {
        String realPath = requestGlobals.getHTTPServletRequest()
                .getSession().getServletContext().getRealPath(path);
        return realPath;
    }
}