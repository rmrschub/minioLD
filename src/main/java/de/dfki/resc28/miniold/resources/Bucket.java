package de.dfki.resc28.miniold.resources;

import io.minio.ObjectStat;
import io.minio.Result;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidArgumentException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidExpiresRangeException;
import io.minio.errors.NoResponseException;
import io.minio.messages.Item;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.xmlpull.v1.XmlPullParserException;

import com.google.common.base.Strings;

import de.dfki.resc28.miniold.Server;
import de.dfki.resc28.miniold.util.URIBuilder;
import de.dfki.resc28.miniold.vocabularies.DCAT;

public class Bucket 
{
	private String bucketId;
	
	public Bucket(String bucketId) 
	{
		this.bucketId = bucketId;
	}
	
	public Response read(final String acceptType) throws InvalidKeyException, InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, NoResponseException, ErrorResponseException, InternalException, IOException, XmlPullParserException, URISyntaxException, InvalidExpiresRangeException
	{
		final Model bucketModel = ModelFactory.createDefaultModel();
		bucketModel.setNsPrefix("xsd", XSD.NS);
		bucketModel.setNsPrefix("rdf", RDF.uri);
		bucketModel.setNsPrefix("rdfs", RDFS.uri);
		bucketModel.setNsPrefix("dcterms", DCTerms.NS);
		bucketModel.setNsPrefix("dcat", DCAT.NAMESPACE.getURI());
		Resource bucket = bucketModel.createResource(new URIBuilder(Server.fBaseURI).addPath(this.bucketId).build().toString());
		bucketModel.add(bucket, RDF.type, DCAT.Dataset);
		
		for (Result<Item> r : Server.fMinioClient.listObjects(this.bucketId))
		{
			Item item = r.get();
			ObjectStat stat = Server.fMinioClient.statObject(this.bucketId, item.objectName());

			Resource object = bucketModel.createResource(new URIBuilder(Server.fBaseURI).addPath(this.bucketId).addPath(stat.name()).build().toString());
			bucketModel.add(object, RDF.type, DCAT.Distribution);
			bucketModel.add(object, DCTerms.title, bucketModel.createTypedLiteral(stat.name()) );
			bucketModel.add(object, DCTerms.created, bucketModel.createTypedLiteral(stat.createdTime()) );
			bucketModel.add(object, DCTerms.modified, bucketModel.createTypedLiteral(item.lastModified()) );
			bucketModel.add(object, DCAT.downloadURL, bucketModel.createTypedLiteral(Server.fMinioClient.presignedGetObject(this.bucketId, item.objectName()),XSDDatatype.XSDanyURI) );
			bucketModel.add(object, DCAT.mediaType, bucketModel.createTypedLiteral(stat.contentType()) );
			bucketModel.add(object, DCTerms.format, bucketModel.createTypedLiteral(stat.contentType()) );
			bucketModel.add(object, DCAT.byteSize, bucketModel.createTypedLiteral(stat.length()) );						
			bucketModel.add(bucket, DCAT.distribution, object);
		}
		
		StreamingOutput out = new StreamingOutput() 
		{
			public void write(OutputStream output) throws IOException, WebApplicationException
			{
				RDFDataMgr.write(output, bucketModel, RDFDataMgr.determineLang(null, acceptType, null)) ;
			}
		};
		
		return Response.ok(out)
					   .type(acceptType)
					   .build();
	}
	
	public Response delete() throws InvalidKeyException, InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, NoResponseException, ErrorResponseException, InternalException, IOException, XmlPullParserException 
	{
		if (!Server.fMinioClient.bucketExists(this.bucketId))
		{
			return Response.status(Status.NOT_FOUND).build();
		}
		else if (Server.fMinioClient.listObjects(this.bucketId).iterator().hasNext()) 
		{
			return Response.status(Status.BAD_REQUEST).entity("Bucket not empty!").build();
		}
		else
		{
			Server.fMinioClient.removeBucket(this.bucketId);
			return Response.seeOther(Server.fBaseURI).build();
		}
	}
	
	public Response add(String objectId, String contentType, InputStream input, long inputSize) throws InvalidKeyException, InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, NoResponseException, ErrorResponseException, InternalException, InvalidArgumentException, IOException, XmlPullParserException, URISyntaxException
	{
		if ( Strings.isNullOrEmpty(objectId) )
			objectId = java.util.UUID.randomUUID().toString();
		
		Server.fMinioClient.putObject(this.bucketId, objectId, input, inputSize, contentType);

		return Response.created(new URIBuilder(Server.fBaseURI).addPath(this.bucketId).addPath(objectId).build())
					   .build();
	}


	
	@Context protected UriInfo fRequestUrl;
}

