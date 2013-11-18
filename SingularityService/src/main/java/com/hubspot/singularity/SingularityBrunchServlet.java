package com.hubspot.singularity;

import io.dropwizard.servlets.assets.AssetServlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;

import com.google.common.base.Charsets;

@SuppressWarnings("serial")
public class SingularityBrunchServlet extends AssetServlet {

  public SingularityBrunchServlet(String resourcePath, String uriPath, String indexFile) {
    super(resourcePath, uriPath, indexFile, Charsets.UTF_8);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    Request request = (Request) req;
    
    request.setPathInfo("/");
  
    super.doGet(request, resp);
  }
  
  

}
