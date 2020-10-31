package au.com.dius.pact.core.support;

import com.github.michaelbull.result.Err;
import com.github.michaelbull.result.Ok;
import com.github.michaelbull.result.Result;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;

import java.util.Objects;

public class Version {
  private Integer major;
  private Integer minor;
  private Integer patch;

  public Version(Integer major, Integer minor, Integer patch) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
  }

  public Version(Integer major, Integer minor) {
    this.major = major;
    this.minor = minor;
  }

  public Integer getMajor() {
    return major;
  }

  public void setMajor(Integer major) {
    this.major = major;
  }

  public Integer getMinor() {
    return minor;
  }

  public void setMinor(Integer minor) {
    this.minor = minor;
  }

  public Integer getPatch() {
    return patch;
  }

  public void setPatch(Integer patch) {
    this.patch = patch;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Version version = (Version) o;
    return Objects.equals(major, version.major) &&
      Objects.equals(minor, version.minor) &&
      Objects.equals(patch, version.patch);
  }

  @Override
  public int hashCode() {
    return Objects.hash(major, minor, patch);
  }

  @Override
  public String toString() {
    if (patch == null) {
      return String.format("%d.%d", major, minor);
    } else {
      return String.format("%d.%d.%d", major, minor, patch);
    }
  }

  public static Result<Version, Exception> parse(String version) {
    CharStream charStream = CharStreams.fromString(version);
    VersionLexer lexer = new VersionLexer(charStream);
    TokenStream tokens = new CommonTokenStream(lexer);
    VersionParser parser = new VersionParser(tokens);
    VersionParser.VersionContext result = parser.version();
    if (result.exception != null) {
      return new Err(result.exception);
    } else {
      return new Ok(result.v);
    }
  }
}
