package edu.wisc.regfixer.server;

import static spark.Spark.*;
import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.fixer.Job;
import edu.wisc.regfixer.fixer.Main;

public class Server {
  public static void main(String[] args) {
    Gson gson = new Gson();

    port(Integer.valueOf(System.getenv("PORT")));

    staticFileLocation("/dist");

    get("/", (req, res) -> {
      return new ModelAndView(null, "frontend.html");
    }, new FreeMarkerEngine());

    post("/api/fix", (req, res) -> {
      Payload payload = null;

      try {
        payload = gson.fromJson(req.body(), Payload.class);
      } catch (JsonSyntaxException ex) {
        res.status(400);
        String error = "malformed JSON";
        return gson.toJson(new JobError(error));
      } catch (Exception ex) {
        res.status(400);
        String error = "malformed JSON";
        return gson.toJson(new JobError(error));
      }

      if (payload.getRegex() == null) {
        res.status(422);
        String error = "missing \"regex\" field";
        return gson.toJson(new JobError(error));
      }

      if (payload.getRanges() == null) {
        res.status(422);
        String error = "missing \"ranges\" field";
        return gson.toJson(new JobError(error));
      }

      if (payload.getCorpus() == null) {
        res.status(422);
        String error = "missing \"corpus\" field";
        return gson.toJson(new JobError(error));
      }

      Job job = null;
      try {
        job = payload.toJob();
      } catch (Exception ex) {
        res.status(400);
        String error = "malformed regular expression";
        return gson.toJson(new JobError(error));
      }

      RegexNode candidate = Main.synthesize(job);

      if (candidate == null) {
        res.status(204);
        return gson.toJson(new JobError("could not synthesize a fix"));
      } else {
        res.status(200);
        return gson.toJson(candidate.toString());
      }
    });
  }
}
