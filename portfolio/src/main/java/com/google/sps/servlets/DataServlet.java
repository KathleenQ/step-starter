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

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that handles input comments data and returns the contents
 * with the limit number of maximum comments' number sent by viewers.
 */
@WebServlet("/data")
public class DataServlet extends HttpServlet {
  DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Query query = new Query("Comment");
    Iterable<Entity> results = (datastore.prepare(query)).asIterable();
    List<String> commentsStorage = new ArrayList<>();
    List<Integer> boundsStorage = new ArrayList<>();

    // Collect all comments and the preferred maximum numbers of comments.
    results.forEach(e -> {
      addCommentContents(e, commentsStorage);
      collectMaxCommentsNum(e, boundsStorage);
    });

    // Show the specific number of comments.
    String json =
        new Gson().toJson(collectShownComments(commentsStorage, getMaxCommentsNum(boundsStorage)));
    response.setContentType("text/html");
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String comment = request.getParameter("comments-input");
    String maxCommentsNumString = request.getParameter("max-comments-num");
    int maxCommentsNum = 0;
    if (maxCommentsNumString != null) {
      maxCommentsNum = Integer.parseInt(maxCommentsNumString);
    }
    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("contents", comment);
    commentEntity.setProperty("bound", maxCommentsNum);
    datastore.put(commentEntity);
    response.setContentType("text/html;");
    response.getWriter().println("Thank you for your comments!");
  }

  /** For a specific entity, add the inserted maximum comments' number to the collection. */
  public void collectMaxCommentsNum(Entity entity, List<Integer> boundsStorage) {
    if (entity.getProperty("bound") != null) {
      long boundLong = (long) entity.getProperty("bound");
      int bound = Math.toIntExact(boundLong);
      boundsStorage.add(bound);
    }
  }

  /** For a specific entity, add its comment's contents to the storage. */
  public void addCommentContents(Entity entity, List<String> commentsStorage) {
    String contents = (String) entity.getProperty("contents");
    if (contents != "") {
      commentsStorage.add(contents);
    }
  }

  /** Returns the output comments' collection with the limitation of maximum comments' number. */
  public List<String> collectShownComments(List<String> commentsStorage, int maxCommentsNum) {
    if (commentsStorage.size() > maxCommentsNum) {
      commentsStorage = commentsStorage.subList(0, maxCommentsNum);
    }
    return commentsStorage;
  }

  /** Calculates the final used maximum number of comments. */
  public int getMaxCommentsNum(List<Integer> boundsStorage) {
    // Sets 3 as default if nobody inserts a preferred maximum comments' number.
    int maxCommentsNum = 3;
    if (!boundsStorage.isEmpty()) {
      maxCommentsNum = Collections.max(boundsStorage);
    }
    return maxCommentsNum;
  }
}
