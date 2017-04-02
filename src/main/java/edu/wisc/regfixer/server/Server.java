package edu.wisc.regfixer.server;

import static spark.Spark.*;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import edu.wisc.regfixer.fixer.Job;

public class Server {
  public static void main(String[] args) {
    Gson gson = new Gson();

    port(Integer.valueOf(System.getenv("PORT")));

    post("/api/fix", (req, res) -> {
      Job job = null;

      try {
        job = gson.fromJson(req.body(), Job.class);
      } catch (JsonSyntaxException ex) {
        res.status(400);
        String error = "malformed JSON";
        return gson.toJson(new JobError(error));
      } catch (Exception ex) {
        res.status(400);
        String error = "malformed JSON";
        return gson.toJson(new JobError(error));
      }

      if (job.getOriginalRegex() == null) {
        res.status(422);
        String error = "missing \"regex\" field";
        return gson.toJson(new JobError(error));
      }

      if (job.getSelectedRanges() == null) {
        res.status(422);
        String error = "missing \"ranges\" field";
        return gson.toJson(new JobError(error));
      }

      if (job.getCorpus() == null) {
        res.status(422);
        String error = "missing \"corpus\" field";
        return gson.toJson(new JobError(error));
      }

      return gson.toJson(job);
    });
  }
}
