package de.dfki.resc28.miniold.resources;

import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.NoResponseException;
import io.minio.messages.Bucket;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

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

public class BucketList 
{
	public Response read (final String acceptType) throws InvalidKeyException, InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, NoResponseException, ErrorResponseException, InternalException, IOException, XmlPullParserException, URISyntaxException 
	{
		final Model catalogModel = ModelFactory.createDefaultModel();
		catalogModel.setNsPrefix("xsd", XSD.NS);
		catalogModel.setNsPrefix("rdf", RDF.uri);
		catalogModel.setNsPrefix("rdfs", RDFS.uri);
		catalogModel.setNsPrefix("dcterms", DCTerms.NS);
		catalogModel.setNsPrefix("dcat", DCAT.NAMESPACE.getURI());
		
		Resource catalog = catalogModel.createResource(Server.fBaseURI.toString());
		catalogModel.add(catalog, RDF.type, DCAT.Catalog);

		for (Bucket b : Server.fMinioClient.listBuckets())
		{
			Resource dataset = catalogModel.createResource(new URIBuilder(Server.fBaseURI).addPath(b.name()).build().toString());
			catalogModel.add(catalog, DCAT.dataset, dataset);
		}
				
		StreamingOutput out = new StreamingOutput() 
		{
			public void write(OutputStream output) throws IOException, WebApplicationException
			{		
				RDFDataMgr.write(output, catalogModel, RDFDataMgr.determineLang(null, acceptType, null)) ;
			}
		};
		
		return Response.ok(out)
					   .type(acceptType)
					   .build();
	}

	
	public Response add (String bucketName) throws InvalidKeyException, InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, NoResponseException, ErrorResponseException, InternalException, IOException, XmlPullParserException, URISyntaxException
	{
		if ( Strings.isNullOrEmpty(bucketName) || Server.fMinioClient.bucketExists(bucketName) )
			bucketName = java.util.UUID.randomUUID().toString();
		
		Server.fMinioClient.makeBucket(bucketName);
		
		return Response.created(new URIBuilder(Server.fBaseURI).addPath(bucketName).build())
				   .build();
	}
	
	
	@Context protected UriInfo fRequestUrl;
}
