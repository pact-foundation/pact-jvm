package au.com.dius.pact.core.matchers.engine.interpreter

import au.com.dius.pact.core.matchers.engine.ExecutionPlanNode
import au.com.dius.pact.core.matchers.engine.MatchingConfiguration
import au.com.dius.pact.core.matchers.engine.NodeResult
import au.com.dius.pact.core.matchers.engine.NodeValue
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.matchers.engine.bodies.XMLPlanBuilder
import au.com.dius.pact.core.matchers.engine.resolvers.ValueResolver
import au.com.dius.pact.core.matchers.engine.bodies.JsonPlanBuilder
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.json.JsonValue
import com.github.difflib.DiffUtils
import spock.lang.Specification

import static com.github.difflib.UnifiedDiffUtils.generateUnifiedDiff

@SuppressWarnings(['LineLength', 'ClassSize', 'UnusedVariable', 'UnnecessaryParenthesesForMethodCallWithClosure'])
class ExecutionPlanInterpreterSpec extends Specification {
  def 'json with null'() {
    given:
    List<String> path = ['$']
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)
    ExecutionPlanInterpreter interpreter = new ExecutionPlanInterpreter(context)

    def builder = JsonPlanBuilder.INSTANCE
    def content = JsonValue.Null.INSTANCE.serialise().bytes
    ExecutionPlanNode node = builder.buildPlan(content, context)

    ValueResolver resolver = Mock() {
      resolve(_, _) >> new Result.Ok( new NodeValue.BARRAY(content))
    }

    when:
    def result = interpreter.walkTree(path, node, resolver)
    def buffer = new StringBuilder()
    result.prettyForm(buffer, 0)
    def prettyResult = buffer.toString()

