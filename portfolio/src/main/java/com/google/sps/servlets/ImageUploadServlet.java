// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package main.java.com.google.sps.servlets;

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that handles the upload image and show it to the viewer using Blobstore. */
@WebServlet("/my-form-handler")
public class ImageUploadServlet extends HttpServlet {
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get the URL of the image that the user uploaded to Blobstore.
    final String imageUrl = getUploadedImageUrl(request, "image");

    // Output some HTML that shows the data the user entered.
    // A real codebase would probably store these in Datastore.
    PrintWriter out = response.getWriter();
    out.println("<p>Here's the image you uploaded:</p>");
    out.println("<a href=\"" + imageUrl + "\">");
    out.println("<img src=\"" + imageUrl + "\" />");
    out.println("</a>");
  }

  /** Returns a URL that points to the uploaded image, or null if the user didn't upload a image. */
  private String getUploadedImageUrl(HttpServletRequest request, String formInputElementName) {
    final BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    final Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
    final List<BlobKey> blobKeys = blobs.get("image");

    // If a viewer submitted the form without selecting a image, no URL will be got.
    if (blobKeys == null || blobKeys.isEmpty()) {
      return null;
    }

    // The form only contains a single image input, so only the first index needs to be got.
    final BlobKey blobKey = blobKeys.get(0);

    // Handle the no image submission case for live servers.
    final BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
    if (blobInfo.getSize() == 0) {
      blobstoreService.delete(blobKey);
      return null;
    }

    // Get the URL that points to the uploaded image, and thus get the corresponding image.
    final ImagesService imagesService = ImagesServiceFactory.getImagesService();
    final ServingUrlOptions options = ServingUrlOptions.Builder.withBlobKey(blobKey);
    try {
      URL url = new URL(imagesService.getServingUrl(options));
      return url.getPath();
    } catch (MalformedURLException e) {
      return imagesService.getServingUrl(options);
    }
  }
}
