package us.dot.its.jpo.ode.storage;

public enum LogFileType {
  BSM,
  OBU,
  UNKNOWN;

  public static LogFileType fromString(String type) {
    if ("bsmlog".equalsIgnoreCase(type)) {
      return BSM;
    } else if ("obulog".equalsIgnoreCase(type)) {
      return OBU;
    } else {
      return UNKNOWN;
    }
  }
}
