package de.dfki.resc28.miniold.resources;

import io.minio.ObjectStat;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidArgumentException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidExpiresRangeException;
import io.minio.errors.NoResponseException;

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

import de.dfki.resc28.miniold.Server;
import de.dfki.resc28.miniold.util.URIBuilder;
import de.dfki.resc28.miniold.vocabularies.DCAT;

public class FileObject 
{
	private String bucketId;
	private String objectId;
	
	public FileObject(String bucketId, String objectId) 
	{
		this.bucketId = bucketId;
		this.objectId = objectId;
	}

	public Response read(final String acceptType) throws InvalidKeyException, InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, NoResponseException, ErrorResponseException, InternalException, IOException, XmlPullParserException, URISyntaxException, InvalidExpiresRangeException 
	{
		ObjectStat stat = Server.fMinioClient.statObject(this.bucketId, this.objectId);
		
		final Model objectModel = ModelFactory.createDefaultModel();
		objectModel.setNsPrefix("xsd", XSD.NS);
		objectModel.setNsPrefix("rdf", RDF.uri);
		objectModel.setNsPrefix("rdfs", RDFS.uri);
		objectModel.setNsPrefix("dcterms", DCTerms.NS);
		objectModel.setNsPrefix("dcat", DCAT.NAMESPACE.getURI());
		
		Resource object = objectModel.createResource(new URIBuilder(Server.fBaseURI).addPath(this.bucketId).addPath(objectId).build().toString());
		
		objectModel.add(object, RDF.type, DCAT.Distribution);
		objectModel.add(object, DCTerms.title, objectModel.createTypedLiteral(stat.name()) );
		objectModel.add(object, DCTerms.created, objectModel.createTypedLiteral(stat.createdTime()) );
		objectModel.add(object, DCAT.downloadURL, objectModel.createTypedLiteral(Server.fMinioClient.presignedGetObject(this.bucketId, this.objectId),XSDDatatype.XSDanyURI) );
		objectModel.add(object, DCAT.mediaType, objectModel.createTypedLiteral(stat.contentType()) );
		objectModel.add(object, DCTerms.format, objectModel.createTypedLiteral(stat.contentType()) );
		objectModel.add(object, DCAT.byteSize, objectModel.createTypedLiteral(stat.length()) );	

		Resource bucket = objectModel.createResource(new URIBuilder(Server.fBaseURI).addPath(this.bucketId).build().toString());
		
		objectModel.add(bucket, DCAT.distribution, object);
		
		StreamingOutput out = new StreamingOutput() 
		{
			public void write(OutputStream output) throws IOException, WebApplicationException
			{
				RDFDataMgr.write(output, objectModel, RDFDataMgr.determineLang(null, acceptType, null)) ;
			}
		};
		
		return Response.ok(out)
					   .type(acceptType)
					   .build();
	}
	
	public Response delete() throws InvalidKeyException, InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, NoResponseException, ErrorResponseException, InternalException, IOException, XmlPullParserException, URISyntaxException
	{
		Server.fMinioClient.removeObject(this.bucketId, this.objectId);
		
		return Response.seeOther(new URIBuilder(Server.fBaseURI).addPath(this.bucketId).build())
					   .build();
	}

	public Response update(String contentType, InputStream input, Long inputSize) throws InvalidKeyException, InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, NoResponseException, ErrorResponseException, InternalException, InvalidArgumentException, IOException, XmlPullParserException, URISyntaxException 
	{
		Server.fMinioClient.putObject(this.bucketId, this.objectId, input, inputSize, contentType);
		
		return Response.accepted().location(new URIBuilder(Server.fBaseURI).addPath(this.bucketId).addPath(objectId).build()).build();
	}
}
