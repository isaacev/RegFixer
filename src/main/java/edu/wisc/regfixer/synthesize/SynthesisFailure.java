package edu.wisc.regfixer.synthesize;

public class SynthesisFailure extends RuntimeException {
  public SynthesisFailure (String message) {
    super(message);
  }
}
