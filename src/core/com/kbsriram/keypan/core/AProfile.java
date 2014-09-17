package com.kbsriram.keypan.core;

import java.net.URL;

/**
 * Data class to hold common information that's usually present on a
 * profile site.
 */
public abstract class AProfile
{
    public abstract URL getSiteIcon();
    public abstract String getSiteName();
    public abstract String getConfirmTitle();

    public String getUid()
    { return m_uid; }

    public URL getIconURL()
    { return m_iconurl; }

    public URL getProfileURL()
    { return m_profileurl; }

    public URL getConfirmURL()
    { return m_confirmurl; }

    public String getDisplayName()
    { return m_displayname!=null?m_displayname:m_uid; }

    public boolean isCritical()
    { return m_iscritical; }

    public AProfile setIconURL(URL url)
    {
        m_iconurl = url;
        return this;
    }
    public AProfile setProfileURL(URL url)
    {
        m_profileurl = url;
        return this;
    }
    public AProfile setConfirmURL(URL url)
    {
        m_confirmurl = url;
        return this;
    }
    public AProfile setDisplayName(String n)
    {
        m_displayname = n;
        return this;
    }
    public AProfile setIsCritical(boolean v)
    {
        m_iscritical = v;
        return this;
    }
    protected AProfile(String uid)
    { m_uid = uid; }

    private final String m_uid;
    private URL m_iconurl;
    private URL m_profileurl;
    private URL m_confirmurl;
    private String m_displayname;
    private boolean m_iscritical = false;
}
