package com.kbsriram.keypan.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Funnel all network traffic through here, so I can swap in
 * implementations for app-engine, etc.
 */

public interface IGetter
{
    public void setTarget(URL url) throws IOException;
    public void setHeader(String key, String value);
    public InputStream openStream() throws IOException;
}
