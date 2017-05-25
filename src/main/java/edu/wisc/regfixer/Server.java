package edu.wisc.regfixer;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import edu.wisc.regfixer.server.RequestError;
import edu.wisc.regfixer.server.RequestPayload;
import edu.wisc.regfixer.server.ResponseError;
import edu.wisc.regfixer.server.ResponsePayload;
import static spark.Spark.port;
import static spark.Spark.post;

public class Server {
  public static void main (String[] args) {
    Gson gson = new Gson();

    if (System.getenv("PORT") == null) {
      System.err.println("Usage: PORT=<NUMBER> regfixer");
      System.exit(1);
    }

    try {
      port(Integer.valueOf(System.getenv("PORT")));
    } catch (NumberFormatException ex) {
      System.err.println("Usage: PORT=<NUMBER> regfixer");
      System.exit(1);
    }

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

      String result = RegFixer.fix(request.toJob());

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
