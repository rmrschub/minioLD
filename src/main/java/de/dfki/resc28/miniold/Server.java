package de.dfki.resc28.miniold;

import io.minio.MinioClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import de.dfki.resc28.miniold.service.MinioLinkedAPI;


@ApplicationPath("/")
public class Server extends Application 
{
	public static URI fBaseURI;
	public static URI fSerendipityURI;
	public static MinioClient fMinioClient; 
	

	public Server(@Context ServletContext servletContext) throws URISyntaxException, IOException
	{
		configure();
		
		
	}
	
	public Set<Object> getSingletons() 
    {	
		MinioLinkedAPI bla = new MinioLinkedAPI();
//		RepresentationEnricher enricher = new RepresentationEnricher(fSerendipityURI.toString());
		return new HashSet<Object>(Arrays.asList(bla));
    }

	public static synchronized void configure() 
    {
        try 
        {
            String configFile = System.getProperty("miniold.configuration");
            java.io.InputStream is;

            if (configFile != null) 
            {
                is = new java.io.FileInputStream(configFile);
                System.out.format("Loading MinioLD configuration from %s ...%n", configFile);
            } 
            else 
            {
                is = Server.class.getClassLoader().getResourceAsStream("miniold.properties");
                System.out.println("Loading MinioLD configuration from internal resource file ...");
            }

            java.util.Properties p = new Properties();
            p.load(is);
            
            

            
            Server.fMinioClient = new MinioClient(p.getProperty("minio.baseURI"),
            									  Integer.valueOf(p.getProperty("minio.port")) ,
            									  p.getProperty("minio.accessKey"),
            									  p.getProperty("minio.secretKey"));
            
            Server.fBaseURI = new URI(p.getProperty("minioLD.baseURI"));
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    public static String getProperty(java.util.Properties p, String key, String sysKey) 
    {
        String value = System.getProperty(sysKey);
        if (value != null) 
        {
            return value;
        }
        return p.getProperty(key);
    }

}
