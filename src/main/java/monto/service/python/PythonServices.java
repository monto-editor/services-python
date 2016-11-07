package monto.service.python;

import monto.service.types.ServiceId;

public final class PythonServices {
  public static final ServiceId TOKENIZER = new ServiceId("pythonTokenizer");
  public static final ServiceId PARSER = new ServiceId("pythonParser");
  public static final ServiceId OUTLINER = new ServiceId("pythonOutliner");
  public static final ServiceId CODE_COMPLETION = new ServiceId("pythonCodeCompletion");
  public static final ServiceId IDENTIFIER_FINDER = new ServiceId("pythonIdentifierFinder");

  private PythonServices() {}
}