    then:
    result.result.value.bool == true
    prettyResult == '''%tee (
    |  %json:parse (
    |    $.body => BYTES(4, bnVsbA==)
    |  ) => json:null,
    |  :$ (
    |    %match:equality (
    |      json:null => json:null,
    |      ~>$ => json:null,
    |      NULL => NULL
    |    ) => BOOL(true)
    |  ) => BOOL(true)
    |) => BOOL(true)'''.stripMargin('|')
  }

  def 'json with null mismatch'() {
    given:
    List<String> path = ['$']
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)
    ExecutionPlanInterpreter interpreter = new ExecutionPlanInterpreter(context)

    def builder = JsonPlanBuilder.INSTANCE
    def content = JsonValue.Null.INSTANCE.serialise().bytes
    ExecutionPlanNode node = builder.buildPlan(content, context)

    ValueResolver resolver = Mock() {
      resolve(_, _) >> new Result.Ok( new NodeValue.BARRAY('true'.bytes))
    }

    when:
    def result = interpreter.walkTree(path, node, resolver)
    def buffer = new StringBuilder()
    result.prettyForm(buffer, 0)
    def prettyResult = buffer.toString()

    then:
    result.result.value.bool == false
    prettyResult == '''%tee (
    |  %json:parse (
    |    $.body => BYTES(4, dHJ1ZQ==)
    |  ) => json:true,
    |  :$ (
    |    %match:equality (
    |      json:null => json:null,
    |      ~>$ => json:true,
    |      NULL => NULL
    |    ) => ERROR(Expected true \\(Boolean\\) to be equal to null \\(Null\\))
    |  ) => BOOL(false)
    |) => BOOL(false)'''.stripMargin('|')
  }

  def 'invalid json'() {
    given:
    List<String> path = ['$']
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)
    ExecutionPlanInterpreter interpreter = new ExecutionPlanInterpreter(context)

    def builder = JsonPlanBuilder.INSTANCE
    def content = JsonValue.Null.INSTANCE.serialise().bytes
    ExecutionPlanNode node = builder.buildPlan(content, context)

    ValueResolver resolver = Mock() {
      resolve(_, _) >> new Result.Ok( new NodeValue.BARRAY('{'.bytes))
    }

    when:
    def result = interpreter.walkTree(path, node, resolver)
    def buffer = new StringBuilder()
    result.prettyForm(buffer, 0)
    def prettyResult = buffer.toString()

    then:
    result.result instanceof NodeResult.ERROR
    prettyResult == '''%tee (
    |  %json:parse (
    |    $.body => BYTES(1, ew==)
    |  ) => ERROR(json parse error: Invalid Json document \\(1:2\\) - found end of document while parsing object),
    |  :$ (
    |    %match:equality (
    |      json:null,
    |      ~>$,
    |      NULL
    |    )
    |  )
    |) => ERROR(json parse error: Invalid Json document \\(1:2\\) - found end of document while parsing object)'''
      .stripMargin('|')
  }

  def 'json with boolean'() {
    given:
    List<String> path = ['$']
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)
    ExecutionPlanInterpreter interpreter = new ExecutionPlanInterpreter(context)

    def builder = JsonPlanBuilder.INSTANCE
    def content = JsonValue.True.INSTANCE.serialise().bytes
    ExecutionPlanNode node = builder.buildPlan(content, context)

    ValueResolver resolver = Mock() {
      resolve(_, _) >> new Result.Ok( new NodeValue.BARRAY(content))
    }

    when:
    def result = interpreter.walkTree(path, node, resolver)
    def buffer = new StringBuilder()
    result.prettyForm(buffer, 0)
    def prettyResult = buffer.toString()

    then:
    result.result.value.bool == true
    prettyResult == '''%tee (
    |  %json:parse (
    |    $.body => BYTES(4, dHJ1ZQ==)
    |  ) => json:true,
    |  :$ (
    |    %match:equality (
    |      json:true => json:true,
    |      ~>$ => json:true,
    |      NULL => NULL
    |    ) => BOOL(true)
    |  ) => BOOL(true)
    |) => BOOL(true)'''.stripMargin('|')
  }

  def 'json with boolean mismatch'() {
    given:
    List<String> path = ['$']
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)
    ExecutionPlanInterpreter interpreter = new ExecutionPlanInterpreter(context)

    def builder = JsonPlanBuilder.INSTANCE
    def content = JsonValue.True.INSTANCE.serialise().bytes
    ExecutionPlanNode node = builder.buildPlan(content, context)

    ValueResolver resolver = Mock() {
      resolve(_, _) >> new Result.Ok( new NodeValue.BARRAY('false'.bytes))
    }

    when:
    def result = interpreter.walkTree(path, node, resolver)
    def buffer = new StringBuilder()
    result.prettyForm(buffer, 0)
    def prettyResult = buffer.toString()

    then:
    result.result.value.bool == false
    prettyResult == '''%tee (
    |  %json:parse (
    |    $.body => BYTES(5, ZmFsc2U=)
    |  ) => json:false,
    |  :$ (
    |    %match:equality (
    |      json:true => json:true,
    |      ~>$ => json:false,
    |      NULL => NULL
    |    ) => ERROR(Expected false \\(Boolean\\) to be equal to true \\(Boolean\\))
    |  ) => BOOL(false)
    |) => BOOL(false)'''.stripMargin('|')
  }

  def 'json with empty array with incorrect type'() {
    given:
    List<String> path = ['$']
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)
    ExecutionPlanInterpreter interpreter = new ExecutionPlanInterpreter(context)

    def builder = JsonPlanBuilder.INSTANCE
    ExecutionPlanNode node = builder.buildPlan('[]'.bytes, context)

    ValueResolver resolver = Mock() {
      resolve(_, _) >> new Result.Ok( new NodeValue.BARRAY('false'.bytes))
    }

    when:
    def result = interpreter.walkTree(path, node, resolver)
    def buffer = new StringBuilder()
    result.prettyForm(buffer, 0)
    def prettyResult = buffer.toString()

    then:
    result.result.value.bool == false
    prettyResult == '''%tee (
    |  %json:parse (
    |    $.body => BYTES(5, ZmFsc2U=)
    |  ) => json:false,
    |  :$ (
    |    %json:expect:empty (
    |      'ARRAY' => 'ARRAY',
    |      ~>$ => json:false
    |    ) => ERROR(Was expecting a JSON Array but got a Boolean)
    |  ) => BOOL(false)
    |) => BOOL(false)'''.stripMargin('|')
  }

  def 'json with empty array'() {
    given:
    List<String> path = ['$']
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)
    ExecutionPlanInterpreter interpreter = new ExecutionPlanInterpreter(context)

    def builder = JsonPlanBuilder.INSTANCE
    ExecutionPlanNode node = builder.buildPlan('[]'.bytes, context)

    ValueResolver resolver = Mock() {
      resolve(_, _) >> new Result.Ok( new NodeValue.BARRAY('[]'.bytes))
    }

    when:
    def result = interpreter.walkTree(path, node, resolver)
    def buffer = new StringBuilder()
    result.prettyForm(buffer, 0)
    def prettyResult = buffer.toString()

    then:
    result.result.value.bool == true
    prettyResult == '''%tee (
    |  %json:parse (
    |    $.body => BYTES(2, W10=)
    |  ) => json:[],
    |  :$ (
    |    %json:expect:empty (
    |      'ARRAY' => 'ARRAY',
    |      ~>$ => json:[]
    |    ) => BOOL(true)
    |  ) => BOOL(true)
    |) => BOOL(true)'''.stripMargin('|')
  }

  def 'json with empty array mismatch'() {
    given:
    List<String> path = ['$']
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)
    ExecutionPlanInterpreter interpreter = new ExecutionPlanInterpreter(context)

    def builder = JsonPlanBuilder.INSTANCE
    ExecutionPlanNode node = builder.buildPlan('[]'.bytes, context)

    ValueResolver resolver = Mock() {
      resolve(_, _) >> new Result.Ok( new NodeValue.BARRAY('[true]'.bytes))
    }

    when:
    def result = interpreter.walkTree(path, node, resolver)
    def buffer = new StringBuilder()
    result.prettyForm(buffer, 0)
    def prettyResult = buffer.toString()

    then:
    result.result.value.bool == false
    prettyResult == '''%tee (
    |  %json:parse (
    |    $.body => BYTES(6, W3RydWVd)
    |  ) => json:[true],
    |  :$ (
    |    %json:expect:empty (
    |      'ARRAY' => 'ARRAY',
    |      ~>$ => json:[true]
    |    ) => ERROR(Expected JSON Array \\([true]\\) to be empty)
    |  ) => BOOL(false)
    |) => BOOL(false)'''.stripMargin('|')
  }

  def 'json with array'() {
    given:
    List<String> path = ['$']
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)
    ExecutionPlanInterpreter interpreter = new ExecutionPlanInterpreter(context)

    def builder = JsonPlanBuilder.INSTANCE
    ExecutionPlanNode node = builder.buildPlan('[1, 2, 3]'.bytes, context)

    ValueResolver resolver = Mock() {
      resolve(_, _) >> new Result.Ok( new NodeValue.BARRAY('[1,2,3]'.bytes))
    }

    def expected = '''%tee (
    |  %json:parse (
    |    $.body => BYTES(7, WzEsMiwzXQ==)
    |  ) => json:[1,2,3],
    |  :$ (
    |    %json:match:length (
    |      'ARRAY' => 'ARRAY',
    |      UINT(3) => UINT(3),
    |      ~>$ => json:[1,2,3]
    |    ) => BOOL(true),
    |    :$[0] (
    |      %if (
    |        %check:exists (
    |          ~>$[0] => json:1
    |        ) => BOOL(true),
    |        %match:equality (
    |          json:1 => json:1,
    |          ~>$[0] => json:1,
    |          NULL => NULL
    |        ) => BOOL(true)
    |      ) => BOOL(true)
    |    ) => BOOL(true),
    |    :$[1] (
    |      %if (
    |        %check:exists (
    |          ~>$[1] => json:2
    |        ) => BOOL(true),
    |        %match:equality (
    |          json:2 => json:2,
    |          ~>$[1] => json:2,
    |          NULL => NULL
    |        ) => BOOL(true)
    |      ) => BOOL(true)
    |    ) => BOOL(true),
    |    :$[2] (
    |      %if (
    |        %check:exists (
    |          ~>$[2] => json:3
    |        ) => BOOL(true),
    |        %match:equality (
    |          json:3 => json:3,
    |          ~>$[2] => json:3,
    |          NULL => NULL
    |        ) => BOOL(true)
    |      ) => BOOL(true)
    |    ) => BOOL(true)
    |  ) => BOOL(true)
    |) => BOOL(true)'''.stripMargin('|')

    when:
    def result = interpreter.walkTree(path, node, resolver)
    def buffer = new StringBuilder()
    result.prettyForm(buffer, 0)
    def prettyResult = buffer.toString()
    def patch = DiffUtils.diff(prettyResult, expected, null)
    def diff = generateUnifiedDiff('', '', prettyResult.split('\n') as List<String>, patch, 0).join('\n')

    then:
    result.result.value.bool == true
    diff == ''
  }

  def 'json with array with incorrect type'() {
    given:
    List<String> path = ['$']
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)
    ExecutionPlanInterpreter interpreter = new ExecutionPlanInterpreter(context)

    def builder = JsonPlanBuilder.INSTANCE
    ExecutionPlanNode node = builder.buildPlan('[1, 2, 3]'.bytes, context)

    ValueResolver resolver = Mock() {
      resolve(_, _) >> new Result.Ok( new NodeValue.BARRAY('false'.bytes))
    }

    def expected = '''%tee (
    |  %json:parse (
    |    $.body => BYTES(5, ZmFsc2U=)
    |  ) => json:false,
    |  :$ (
    |    %json:match:length (
    |      'ARRAY' => 'ARRAY',
    |      UINT(3) => UINT(3),
    |      ~>$ => json:false
    |    ) => ERROR(Was expecting a JSON Array but got a Boolean),
    |    :$[0] (
    |      %if (
    |        %check:exists (
    |          ~>$[0] => NULL
    |        ) => BOOL(false),
    |        %match:equality (
    |          json:1,
    |          ~>$[0],
    |          NULL
    |        )
    |      ) => BOOL(false)
    |    ) => BOOL(false),
    |    :$[1] (
    |      %if (
    |        %check:exists (
    |          ~>$[1] => NULL
    |        ) => BOOL(false),
    |        %match:equality (
    |          json:2,
    |          ~>$[1],
    |          NULL
    |        )
    |      ) => BOOL(false)
    |    ) => BOOL(false),
    |    :$[2] (
    |      %if (
    |        %check:exists (
    |          ~>$[2] => NULL
    |        ) => BOOL(false),
    |        %match:equality (
    |          json:3,
    |          ~>$[2],
    |          NULL
    |        )
    |      ) => BOOL(false)
    |    ) => BOOL(false)
    |  ) => BOOL(false)
    |) => BOOL(false)'''.stripMargin('|')

    when:
    def result = interpreter.walkTree(path, node, resolver)
    def buffer = new StringBuilder()
    result.prettyForm(buffer, 0)
    def prettyResult = buffer.toString()
    def patch = DiffUtils.diff(prettyResult, expected, null)
    def diff = generateUnifiedDiff('', '', prettyResult.split('\n') as List<String>, patch, 0).join('\n')

    then:
    result.result.value.bool == false
    diff == ''
  }

  def 'json with array mismatch with length and type'() {
    given:
    List<String> path = ['$']
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)
    ExecutionPlanInterpreter interpreter = new ExecutionPlanInterpreter(context)

    def builder = JsonPlanBuilder.INSTANCE
    ExecutionPlanNode node = builder.buildPlan('[1, 2, 3]'.bytes, context)

    ValueResolver resolver = Mock() {
      resolve(_, _) >> new Result.Ok( new NodeValue.BARRAY('[true]'.bytes))
    }

    def expected = '''%tee (
    |  %json:parse (
    |    $.body => BYTES(6, W3RydWVd)
    |  ) => json:[true],
    |  :$ (
    |    %json:match:length (
    |      'ARRAY' => 'ARRAY',
    |      UINT(3) => UINT(3),
    |      ~>$ => json:[true]
    |    ) => ERROR(Was expecting a length of 3, but actual length is 1),
    |    :$[0] (
    |      %if (
    |        %check:exists (
    |          ~>$[0] => json:true
    |        ) => BOOL(true),
    |        %match:equality (
    |          json:1 => json:1,
    |          ~>$[0] => json:true,
    |          NULL => NULL
    |        ) => ERROR(Expected true \\(Boolean\\) to be equal to 1 \\(Integer\\))
    |      ) => BOOL(false)
    |    ) => BOOL(false),
    |    :$[1] (
    |      %if (
    |        %check:exists (
    |          ~>$[1] => NULL
    |        ) => BOOL(false),
    |        %match:equality (
    |          json:2,
    |          ~>$[1],
    |          NULL
    |        )
    |      ) => BOOL(false)
    |    ) => BOOL(false),
    |    :$[2] (
    |      %if (
    |        %check:exists (
    |          ~>$[2] => NULL
    |        ) => BOOL(false),
    |        %match:equality (
    |          json:3,
    |          ~>$[2],
    |          NULL
    |        )
    |      ) => BOOL(false)
    |    ) => BOOL(false)
    |  ) => BOOL(false)
    |) => BOOL(false)'''.stripMargin('|')

    when:
    def result = interpreter.walkTree(path, node, resolver)
    def buffer = new StringBuilder()
    result.prettyForm(buffer, 0)
    def prettyResult = buffer.toString()
    def patch = DiffUtils.diff(prettyResult, expected, null)
    def diff = generateUnifiedDiff('', '', prettyResult.split('\n') as List<String>, patch, 0).join('\n')

    then:
    result.result.value.bool == false
    diff == ''
  }

  def 'json with array mismatch with value'() {
    given:
    List<String> path = ['$']
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)
    ExecutionPlanInterpreter interpreter = new ExecutionPlanInterpreter(context)

    def builder = JsonPlanBuilder.INSTANCE
    ExecutionPlanNode node = builder.buildPlan('[1, 2, 3]'.bytes, context)

    ValueResolver resolver = Mock() {
      resolve(_, _) >> new Result.Ok( new NodeValue.BARRAY('[1,3,3]'.bytes))
    }

    def expected = '''%tee (
    |  %json:parse (
    |    $.body => BYTES(7, WzEsMywzXQ==)
    |  ) => json:[1,3,3],
    |  :$ (
    |    %json:match:length (
    |      'ARRAY' => 'ARRAY',
    |      UINT(3) => UINT(3),
    |      ~>$ => json:[1,3,3]
    |    ) => BOOL(true),
    |    :$[0] (
    |      %if (
    |        %check:exists (
    |          ~>$[0] => json:1
    |        ) => BOOL(true),
    |        %match:equality (
    |          json:1 => json:1,
    |          ~>$[0] => json:1,
    |          NULL => NULL
    |        ) => BOOL(true)
    |      ) => BOOL(true)
    |    ) => BOOL(true),
    |    :$[1] (
    |      %if (
    |        %check:exists (
    |          ~>$[1] => json:3
    |        ) => BOOL(true),
    |        %match:equality (
    |          json:2 => json:2,
    |          ~>$[1] => json:3,
    |          NULL => NULL
    |        ) => ERROR(Expected 3 \\(Integer\\) to be equal to 2 \\(Integer\\))
    |      ) => BOOL(false)
    |    ) => BOOL(false),
    |    :$[2] (
    |      %if (
    |        %check:exists (
    |          ~>$[2] => json:3
    |        ) => BOOL(true),
    |        %match:equality (
    |          json:3 => json:3,
    |          ~>$[2] => json:3,
    |          NULL => NULL
    |        ) => BOOL(true)
    |      ) => BOOL(true)
    |    ) => BOOL(true)
    |  ) => BOOL(false)
    |) => BOOL(false)'''.stripMargin('|')

    when:
    def result = interpreter.walkTree(path, node, resolver)
    def buffer = new StringBuilder()
    result.prettyForm(buffer, 0)
    def prettyResult = buffer.toString()
    def patch = DiffUtils.diff(prettyResult, expected, null)
    def diff = generateUnifiedDiff('', '', prettyResult.split('\n') as List<String>, patch, 0).join('\n')

    then:
    result.result.value.bool == false
    diff == ''
  }

  def 'very simple xml'() {
    given:
    List<String> path = ['$']
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)
    ExecutionPlanInterpreter interpreter = new ExecutionPlanInterpreter(context)

    def builder = XMLPlanBuilder.INSTANCE
    def content = '<foo>test</foo>'.bytes
    ExecutionPlanNode node = builder.buildPlan(content, context)

    ValueResolver resolver = Mock() {
      resolve(_, _) >> { new Result.Ok(new NodeValue.BARRAY(content)) }
    }

    when:
    def result = interpreter.walkTree(path, node, resolver)
    def buffer = new StringBuilder()
    result.prettyForm(buffer, 0)
    def prettyResult = buffer.toString()
    def expected = '''%tee (
      |  %xml:parse (
      |    $.body => BYTES(15, PGZvbz50ZXN0PC9mb28+)
      |  ) => xml:'<foo>test</foo>',
      |  :$ (
      |    %if (
      |      %check:exists (
      |        ~>$.foo => xml:'<foo>test</foo>'
      |      ) => BOOL(true),
      |      :$.foo (
      |        :#text (
      |          %match:equality (
      |            'test' => 'test',
      |            %to-string (
      |              ~>$.foo['#text'] => xml:text:'test'
      |            ) => 'test',
      |            NULL => NULL
      |          ) => BOOL(true)
      |        ) => BOOL(true),
      |        %expect:empty (
      |          ~>$.foo => xml:'<foo>test</foo>'
      |        ) => BOOL(true)
      |      ) => BOOL(true),
      |      %error (
      |        'Was expecting an XML element \\/foo but it was missing'
      |      )
      |    ) => BOOL(true)
      |  ) => BOOL(true)
      |) => BOOL(true)'''.stripMargin('|')
    def patch = DiffUtils.diff(prettyResult, expected, null)
    def diff = generateUnifiedDiff('', '', prettyResult.split('\n') as List<String>, patch, 0).join('\n')

    then:
    result.result.value.bool == true
    diff == ''

    when:
    content = '<bar></bar>'.bytes
    result = interpreter.walkTree(path, node, resolver)
    buffer = new StringBuilder()
    result.prettyForm(buffer, 0)
    prettyResult = buffer.toString()
    expected = '''%tee (
      |  %xml:parse (
      |    $.body => BYTES(11, PGJhcj48L2Jhcj4=)
      |  ) => xml:'<bar/>',
      |  :$ (
      |    %if (
      |      %check:exists (
      |        ~>$.foo => NULL
      |      ) => BOOL(false),
      |      :$.foo (
      |        :#text (
      |          %match:equality (
      |            'test',
      |            %to-string (
      |              ~>$.foo['#text']
      |            ),
      |            NULL
      |          )
      |        ),
      |        %expect:empty (
      |          ~>$.foo
      |        )
      |      ),
      |      %error (
      |        'Was expecting an XML element \\/foo but it was missing' => 'Was expecting an XML element \\/foo but it was missing'
      |      ) => ERROR(Was expecting an XML element /foo but it was missing)
      |    ) => ERROR(Was expecting an XML element /foo but it was missing)
      |  ) => BOOL(false)
      |) => BOOL(false)'''.stripMargin('|')
    patch = DiffUtils.diff(prettyResult, expected, null)
    diff = generateUnifiedDiff('', '', prettyResult.split('\n') as List<String>, patch, 0).join('\n')

    then:
    result.result.value.bool == false
    diff == ''
  }

  def 'invalid XML'() {
    given:
    List<String> path = ['$']
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)
    ExecutionPlanInterpreter interpreter = new ExecutionPlanInterpreter(context)

    def builder = XMLPlanBuilder.INSTANCE
    def content = '<foo>test</foo>'.bytes
    ExecutionPlanNode node = builder.buildPlan(content, context)

    ValueResolver resolver = Mock() {
      resolve(_, _) >> { new Result.Ok(new NodeValue.BARRAY('<foo>test'.bytes)) }
    }

    when:
    def result = interpreter.walkTree(path, node, resolver)
    def buffer = new StringBuilder()
    result.prettyForm(buffer, 0)
    def prettyResult = buffer.toString()
    def expected = '''%tee (
      |  %xml:parse (
      |    $.body => BYTES(9, PGZvbz50ZXN0)
      |  ) => ERROR(XML parse error: XML document structures must start and end within the same entity.),
      |  :$ (
      |    %if (
      |      %check:exists (
      |        ~>$.foo
      |      ),
      |      :$.foo (
      |        :#text (
      |          %match:equality (
      |            'test',
      |            %to-string (
      |              ~>$.foo['#text']
      |            ),
      |            NULL
      |          )
      |        ),
      |        %expect:empty (
      |          ~>$.foo
      |        )
      |      ),
      |      %error (
      |        'Was expecting an XML element \\/foo but it was missing'
      |      )
      |    )
      |  )
      |) => ERROR(XML parse error: XML document structures must start and end within the same entity.)'''.stripMargin('|')
    def patch = DiffUtils.diff(prettyResult, expected, null)
    def diff = generateUnifiedDiff('', '', prettyResult.split('\n') as List<String>, patch, 0).join('\n')

    then:
    result.result instanceof NodeResult.ERROR
    diff == ''
  }

  def 'xml with missing items'() {
    given:
    List<String> path = ['$']
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)
    ExecutionPlanInterpreter interpreter = new ExecutionPlanInterpreter(context)

    def builder = XMLPlanBuilder.INSTANCE
    ExecutionPlanNode node = builder.buildPlan('<values><value>A</value><value>B</value></values>'.bytes, context)

    def content = '<values><value>A</value></values>'.bytes
    ValueResolver resolver = Mock() {
      resolve(_, _) >> { new Result.Ok(new NodeValue.BARRAY(content)) }
    }

    when:
    def result = interpreter.walkTree(path, node, resolver)
    def buffer = new StringBuilder()
    result.prettyForm(buffer, 0)
    def prettyResult = buffer.toString()
    def expected = '''%tee (
      |  %xml:parse (
      |    $.body => BYTES(33, PHZhbHVlcz48dmFsdWU+QTwvdmFsdWU+PC92YWx1ZXM+)
      |  ) => xml:'<values>\\n    <value>A<\\/value>\\n<\\/values>',
      |  :$ (
      |    %if (
      |      %check:exists (
      |        ~>$.values => xml:'<values>\\n    <value>A<\\/value>\\n<\\/values>'
      |      ) => BOOL(true),
      |      :$.values (
      |        :#text (
      |          %expect:empty (
      |            %to-string (
      |              ~>$.values['#text'] => NULL
      |            ) => ''
      |          ) => BOOL(true)
      |        ) => BOOL(true),
      |        %expect:only-entries (
      |          ['value'] => ['value'],
      |          ~>$.values => xml:'<values>\\n    <value>A<\\/value>\\n<\\/values>'
      |        ) => OK,
      |        %expect:count (
      |          UINT(2) => UINT(2),
      |          ~>$.values.value => xml:'<value>A</value>',
      |          %join (
      |            'Expected 2 <value> child elements but there were ' => 'Expected 2 <value> child elements but there were ',
      |            %length (
      |              ~>$.values.value => xml:'<value>A</value>'
      |            ) => UINT(1)
      |          ) => 'Expected 2 <value> child elements but there were 1'
      |        ) => ERROR(Expected 2 <value> child elements but there were 1),
      |        %if (
      |          %check:exists (
      |            ~>$.values.value[0] => xml:'<value>A</value>'
      |          ) => BOOL(true),
      |          :$.values.value[0] (
      |            :#text (
      |              %match:equality (
      |                'A' => 'A',
      |                %to-string (
      |                  ~>$.values.value[0]['#text'] => xml:text:'A'
      |                ) => 'A',
      |                NULL => NULL
      |              ) => BOOL(true)
      |            ) => BOOL(true),
      |            %expect:empty (
      |              ~>$.values.value[0] => xml:'<value>A</value>'
      |            ) => BOOL(true)
      |          ) => BOOL(true),
      |          %error (
      |            'Was expecting an XML element \\/values\\/value\\/0 but it was missing'
      |          )
      |        ) => BOOL(true),
      |        %if (
      |          %check:exists (
      |            ~>$.values.value[1] => NULL
      |          ) => BOOL(false),
      |          :$.values.value[1] (
      |            :#text (
      |              %match:equality (
      |                'B',
      |                %to-string (
      |                  ~>$.values.value[1]['#text']
      |                ),
      |                NULL
      |              )
      |            ),
      |            %expect:empty (
      |              ~>$.values.value[1]
      |            )
      |          ),
      |          %error (
      |            'Was expecting an XML element \\/values\\/value\\/1 but it was missing' => 'Was expecting an XML element \\/values\\/value\\/1 but it was missing'
      |          ) => ERROR(Was expecting an XML element /values/value/1 but it was missing)
      |        ) => ERROR(Was expecting an XML element /values/value/1 but it was missing)
      |      ) => BOOL(false),
      |      %error (
      |        'Was expecting an XML element \\/values but it was missing'
      |      )
      |    ) => BOOL(false)
      |  ) => BOOL(false)
      |) => BOOL(false)'''.stripMargin('|')
    def patch = DiffUtils.diff(prettyResult, expected, null)
    def diff = generateUnifiedDiff('', '', prettyResult.split('\n') as List<String>, patch, 0).join('\n')

    then:
    result.result.value.bool == false
    diff == ''
  }

  def 'xml with additional items'() {
    given:
    List<String> path = ['$']
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)
    ExecutionPlanInterpreter interpreter = new ExecutionPlanInterpreter(context)

    def builder = XMLPlanBuilder.INSTANCE
    ExecutionPlanNode node = builder.buildPlan('<values><value>A</value><value>B</value></values>'.bytes, context)

    def content = '<values><value>A</value><value>B</value><value>C</value></values>'.bytes
    ValueResolver resolver = Mock() {
      resolve(_, _) >> { new Result.Ok(new NodeValue.BARRAY(content)) }
    }

    when:
    def result = interpreter.walkTree(path, node, resolver)
    def buffer = new StringBuilder()
    result.prettyForm(buffer, 0)
    def prettyResult = buffer.toString()
    def expected = '''%tee (
      |  %xml:parse (
      |    $.body => BYTES(65, PHZhbHVlcz48dmFsdWU+QTwvdmFsdWU+PHZhbHVlPkI8L3ZhbHVlPjx2YWx1ZT5DPC92YWx1ZT48L3ZhbHVlcz4=)
      |  ) => xml:'<values>\\n    <value>A<\\/value>\\n    <value>B<\\/value>\\n    <value>C<\\/value>\\n<\\/values>',
      |  :$ (
      |    %if (
      |      %check:exists (
      |        ~>$.values => xml:'<values>\\n    <value>A<\\/value>\\n    <value>B<\\/value>\\n    <value>C<\\/value>\\n<\\/values>'
      |      ) => BOOL(true),
      |      :$.values (
      |        :#text (
      |          %expect:empty (
      |            %to-string (
      |              ~>$.values['#text'] => NULL
      |            ) => ''
      |          ) => BOOL(true)
      |        ) => BOOL(true),
      |        %expect:only-entries (
      |          ['value'] => ['value'],
      |          ~>$.values => xml:'<values>\\n    <value>A<\\/value>\\n    <value>B<\\/value>\\n    <value>C<\\/value>\\n<\\/values>'
      |        ) => OK,
      |        %expect:count (
      |          UINT(2) => UINT(2),
      |          ~>$.values.value => [xml:'<value>A</value>', xml:'<value>B</value>', xml:'<value>C</value>'],
      |          %join (
      |            'Expected 2 <value> child elements but there were ' => 'Expected 2 <value> child elements but there were ',
      |            %length (
      |              ~>$.values.value => [xml:'<value>A</value>', xml:'<value>B</value>', xml:'<value>C</value>']
      |            ) => UINT(3)
      |          ) => 'Expected 2 <value> child elements but there were 3'
      |        ) => ERROR(Expected 2 <value> child elements but there were 3),
      |        %if (
      |          %check:exists (
      |            ~>$.values.value[0] => xml:'<value>A</value>'
      |          ) => BOOL(true),
      |          :$.values.value[0] (
      |            :#text (
      |              %match:equality (
      |                'A' => 'A',
      |                %to-string (
      |                  ~>$.values.value[0]['#text'] => xml:text:'A'
      |                ) => 'A',
      |                NULL => NULL
      |              ) => BOOL(true)
      |            ) => BOOL(true),
      |            %expect:empty (
      |              ~>$.values.value[0] => xml:'<value>A</value>'
      |            ) => BOOL(true)
      |          ) => BOOL(true),
      |          %error (
      |            'Was expecting an XML element \\/values\\/value\\/0 but it was missing'
      |          )
      |        ) => BOOL(true),
      |        %if (
      |          %check:exists (
      |            ~>$.values.value[1] => xml:'<value>B</value>'
      |          ) => BOOL(true),
      |          :$.values.value[1] (
      |            :#text (
      |              %match:equality (
      |                'B' => 'B',
      |                %to-string (
      |                  ~>$.values.value[1]['#text'] => xml:text:'B'
      |                ) => 'B',
      |                NULL => NULL
      |              ) => BOOL(true)
      |            ) => BOOL(true),
      |            %expect:empty (
      |              ~>$.values.value[1] => xml:'<value>B</value>'
      |            ) => BOOL(true)
      |          ) => BOOL(true),
      |          %error (
      |            'Was expecting an XML element \\/values\\/value\\/1 but it was missing'
      |          )
      |        ) => BOOL(true)
      |      ) => BOOL(false),
      |      %error (
      |        'Was expecting an XML element \\/values but it was missing'
      |      )
      |    ) => BOOL(false)
      |  ) => BOOL(false)
      |) => BOOL(false)'''.stripMargin('|')
    def patch = DiffUtils.diff(prettyResult, expected, null)
    def diff = generateUnifiedDiff('', '', prettyResult.split('\n') as List<String>, patch, 0).join('\n')

    then:
    result.result.value.bool == false
    diff == ''
  }

  def 'xml missing second property attributes'() {
    given:
    List<String> path = ['$']
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    def config = new MatchingConfiguration(false, false, true, false)
    def context = new PlanMatchingContext(pact, interaction, config)
    ExecutionPlanInterpreter interpreter = new ExecutionPlanInterpreter(context)

    def builder = XMLPlanBuilder.INSTANCE
    def xml = '''<?xml version="1.0" encoding="UTF-8"?>
      <config>
        <name>My Settings</name>
        <sound>
          <property name="volume" value="11" />
          <property name="mixer" value="standard" />
        </sound>
      </config>
    '''
    ExecutionPlanNode node = builder.buildPlan(xml.bytes, context)

    def content = '''<?xml version="1.0" encoding="UTF-8"?>
      <config>
        <name>My Settings</name>
        <sound>
          <property name="volume" value="11" />
          <property />
        </sound>
      </config>
    '''
    ValueResolver resolver = Mock() {
      resolve(_, _) >> { new Result.Ok(new NodeValue.BARRAY(content.bytes)) }
    }

    when:
    def result = interpreter.walkTree(path, node, resolver)
    def buffer = new StringBuilder()
    result.prettyForm(buffer, 0)
    def prettyResult = buffer.toString()
    def expected = '''%tee (
    |  %xml:parse (
    |    $.body => BYTES(211, PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KICAgICAgPGNvbmZpZz4KICAgICAgICA8bmFtZT5NeSBTZXR0aW5nczwvbmFtZT4KICAgICAgICA8c291bmQ+CiAgICAgICAgICA8cHJvcGVydHkgbmFtZT0idm9sdW1lIiB2YWx1ZT0iMTEiIC8+CiAgICAgICAgICA8cHJvcGVydHkgLz4KICAgICAgICA8L3NvdW5kPgogICAgICA8L2NvbmZpZz4KICAgIA==)
    |  ) => xml:'<config>\\n            \\n    <name>My Settings<\\/name>\\n            \\n    <sound>\\n                  \\n        <property name=\\"volume\\" value=\\"11\\"\\/>\\n                  \\n        <property\\/>\\n                \\n    <\\/sound>\\n          \\n<\\/config>',
    |  :$ (
    |    %if (
    |      %check:exists (
    |        ~>$.config => xml:'<config>\\n            \\n    <name>My Settings<\\/name>\\n            \\n    <sound>\\n                  \\n        <property name=\\"volume\\" value=\\"11\\"\\/>\\n                  \\n        <property\\/>\\n                \\n    <\\/sound>\\n          \\n<\\/config>'
    |      ) => BOOL(true),
    |      :$.config (
    |        :#text (
    |          %expect:empty (
    |            %to-string (
    |              ~>$.config['#text'] => NULL
    |            ) => ''
    |          ) => BOOL(true)
    |        ) => BOOL(true),
    |        %expect:only-entries (
    |          ['name', 'sound'] => ['name', 'sound'],
    |          ~>$.config => xml:'<config>\\n            \\n    <name>My Settings<\\/name>\\n            \\n    <sound>\\n                  \\n        <property name=\\"volume\\" value=\\"11\\"\\/>\\n                  \\n        <property\\/>\\n                \\n    <\\/sound>\\n          \\n<\\/config>'
    |        ) => OK,
    |        %expect:count (
    |          UINT(1) => UINT(1),
    |          ~>$.config.name => xml:'<name>My Settings<\\/name>',
    |          %join (
    |            'Expected 1 <name> child element but there were ',
    |            %length (
    |              ~>$.config.name
    |            )
    |          )
    |        ) => OK,
    |        %if (
    |          %check:exists (
    |            ~>$.config.name[0] => xml:'<name>My Settings<\\/name>'
    |          ) => BOOL(true),
    |          :$.config.name[0] (
    |            :#text (
    |              %match:equality (
    |                'My Settings' => 'My Settings',
    |                %to-string (
    |                  ~>$.config.name[0]['#text'] => xml:text:'My Settings'
    |                ) => 'My Settings',
    |                NULL => NULL
    |              ) => BOOL(true)
    |            ) => BOOL(true),
    |            %expect:empty (
    |              ~>$.config.name[0] => xml:'<name>My Settings<\\/name>'
    |            ) => BOOL(true)
    |          ) => BOOL(true),
    |          %error (
    |            'Was expecting an XML element \\/config\\/name\\/0 but it was missing'
    |          )
    |        ) => BOOL(true),
    |        %expect:count (
    |          UINT(1) => UINT(1),
    |          ~>$.config.sound => xml:'<sound>\\n              \\n    <property name=\\"volume\\" value=\\"11\\"\\/>\\n              \\n    <property\\/>\\n            \\n<\\/sound>',
    |          %join (
    |            'Expected 1 <sound> child element but there were ',
    |            %length (
    |              ~>$.config.sound
    |            )
    |          )
    |        ) => OK,
    |        %if (
    |          %check:exists (
    |            ~>$.config.sound[0] => xml:'<sound>\\n              \\n    <property name=\\"volume\\" value=\\"11\\"\\/>\\n              \\n    <property\\/>\\n            \\n<\\/sound>'
    |          ) => BOOL(true),
    |          :$.config.sound[0] (
    |            :#text (
    |              %expect:empty (
    |                %to-string (
    |                  ~>$.config.sound[0]['#text'] => NULL
    |                ) => ''
    |              ) => BOOL(true)
    |            ) => BOOL(true),
    |            %expect:only-entries (
    |              ['property'] => ['property'],
    |              ~>$.config.sound[0] => xml:'<sound>\\n              \\n    <property name=\\"volume\\" value=\\"11\\"\\/>\\n              \\n    <property\\/>\\n            \\n<\\/sound>'
    |            ) => OK,
    |            %expect:count (
    |              UINT(2) => UINT(2),
    |              ~>$.config.sound[0].property => [xml:'<property name=\\"volume\\" value=\\"11\\"\\/>', xml:'<property/>'],
    |              %join (
    |                'Expected 2 <property> child elements but there were ',
    |                %length (
    |                  ~>$.config.sound[0].property
    |                )
    |              )
    |            ) => OK,
    |            %if (
    |              %check:exists (
    |                ~>$.config.sound[0].property[0] => xml:'<property name=\\"volume\\" value=\\"11\\"\\/>'
    |              ) => BOOL(true),
    |              :$.config.sound[0].property[0] (
    |                :attributes (
    |                  :$.config.sound[0].property[0]['@name'] (
    |                    #{"@name='volume'"},
    |                    %if (
    |                      %check:exists (
    |                        ~>$.config.sound[0].property[0]['@name'] => xml:attribute:'name'='volume'
    |                      ) => BOOL(true),
    |                      %match:equality (
    |                        'volume' => 'volume',
    |                        %xml:value (
    |                          ~>$.config.sound[0].property[0]['@name'] => xml:attribute:'name'='volume'
    |                        ) => 'volume',
    |                        NULL => NULL
    |                      ) => BOOL(true)
    |                    ) => BOOL(true)
    |                  ) => BOOL(true),
    |                  :$.config.sound[0].property[0]['@value'] (
    |                    #{"@value='11'"},
    |                    %if (
    |                      %check:exists (
    |                        ~>$.config.sound[0].property[0]['@value'] => xml:attribute:'value'='11'
    |                      ) => BOOL(true),
    |                      %match:equality (
    |                        '11' => '11',
    |                        %xml:value (
    |                          ~>$.config.sound[0].property[0]['@value'] => xml:attribute:'value'='11'
    |                        ) => '11',
    |                        NULL => NULL
    |                      ) => BOOL(true)
    |                    ) => BOOL(true)
    |                  ) => BOOL(true),
    |                  %expect:entries (
    |                    ['name', 'value'] => ['name', 'value'],
    |                    %xml:attributes (
    |                      ~>$.config.sound[0].property[0] => xml:'<property name=\\"volume\\" value=\\"11\\"\\/>'
    |                    ) => {'name': 'volume', 'value': '11'},
    |                    %join (
    |                      'The following expected attributes were missing: ',
    |                      %join-with (
    |                        ', ',
    |                        ** (
    |                          %apply ()
    |                        )
    |                      )
    |                    )
    |                  ) => OK,
    |                  %expect:only-entries (
    |                    ['name', 'value'] => ['name', 'value'],
    |                    %xml:attributes (
    |                      ~>$.config.sound[0].property[0] => xml:'<property name=\\"volume\\" value=\\"11\\"\\/>'
    |                    ) => {'name': 'volume', 'value': '11'}
    |                  ) => OK
    |                ) => BOOL(true),
    |                :#text (
    |                  %expect:empty (
    |                    %to-string (
    |                      ~>$.config.sound[0].property[0]['#text'] => NULL
    |                    ) => ''
    |                  ) => BOOL(true)
    |                ) => BOOL(true),
    |                %expect:empty (
    |                  ~>$.config.sound[0].property[0] => xml:'<property name=\\"volume\\" value=\\"11\\"\\/>'
    |                ) => BOOL(true)
    |              ) => BOOL(true),
    |              %error (
    |                'Was expecting an XML element \\/config\\/sound\\/0\\/property\\/0 but it was missing'
    |              )
    |            ) => BOOL(true),
    |            %if (
    |              %check:exists (
    |                ~>$.config.sound[0].property[1] => xml:'<property/>'
    |              ) => BOOL(true),
    |              :$.config.sound[0].property[1] (
    |                :attributes (
    |                  :$.config.sound[0].property[1]['@name'] (
    |                    #{"@name='mixer'"},
    |                    %if (
    |                      %check:exists (
    |                        ~>$.config.sound[0].property[1]['@name'] => NULL
    |                      ) => BOOL(false),
    |                      %match:equality (
    |                        'mixer',
    |                        %xml:value (
    |                          ~>$.config.sound[0].property[1]['@name']
    |                        ),
    |                        NULL
    |                      )
    |                    ) => BOOL(false)
    |                  ) => BOOL(false),
    |                  :$.config.sound[0].property[1]['@value'] (
    |                    #{"@value='standard'"},
    |                    %if (
    |                      %check:exists (
    |                        ~>$.config.sound[0].property[1]['@value'] => NULL
    |                      ) => BOOL(false),
    |                      %match:equality (
    |                        'standard',
    |                        %xml:value (
    |                          ~>$.config.sound[0].property[1]['@value']
    |                        ),
    |                        NULL
    |                      )
    |                    ) => BOOL(false)
    |                  ) => BOOL(false),
    |                  %expect:entries (
    |                    ['name', 'value'] => ['name', 'value'],
    |                    %xml:attributes (
    |                      ~>$.config.sound[0].property[1] => xml:'<property/>'
    |                    ) => {},
    |                    %join (
    |                      'The following expected attributes were missing: ' => 'The following expected attributes were missing: ',
    |                      %join-with (
    |                        ', ' => ', ',
    |                        ** (
    |                          %apply () => 'name',
    |                          %apply () => 'value'
    |                        ) => OK
    |                      ) => 'name, value'
    |                    ) => 'The following expected attributes were missing: name, value'
    |                  ) => ERROR(The following expected attributes were missing: name, value),
    |                  %expect:only-entries (
    |                    ['name', 'value'] => ['name', 'value'],
    |                    %xml:attributes (
    |                      ~>$.config.sound[0].property[1] => xml:'<property/>'
    |                    ) => {}
    |                  ) => OK
    |                ) => BOOL(false),
    |                :#text (
    |                  %expect:empty (
    |                    %to-string (
    |                      ~>$.config.sound[0].property[1]['#text'] => NULL
    |                    ) => ''
    |                  ) => BOOL(true)
    |                ) => BOOL(true),
    |                %expect:empty (
    |                  ~>$.config.sound[0].property[1] => xml:'<property/>'
    |                ) => BOOL(true)
    |              ) => BOOL(false),
    |              %error (
    |                'Was expecting an XML element \\/config\\/sound\\/0\\/property\\/1 but it was missing'
    |              )
    |            ) => BOOL(false)
    |          ) => BOOL(false),
    |          %error (
    |            'Was expecting an XML element \\/config\\/sound\\/0 but it was missing'
    |          )
    |        ) => BOOL(false)
    |      ) => BOOL(false),
    |      %error (
    |        'Was expecting an XML element \\/config but it was missing'
    |      )
    |    ) => BOOL(false)
    |  ) => BOOL(false)
    |) => BOOL(false)'''.stripMargin('|')
    def patch = DiffUtils.diff(prettyResult, expected, null)
    def diff = generateUnifiedDiff('', '', prettyResult.split('\n') as List<String>, patch, 0).join('\n')

    then:
    result.result.value.bool == false
    diff == ''
  }
}
