package de.dfki.resc28.miniold.service;

import io.minio.errors.ErrorResponseException;

import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import de.dfki.resc28.igraphstore.Constants;
import de.dfki.resc28.miniold.resources.Bucket;
import de.dfki.resc28.miniold.resources.BucketList;
import de.dfki.resc28.miniold.resources.FileObject;


@Path("")
public class MinioLinkedAPI 
{
	@GET
	@Produces({ Constants.CT_APPLICATION_JSON_LD, Constants.CT_APPLICATION_NQUADS, Constants.CT_APPLICATION_NTRIPLES, Constants.CT_APPLICATION_RDF_JSON, Constants.CT_APPLICATION_RDFXML, Constants.CT_APPLICATION_TRIX, Constants.CT_APPLICATION_XTURTLE, Constants.CT_TEXT_N3, Constants.CT_TEXT_TRIG, Constants.CT_TEXT_TURTLE })
	public Response getBuckets ( @HeaderParam(HttpHeaders.ACCEPT) @DefaultValue(Constants.CT_TEXT_TURTLE) final String acceptType )
	{	
		try 
		{
			BucketList buckets = new BucketList();
			return buckets.read(acceptType);
		} 
		catch (Exception e) 
		{
			throw new WebApplicationException(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
		} 
	}

	@POST
	public Response createBucket ( @HeaderParam("Slug") String bucketName )
	{
		try
		{
			BucketList buckets = new BucketList();
			return buckets.add(bucketName);
		}
		catch (Exception e) 
		{
			throw new WebApplicationException(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
		}
	}

	@GET
	@Path(("{bucketId}"))
	@Produces({ Constants.CT_APPLICATION_JSON_LD, Constants.CT_APPLICATION_NQUADS, Constants.CT_APPLICATION_NTRIPLES, Constants.CT_APPLICATION_RDF_JSON, Constants.CT_APPLICATION_RDFXML, Constants.CT_APPLICATION_TRIX, Constants.CT_APPLICATION_XTURTLE, Constants.CT_TEXT_N3, Constants.CT_TEXT_TRIG, Constants.CT_TEXT_TURTLE })
	public Response getBucket ( @HeaderParam(HttpHeaders.ACCEPT) @DefaultValue(Constants.CT_TEXT_TURTLE) final String acceptType,
								@PathParam("bucketId") String bucketId )
	{
		try
		{
			Bucket bucket = new Bucket(bucketId);
			return bucket.read(acceptType);
		}
		catch (Exception e) 
		{
			throw new WebApplicationException(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
		}
	}

	@DELETE
	@Path(("{bucketId}"))
	public Response deleteBucket ( @PathParam("bucketId") String bucketId )
	{
		try
		{
			Bucket bucket = new Bucket(bucketId);
			return bucket.delete();
		}
		catch (Exception e) 
		{
			throw new WebApplicationException(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
		}
	}

	@POST
	@Path(("{bucketId}"))
	public Response addObject ( InputStream input, 
								@PathParam("bucketId") String bucketId, 
								@HeaderParam("Slug") String objectId,
								@HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType )
	{
		try
		{
			Bucket bucket = new Bucket(bucketId);
			return bucket.add(objectId, contentType, input, Long.valueOf(fRequestHeaders.getLength()));
		}
		catch (Exception e) 
		{
			throw new WebApplicationException(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
		}
	}
	
	@GET
	@Path(("{bucketId}/{objectId}"))
	@Produces({ Constants.CT_APPLICATION_JSON_LD, Constants.CT_APPLICATION_NQUADS, Constants.CT_APPLICATION_NTRIPLES, Constants.CT_APPLICATION_RDF_JSON, Constants.CT_APPLICATION_RDFXML, Constants.CT_APPLICATION_TRIX, Constants.CT_APPLICATION_XTURTLE, Constants.CT_TEXT_N3, Constants.CT_TEXT_TRIG, Constants.CT_TEXT_TURTLE })
	public Response getObject ( @HeaderParam(HttpHeaders.ACCEPT) @DefaultValue(Constants.CT_TEXT_TURTLE) final String acceptType,
								@PathParam("bucketId") String bucketId, 
								@PathParam("objectId") String objectId )
	{
		try
		{
			FileObject object = new FileObject(bucketId, objectId);
			return object.read(acceptType);
		}
		catch (Exception e) 
		{
			throw new WebApplicationException(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
		}		
	}

	@DELETE
	@Path(("{bucketId}/{objectId}"))
	public Response deleteObject ( @PathParam("bucketId") String bucketId,
								   @PathParam("objectId") String objectId )
	{
		try
		{
			FileObject object = new FileObject(bucketId, objectId);
			return object.delete();
		}
		catch (Exception e) 
		{
			throw new WebApplicationException(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
		}
	}

	@PUT
	@Path(("{bucketId}/{objectId}"))
	public Response updateObject ( InputStream input, 
								   @PathParam("bucketId") String bucketId,
								   @PathParam("objectId") String objectId,
								   @HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType )
	{
		try
		{
			FileObject object = new FileObject(bucketId, objectId);
			return object.update(contentType, input, Long.valueOf(fRequestHeaders.getLength()));
		}
		catch (Exception e) 
		{
			throw new WebApplicationException(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
		}
	}
	
	@Context HttpServletRequest fRequest;
	@Context protected ServletContext fContext;
	@Context protected HttpHeaders fRequestHeaders;
	@Context protected UriInfo fRequestUrl;
	protected static String fPublicURI = null;
}
