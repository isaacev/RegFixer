package edu.wisc.regfixer;

import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import edu.wisc.regfixer.server.RequestError;
import edu.wisc.regfixer.server.RequestPayload;
import edu.wisc.regfixer.server.ResponseError;
import edu.wisc.regfixer.server.ResponsePayload;
import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFileLocation;

public class Server {
  public static void start (int portNum, int loopLimit) {
    Gson gson = new Gson();

    port(portNum);

    staticFileLocation("/dist");

    get("/", (req, res) -> {
      return new ModelAndView(null, "frontend.html");
    }, new FreeMarkerEngine());

    post("/api/fix", (req, res) -> {
      RequestPayload request = null;

      try {
        request = gson.fromJson(req.body(), RequestPayload.class);
      } catch (JsonSyntaxException ex) {
        res.status(400);
        return gson.toJson(new RequestError("malformed JSON"));
      } catch (Exception ex) {
        res.status(400);
        return gson.toJson(new RequestError("malformed JSON"));
      }

      String result = "";
      try {
        result = RegFixer.fix(request.toJob(), loopLimit);
      } catch (TimeoutException ex) {
        res.status(408);
        return gson.toJson(new ResponseError("synthesis timeout"));
      }

      res.status(200);
      res.type("application/json; charset=utf-8");

      if (result == null) {
        return gson.toJson(new ResponseError("synthesis failed"));
      } else {
        return gson.toJson(new ResponsePayload(result));
      }
    });
  }
}
