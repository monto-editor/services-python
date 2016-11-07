package monto.service.python;

import monto.service.ZMQConfiguration;
import monto.service.completion.CodeCompletioner;
import monto.service.types.Languages;

public class PythonCodeCompletioner extends CodeCompletioner {
  public PythonCodeCompletioner(ZMQConfiguration zmqConfig) {
    super(
        zmqConfig,
        PythonServices.CODE_COMPLETIONER,
        "Code Completion",
        "A code completion service for Python",
        Languages.PYTHON,
        PythonServices.IDENTIFIER_FINDER);
  }
}
